plugins {
    id("java")
    application
    id("io.quarkus") version "3.35.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Quarkus Base y REST
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.35.2"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jsonb")
    implementation("io.quarkus:quarkus-rest-client")

    // Limpieza periódica de las cachés en memoria del orquestador (evita que crezcan sin límite)
    implementation("io.quarkus:quarkus-scheduler")

    // Health checks (/q/health/live, /q/health/ready) para orquestadores (Azure Container Apps, K8s)
    implementation("io.quarkus:quarkus-smallrye-health")
    // Resiliencia en llamadas salientes (timeout/retry) hacia el simulador de CA / Registro Civil
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")

    // Extensión de Quarkus para Azure Key Vault
    implementation("io.quarkiverse.azureservices:quarkus-azure-keyvault:1.2.4")

    // SDK crudo de Azure Key Vault (KeyClient/CryptographyClient) — mismo que usa
    // Módulo A para envolver/desenvolver la llave AES de nombres/apellidos/cédula
    // con la llave RSA "master-custody-key". La extensión de arriba solo trae
    // secretos, no operaciones de cifrado con una llave.
    implementation("com.azure:azure-security-keyvault-keys:4.10.6")
    implementation("com.azure:azure-identity:1.13.0")

    // Verificación de JWT emitido por Módulo A (autenticación de CertificacionResource)
    implementation("io.quarkus:quarkus-smallrye-jwt")

    // Persistencia (ORM, BD y Migraciones)
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    // implementation("io.quarkus:quarkus-flyway")
    // runtimeOnly("org.flywaydb:flyway-database-postgresql:12.5.0")

    implementation("com.drewnoakes:metadata-extractor:2.20.0")

    // TwelveMonkeys: permite leer PSD con ImageIO.read() compositeando todas las capas visibles
    implementation("com.twelvemonkeys.imageio:imageio-psd:3.11.0")
    // Core necesario para que TwelveMonkeys funcione
    implementation("com.twelvemonkeys.imageio:imageio-core:3.11.0")
    implementation("com.twelvemonkeys.common:common-lang:3.11.0")
    implementation("com.twelvemonkeys.common:common-io:3.11.0")
    implementation("com.twelvemonkeys.common:common-image:3.11.0")

    // Lombok: elimina boilerplate de getters, setters y builders
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Gson: serialización JSON del expediente
    implementation("com.google.code.gson:gson:2.11.0")

    // ZXing: generación de código QR (Opción A — ID interno)
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // iText 7: generación del PDF del certificado + PdfSigner (firma digital real)
    implementation("com.itextpdf:itext7-core:7.2.6")
    implementation("com.itextpdf:sign:7.2.6")
    implementation("com.itextpdf:html2pdf:4.0.5")

    // Thymeleaf: motor de plantillas HTML
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")

    // BouncyCastle: proveedor criptográfico obligatorio para iText 7 PdfSigner
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // JUnit 5 para pruebas unitarias
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-test-common")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("ec.edu.uce.certificadorforense.Main")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true   // muestra System.out.println de los tests
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}