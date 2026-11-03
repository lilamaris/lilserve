plugins {
    id("spring.base")
    id("spring.postgresql")
    id("spring.test.h2")
}

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
    testImplementation(libs.spring.boot.starter.flyway)

    implementation(project(":reservation-service:domain"))
    implementation(project(":reservation-service:application"))
}
