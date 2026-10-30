import extension.libs
import extension.requireLibrary

plugins { id("java.base") }

tasks.withType<Test>().configureEach { useJUnitPlatform() }

dependencies {
    testImplementation(platform(libs.requireLibrary("junit-bom")))
    testImplementation(platform(libs.requireLibrary("mockito-bom")))

    testImplementation(libs.requireLibrary("junit-jupiter"))
    testRuntimeOnly(libs.requireLibrary("junit-platform-launcher"))

    testImplementation(libs.requireLibrary("assertj-core"))

    testImplementation(libs.requireLibrary("mockito-core"))
    testImplementation(libs.requireLibrary("mockito-junit-jupiter"))
}
