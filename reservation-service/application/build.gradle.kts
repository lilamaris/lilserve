plugins { id("spring.base") }

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(libs.uuid.creator)
    implementation(libs.spring.boot.starter.validation)

    implementation(project(":reservation-service:domain"))
    implementation(project(":kernel"))
}
