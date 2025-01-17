repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.2")
    implementation("org.flywaydb:flyway-core:8.4.4")
    implementation("com.github.seratch:kotliquery:1.6.0")
    testImplementation("com.networknt:json-schema-validator:1.0.65")
    testImplementation("org.testcontainers:postgresql:1.16.3")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
}