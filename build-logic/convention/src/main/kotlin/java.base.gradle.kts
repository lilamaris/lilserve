import extension.libs
import extension.requireLibrary

plugins { id("java") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation(libs.requireLibrary("jspecify"))
    compileOnly(libs.requireLibrary("lombok"))
    annotationProcessor(libs.requireLibrary("lombok"))
}
