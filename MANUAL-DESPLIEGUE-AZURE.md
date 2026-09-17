# Manual Didáctico y Definitivo de Despliegue en Microsoft Azure

**Proyecto de Titulación:** Sistema Distribuido Forense de Certificación y Custodia de Obras de Arte  
**Destinataria:** Guía paso a paso preparada para despliegue autónomo en una cuenta limpia de Azure  
**Fecha de actualización:** Septiembre 2026  
**Stack:** Java 25 (Quarkus) + Vue 3 + Azure Container Apps + PostgreSQL Flexible Server + Azure Key Vault  

---

## 1. Introducción y Arquitectura

Este manual está diseñado para que puedas clonar el repositorio en tu computador (`git pull`) y desplegar la infraestructura completa en tu propia suscripción de Microsoft Azure, utilizando tu propio **Azure Key Vault** y tus propias credenciales seguras.

```
                                    +-----------------------------------------+
                                    |    Azure Storage Static Website         |
                                    |        (Frontend Vue 3 SPA)             |
                                    +--------------------+--------------------+
                                                         | HTTPS + JWT
                                  +----------------------+----------------------+
                                  |                                             |
                                  v                                             v
        +-----------------------------------+         +-----------------------------------+
        |  Container App: modulo-validar    |         |  Container App: modulo-b          |
        |  (Identidad, Registro y Login)    |         |  (Peritaje, pHash y Certificados) |
        |  Puerto: 8080 | 0.5 CPU / 1 GiB   |         |  Puerto: 8082 | 2 CPU / 4 GiB     |
        +-----------------+-----------------+         +-----------------+-----------------+
                          |                                             |
                          +----------------------+----------------------+
                                                 | JDBC TLS (5432)
                                                 v
                               +-----------------------------------+
                               | PostgreSQL Flexible Server        |
                               | Base de datos: tesis_db           |
                               +-----------------------------------+

     [Managed Identity]                                            [Managed Identity]
     modulo-validar ------> Azure Key Vault (RSA Keys & Secrets) <------ modulo-b
```

---

## 2. Requisitos Previos en tu Computador

Antes de empezar, abre una consola de **PowerShell** y verifica que tengas instaladas estas herramientas:

```powershell
# 1. Azure CLI (para interactuar con Azure desde la terminal)
az version

# 2. Docker Desktop (debe estar abierto y corriendo)
docker version

# 3. Java Development Kit (JDK 25)
java -version

# 4. Node.js y npm (para compilar el frontend)
node -version
npm -version
```

---

## 3. Resumen Rápido de Modos de Ejecución

El repositorio incluye 3 métodos listos para usar según lo que necesites hacer:

| Modo | Comando | Cuándo usarlo |
| :--- | :--- | :--- |
| **1. Pruebas Locales (Docker Compose)** | `docker compose up --build` | Para probar y evaluar todo el sistema en tu propia computadora sin gastar créditos en Azure. Levanta PostgreSQL, Módulo A, Módulo B y Frontend. |
| **2. Despliegue Inicial en Azure (Automático)** | `.\desplegar-azure.ps1` | Aprovisiona toda la infraestructura en tu cuenta de Azure en 1 solo paso con región `eastus` y tu Key Vault. |
| **3. Actualización de Código en Azure** | `.\actualizar-azure.ps1` | Recompila y actualiza Container Apps y el Frontend en Azure sin tocar la base de datos ni alterar Key Vault. |
| **4. Despliegue Manual Paso a Paso** | Ver secciones a continuación | Para entender o ejecutar cada comando de Azure CLI individualmente. |

### Modo 1: Probar en tu computadora con Docker Compose
Si solo deseas validar que todo funcione en tu laptop:
```powershell
# 1. Compilar los ejecutables de ambos microservicios Java:
cd modulo-validar; .\gradlew.bat quarkusBuild -x test; cd ..
cd modulo_b_analisis_forense_y_certificacion; .\gradlew.bat quarkusBuild -x test; cd ..

# 2. Levantar todos los contenedores:
docker compose up --build
```
Una vez levantado, abre tu navegador en:
- **Frontend Web:** `http://localhost:8085`
- **Módulo A (Identidad):** `http://localhost:8080/q/health/ready`
- **Módulo B (Forense):** `http://localhost:8082/q/health/ready`

### Modo 2: Despliegue Automatizado en Azure
Para subir todo el sistema a tu cuenta de Azure con un solo script:
```powershell
# Inicia sesión en tu cuenta de Azure:
az login

# Ejecuta el script de aprovisionamiento:
.\desplegar-azure.ps1 -KeyVaultName "tu-keyvault-unico" -ResourceGroup "rg-tesis-forense-eastus"
```

### Modo 3: Actualizar el Sistema en Azure tras cambios de código
Cuando modifiques código en cualquier módulo y quieras publicar los cambios a Azure:
```powershell
.\actualizar-azure.ps1 -ResourceGroup "rg-tesis-forense-eastus"
```

---

## 4. Despliegue Manual Paso a Paso en Azure

### 4.1. Paso 0: Selección de Región Óptima para Ecuador

> [!TIP]
> **¿Por qué elegir `eastus` (East US - Virginia)?**  
> Para conexiones de internet desde Ecuador (proveedores como CNT, Claro, Netlife o Telconet), el cable submarino de fibra óptica (PCCS) viaja directamente por el Caribe hacia Florida y Virginia. Por eso, **`eastus` tiene la menor latencia (65 a 85 milisegundos)**, además de ofrecer cuotas completas en cuentas académicas y los menores costos por hora.

1. Inicia sesión en Azure:
   ```powershell
   az login
   ```
2. Si tienes varias suscripciones, selecciona la tuya:
   ```powershell
   az account list --output table
   az account set --subscription "<TU_SUBSCRIPTION_ID>"
   ```
3. Comprueba si tu suscripción tiene alguna restricción de políticas regionales:
   ```powershell
   az policy assignment list --query "[].{Name:name, PolicyDefinition:policyDefinitionId}" -o table
   ```
   *Si tu cuenta no tiene restricciones, usaremos `eastus`. Si tiene restricción a Latinoamérica, usaremos `mexicocentral`.*

---

## 4. Paso 1: Variables de tu Despliegue

Copia y pega estas variables en tu consola de PowerShell, reemplazando los valores entre `<...>` por nombres únicos en minúsculas (Azure exige que nombres como el Key Vault, ACR y Storage sean únicos a nivel mundial):

```powershell
# --- CONFIGURACIÓN GENERAL ---
$SUBSCRIPTION_ID = (az account show --query id -o tsv)
$RESOURCE_GROUP  = "rg-tesis-forense"
$LOCATION        = "eastus"                     # o "mexicocentral" si tu cuenta lo restringe

# --- NOMBRES DE RECURSOS (Deben ser únicos en todo Azure) ---
$KEYVAULT_NAME   = "kv-forense-$((Get-Random -Minimum 1000 -Maximum 9999))"
$ACR_NAME        = "acrforense$((Get-Random -Minimum 1000 -Maximum 9999))"
$POSTGRES_SERVER = "psql-forense-$((Get-Random -Minimum 1000 -Maximum 9999))"
$STORAGE_ACCOUNT = "saforense$((Get-Random -Minimum 1000 -Maximum 9999))"
$CONTAINER_ENV   = "cae-tesis-env"

# --- CREDENCIALES SEGURAS (Cámbialas por tus propias claves) ---
$POSTGRES_USER   = "tesisadmin"
$POSTGRES_PASS   = "TesisForense2026!Segura"    # Debe tener mayúsculas, minúsculas, números y símbolo
$HMAC_SECRET     = "clave-secreta-hmac-cedula-2026-ecuador-segura"
```

Crea el Grupo de Recursos que contendrá todo:
```powershell
az group create --name $RESOURCE_GROUP --location $LOCATION
```

---

## 5. Paso 2: Configuración de tu Azure Key Vault Propio

El sistema utiliza criptografía en la nube para no almacenar contraseñas ni llaves en los contenedores. Tu Key Vault requiere **RBAC habilitado** y 2 llaves RSA específicas.

### 2.1. Crear el Key Vault
```powershell
az keyvault create `
  --name $KEYVAULT_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --enable-rbac-authorization true
```

### 2.2. Asignarte permisos de administración sobre tu Key Vault
Para que puedas crear las llaves y secretos desde tu terminal:
```powershell
$MI_USER_ID = (az ad signed-in-user show --query id -o tsv)
$KEYVAULT_ID = (az keyvault show --name $KEYVAULT_NAME --resource-group $RESOURCE_GROUP --query id -o tsv)

# Asignar rol de Administrador de Key Vault a tu usuario
az role assignment create `
  --role "Key Vault Administrator" `
  --assignee-object-id $MI_USER_ID `
  --assignee-principal-type "User" `
  --scope $KEYVAULT_ID
```
*(Espera 30 segundos a que Azure propague la asignación de permisos)*.

---

### 2.3. Generar la Llave Maestra de Custodia (`master-custody-key`)
> [!IMPORTANT]
> Esta llave realiza el **Cifrado de Sobre (Envelope Encryption)** para proteger las cédulas y nombres en PostgreSQL. **Debe crearse obligatoriamente con las 4 operaciones**: `encrypt`, `decrypt`, `wrapKey` y `unwrapKey`.

```powershell
az keyvault key create `
  --vault-name $KEYVAULT_NAME `
  --name "master-custody-key" `
  --kty RSA `
  --size 2048 `
  --ops encrypt decrypt wrapKey unwrapKey
```

---

### 2.4. Generar la Llave para Firma de Tokens JWT (`Token-JWT-sistema-forense-1`)
Módulo A utiliza esta llave RSA para firmar los tokens de autenticación de los usuarios:
```powershell
az keyvault key create `
  --vault-name $KEYVAULT_NAME `
  --name "Token-JWT-sistema-forense-1" `
  --kty RSA `
  --size 2048 `
  --ops sign verify
```

---

### 2.5. Exportar la Llave Pública RSA hacia el código fuente
Para que el Módulo B pueda verificar que los tokens JWT fueron emitidos legítimamente por el Módulo A, necesita la clave pública en formato PEM.

Ejecuta este comando para descargar directamente la clave pública desde tu Azure Key Vault y sincronizarla en ambos módulos:
```powershell
# 1. Descargar la llave pública en formato PEM directamente desde tu Key Vault
az keyvault key download --vault-name $KEYVAULT_NAME -n "Token-JWT-sistema-forense-1" -e PEM -f modulo-validar/src/main/resources/publicKey.pem

# 2. Copiarla exactamente a Módulo B para que la verificación JWT sea idéntica
Copy-Item modulo-validar/src/main/resources/publicKey.pem modulo_b_analisis_forense_y_certificacion/src/main/resources/publicKey.pem

Write-Host "Clave pública sincronizada con éxito en Módulo A y Módulo B." -ForegroundColor Green
```

---

### 2.6. Crear el Secreto con el Certificado Institucional (`system-certificadora-obras`)
Este certificado PKCS#12 (.p12) representa el sello oficial de la institución que valida los peritajes en el PDF final:
```powershell
# Si cuentas con tu archivo .p12 institucional:
# $certBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes("ruta\a\tu\certificado.p12"))
# az keyvault secret set --vault-name $KEYVAULT_NAME --name "system-certificadora-obras" --value $certBase64

# O si usarás el certificado de pruebas incluido para desarrollo:
$p12DemoBytes = [System.Text.Encoding]::UTF8.GetBytes("CERTIFICADO_DEMO_INSTITUCIONAL_P12")
$p12DemoBase64 = [Convert]::ToBase64String($p12DemoBytes)
az keyvault secret set --vault-name $KEYVAULT_NAME --name "system-certificadora-obras" --value $p12DemoBase64
```

---

## 6. Paso 3: Crear PostgreSQL Flexible Server

Creamos la base de datos compartida para ambos microservicios:

```powershell
az postgres flexible-server create `
  --resource-group $RESOURCE_GROUP `
  --name $POSTGRES_SERVER `
  --location $LOCATION `
  --admin-user $POSTGRES_USER `
  --admin-password $POSTGRES_PASS `
  --sku-name Standard_B1ms `
  --tier Burstable `
  --storage-size 32 `
  --version 16 `
  --yes

# Crear la base de datos de la tesis
az postgres flexible-server db create `
  --resource-group $RESOURCE_GROUP `
  --server-name $POSTGRES_SERVER `
  --database-name "tesis_db"

# Permitir que los servicios internos de Azure se conecten a la base de datos
az postgres flexible-server firewall-rule create `
  --resource-group $RESOURCE_GROUP `
  --name $POSTGRES_SERVER `
  --rule-name "AllowAzureServices" `
  --start-ip-address "0.0.0.0" `
  --end-ip-address "0.0.0.0"
```

---

## 7. Paso 4: Crear Azure Container Registry (ACR) y Subir Imágenes Docker

### 4.1. Crear el registro privado de imágenes
```powershell
az acr create `
  --name $ACR_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --sku Basic `
  --admin-enabled true

az acr login --name $ACR_NAME
$ACR_LOGIN_SERVER = (az acr show --name $ACR_NAME --query loginServer -o tsv)
```

### 4.2. Compilar y Subir Módulo A (`modulo-validar`)
```powershell
cd modulo-validar
.\gradlew.bat clean build -x test
docker build -f src/main/docker/Dockerfile.jvm -t "$ACR_LOGIN_SERVER/modulo-validar:1.0.0" .
docker push "$ACR_LOGIN_SERVER/modulo-validar:1.0.0"
cd ..
```

### 4.3. Compilar y Subir Módulo B (`modulo_b`)
```powershell
cd modulo_b_analisis_forense_y_certificacion
.\gradlew.bat clean build -x test
docker build -f src/main/docker/Dockerfile.jvm -t "$ACR_LOGIN_SERVER/modulo-b:1.0.0" .
docker push "$ACR_LOGIN_SERVER/modulo-b:1.0.0"
cd ..
```

---

## 8. Paso 5: Desplegar Azure Container Apps

### 5.1. Crear el Entorno de Container Apps
```powershell
az containerapp env create `
  --name $CONTAINER_ENV `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION
```

### 5.2. Credenciales del ACR
```powershell
$ACR_PASS = (az acr credential show --name $ACR_NAME --query passwords[0].value -o tsv)
```

### 5.3. Desplegar Módulo A (`modulo-validar`)
```powershell
az containerapp create `
  --name "modulo-validar" `
  --resource-group $RESOURCE_GROUP `
  --environment $CONTAINER_ENV `
  --image "$ACR_LOGIN_SERVER/modulo-validar:1.0.0" `
  --target-port 8080 `
  --ingress external `
  --min-replicas 1 `
  --max-replicas 1 `
  --cpu 0.5 `
  --memory 1.0Gi `
  --system-assigned `
  --registry-server $ACR_LOGIN_SERVER `
  --registry-username $ACR_NAME `
  --registry-password $ACR_PASS `
  --secrets `
    db-password="$POSTGRES_PASS" `
    hmac-secret="$HMAC_SECRET" `
  --env-vars `
    PORT="8080" `
    DB_USER="$POSTGRES_USER" `
    DB_PASSWORD="secretref:db-password" `
    DB_URL="jdbc:postgresql://$POSTGRES_SERVER.postgres.database.azure.com:5432/tesis_db?sslmode=require" `
    AZURE_VAULT_URL="https://$KEYVAULT_NAME.vault.azure.net/" `
    MASTER_KEY_NAME="master-custody-key" `
    JWT_KEY_NAME="Token-JWT-sistema-forense-1" `
    CEDULA_HMAC_SECRET="secretref:hmac-secret" `
    ALLOW_INSECURE_ENCRYPTION_FALLBACK="false" `
    FRONTEND_ORIGIN="*"
```

Asignar permisos de Key Vault al Módulo A:
```powershell
$APP_A_PRINCIPAL = (az containerapp show -n modulo-validar -g $RESOURCE_GROUP --query identity.principalId -o tsv)

az role assignment create `
  --role "Key Vault Crypto User" `
  --assignee-object-id $APP_A_PRINCIPAL `
  --assignee-principal-type "ServicePrincipal" `
  --scope $KEYVAULT_ID

az role assignment create `
  --role "Key Vault Secrets User" `
  --assignee-object-id $APP_A_PRINCIPAL `
  --assignee-principal-type "ServicePrincipal" `
  --scope $KEYVAULT_ID
```

---

### 5.4. Desplegar Módulo B (`modulo-b`)
> [!NOTE]
> Módulo B requiere **2 vCPU y 4 GiB de RAM** para descomprimir capas PSD complejas en memoria sin provocar errores de OutOfMemory.

```powershell
az containerapp create `
  --name "modulo-b" `
  --resource-group $RESOURCE_GROUP `
  --environment $CONTAINER_ENV `
  --image "$ACR_LOGIN_SERVER/modulo-b:1.0.0" `
  --target-port 8082 `
  --ingress external `
  --min-replicas 1 `
  --max-replicas 1 `
  --cpu 2.0 `
  --memory 4.0Gi `
  --system-assigned `
  --registry-server $ACR_LOGIN_SERVER `
  --registry-username $ACR_NAME `
  --registry-password $ACR_PASS `
  --secrets `
    db-password="$POSTGRES_PASS" `
    hmac-secret="$HMAC_SECRET" `
  --env-vars `
    PORT="8082" `
    DB_USER="$POSTGRES_USER" `
    DB_PASSWORD="secretref:db-password" `
    DB_URL="jdbc:postgresql://$POSTGRES_SERVER.postgres.database.azure.com:5432/tesis_db?sslmode=require" `
    AZURE_VAULT_URL="https://$KEYVAULT_NAME.vault.azure.net/" `
    MASTER_KEY_NAME="master-custody-key" `
    CERT_NAME="system-certificadora-obras" `
    CEDULA_HMAC_SECRET="secretref:hmac-secret" `
    ALLOW_INSECURE_ENCRYPTION_FALLBACK="false" `
    FRONTEND_ORIGIN="*"
```

Asignar permisos de Key Vault al Módulo B:
```powershell
$APP_B_PRINCIPAL = (az containerapp show -n modulo-b -g $RESOURCE_GROUP --query identity.principalId -o tsv)

az role assignment create `
  --role "Key Vault Crypto User" `
  --assignee-object-id $APP_B_PRINCIPAL `
  --assignee-principal-type "ServicePrincipal" `
  --scope $KEYVAULT_ID

az role assignment create `
  --role "Key Vault Secrets User" `
  --assignee-object-id $APP_B_PRINCIPAL `
  --assignee-principal-type "ServicePrincipal" `
  --scope $KEYVAULT_ID
```

---

## 9. Paso 6: Desplegar el Frontend Vue (Azure Storage Static Website)

Obtén las URLs públicas de tus microservicios:
```powershell
$URL_MODULO_A = "https://" + (az containerapp show -n modulo-validar -g $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv)
$URL_MODULO_B = "https://" + (az containerapp show -n modulo-b -g $RESOURCE_GROUP --query properties.configuration.ingress.fqdn -o tsv)

Write-Host "URL Módulo A: $URL_MODULO_A" -ForegroundColor Cyan
Write-Host "URL Módulo B: $URL_MODULO_B" -ForegroundColor Cyan
```

### 6.1. Crear cuenta de almacenamiento y habilitar el sitio web
```powershell
az storage account create `
  --name $STORAGE_ACCOUNT `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION `
  --sku Standard_LRS

az storage blob service-properties update `
  --account-name $STORAGE_ACCOUNT `
  --static-website `
  --404-document index.html `
  --index-document index.html

$WEB_ENDPOINT = (az storage account show --name $STORAGE_ACCOUNT --query "primaryEndpoints.web" -o tsv).TrimEnd('/')
Write-Host "URL Pública del Frontend: $WEB_ENDPOINT" -ForegroundColor Green
```

### 6.2. Configurar variables de entorno y compilar el Frontend
Crea el archivo `.env.production` dentro de `frontend_sistema_forense`:
```powershell
@"
VUE_APP_API_AUTH=$URL_MODULO_A/api/v1/auth
VUE_APP_API_CERT=$URL_MODULO_B/api/v1/certificaciones
"@ | Set-Content -Path "frontend_sistema_forense\.env.production" -Encoding UTF8

cd frontend_sistema_forense
npm ci
npm run build
cd ..
```

### 6.3. Subir el Frontend a Azure Storage
```powershell
az storage blob upload-batch `
  --account-name $STORAGE_ACCOUNT `
  --source "frontend_sistema_forense/dist" `
  --destination "`$web" `
  --overwrite
```

### 6.4. Blindar CORS en ambos Container Apps
Ahora que tienes la URL exacta de tu frontend, restringe el CORS para evitar accesos no autorizados:
```powershell
az containerapp update -n modulo-validar -g $RESOURCE_GROUP --set-env-vars FRONTEND_ORIGIN="$WEB_ENDPOINT"
az containerapp update -n modulo-b -g $RESOURCE_GROUP --set-env-vars FRONTEND_ORIGIN="$WEB_ENDPOINT"
```

---

## 10. Paso 7: Verificación Funcional Rápida

Comprueba el estado de salud de tus microservicios con los endpoints del estándar MicroProfile:

```powershell
# 1. Comprobar Módulo A (Liveness y Readiness con Key Vault)
Invoke-RestMethod "$URL_MODULO_A/q/health/live"
Invoke-RestMethod "$URL_MODULO_A/q/health/ready"

# 2. Comprobar Módulo B (Liveness y Readiness con Key Vault)
Invoke-RestMethod "$URL_MODULO_B/q/health/live"
Invoke-RestMethod "$URL_MODULO_B/q/health/ready"
```
Ambos deben responder `{"status": "UP"}`.

Abre en tu navegador la URL de tu frontend:
```text
https://<tu-storage-account>.z13.web.core.windows.net
```
Prueba el flujo:
1. **Registro:** Completa el formulario de autor adjuntando tu firma `.p12`.
2. **Login:** Inicia sesión con tus credenciales y verifica que obtengas acceso.
3. **Certificación:** Carga un archivo PSD y su imagen PNG/JPG representativa.
4. **Firma y Certificado:** Firma digitalmente y descarga el expediente pericial `.zip` y certificado sellado.

---

## 11. Diagnóstico y Preguntas Frecuentes (Troubleshooting)

| Síntoma | Causa Frecuente | Solución Inmediata |
|---|---|---|
| Error 403 en registro: *Forbidden by Key Vault* | La Managed Identity de `modulo-validar` no tiene el rol asignado. | Ejecuta la sección 5.3 para asignar `Key Vault Crypto User` al `principalId`. |
| Error *Invalid operation 'encrypt'* en Key Vault | La llave `master-custody-key` fue creada sin la operación `encrypt`. | Ejecuta el comando de la sección 2.3 asegurando `--ops encrypt decrypt wrapKey unwrapKey`. |
| Error en el navegador: *CORS Policy Blocked* | `FRONTEND_ORIGIN` no coincide exactamente con la URL del Storage. | Verifica que no tenga una barra diagonal `/` al final en la variable de entorno. |
| El análisis PSD demora o reinicia el contenedor | El contenedor no tiene suficiente memoria asignada. | Asegúrate de que `modulo-b` tenga `--cpu 2.0 --memory 4.0Gi` en Container Apps. |

---
*Manual validado para despliegues independientes en Azure Container Apps con Java 25 y Quarkus 3.35+.*
