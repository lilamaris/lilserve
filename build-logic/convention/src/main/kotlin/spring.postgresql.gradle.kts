import extension.libs
import extension.requireLibrary

plugins { id("spring.platform") }

dependencies {
    runtimeOnly(libs.requireLibrary("postgresql"))
    implementation(libs.requireLibrary("spring-boot-starter-flyway"))
    implementation(libs.requireLibrary("flyway-database-postgresql"))

    testImplementation(libs.requireLibrary("spring-boot-testcontainers"))
    testImplementation(libs.requireLibrary("testcontainers-postgresql"))
}
