import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("spring.base")
    id("spring.test.postgres")
    id("org.springframework.boot")
}

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

// The boot plugin ships bootJar disabled outside the root project, so the executable module enables
// its own.
tasks.named<BootJar>("bootJar") { enabled = true }

dependencies {
    implementation(libs.spring.boot.starter)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(testFixtures(project(":reservation-service:persistence")))

    implementation(project(":reservation-service:domain"))
    implementation(project(":reservation-service:application"))
    implementation(project(":reservation-service:persistence"))
    implementation(project(":reservation-service:web"))
}
