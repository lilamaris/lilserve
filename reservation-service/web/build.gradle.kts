plugins { id("spring.web") }

group = "com.lilamaris.lilserve"

version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":kernel"))
    implementation(project(":reservation-service:domain"))
    implementation(project(":reservation-service:application"))
}
