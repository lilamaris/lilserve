plugins {
    id("java.module")
    id("spring.platform")
    `java-library`
}

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

dependencies {
    api(libs.jakarta.persistence.api)

    implementation(project(":kernel"))
}
