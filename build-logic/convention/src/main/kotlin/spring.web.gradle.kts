import extension.libs
import extension.requireLibrary

plugins { id("spring.base") }

dependencies {
    implementation(libs.requireLibrary("spring-boot-starter-validation"))
    implementation(libs.requireLibrary("spring-boot-starter-webmvc"))
    implementation(libs.requireLibrary("springdoc-openapi-starter-webmvc-ui"))

    testImplementation(libs.requireLibrary("spring-boot-starter-webmvc-test"))
}
