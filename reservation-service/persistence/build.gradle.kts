plugins {
    id("spring.base")
    id("spring.test.postgres")
    id("java-test-fixtures")
}

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)

    implementation(project(":reservation-service:domain"))
    implementation(project(":reservation-service:application"))

    testFixturesImplementation(platform(libs.spring.boot.dependencies))
    testFixturesImplementation(libs.spring.boot.testcontainers)
    testFixturesImplementation(libs.testcontainers.postgresql)
}
