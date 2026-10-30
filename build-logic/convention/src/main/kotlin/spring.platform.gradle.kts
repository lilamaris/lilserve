import extension.libs
import extension.requireLibrary

plugins { id("java") }

dependencies {
    implementation(platform(libs.requireLibrary("spring-boot-dependencies")))
    annotationProcessor(platform(libs.requireLibrary("spring-boot-dependencies")))
    testImplementation(platform(libs.requireLibrary("spring-boot-dependencies")))
}
