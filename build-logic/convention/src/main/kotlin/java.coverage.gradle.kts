import extension.libs
import extension.requireLibrary
import java.math.BigDecimal
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java.base")
    id("jacoco")
}

jacoco { toolVersion = libs.requireLibrary("jacoco-agent").get().versionConstraint.requiredVersion }

val mainSourceSet = the<JavaPluginExtension>().sourceSets.getByName("main")

val mainOutputDirs = files(mainSourceSet.output.classesDirs, mainSourceSet.output.resourcesDir)

/**
 * The line coverage floor, read from the module's own gradle.properties. `findProperty` is used
 * instead of `providers.gradleProperty`, which does not see a subproject's properties file.
 */
fun Project.coverageMinimum(): BigDecimal =
    (findProperty("coverage.minimum") as String?)?.let { BigDecimal(it) } ?: BigDecimal("0.80")

/** The class file patterns excluded from coverage, read from the module's own gradle.properties. */
fun Project.coverageExcludes(): List<String> =
    (findProperty("coverage.excludes") as String?)?.split(",")?.map { it.trim() } ?: emptyList()

/**
 * The classes jacoco measures: the main output directories with the excluded class files dropped,
 * so an exclusion applies to the measured classes instead of being matched against the bundle name.
 */
fun Project.measuredClasses(): FileCollection =
    files(
        mainOutputDirs.files.map { dir -> fileTree(dir).matching { exclude(coverageExcludes()) } }
    )

tasks.named<Test>("test") { finalizedBy(tasks.named("jacocoTestReport")) }

tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(measuredClasses())
    dependsOn(tasks.named("test"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    val floor = coverageMinimum()
    classDirectories.setFrom(measuredClasses())
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = floor
            }
        }
    }
}

tasks.named("check") { dependsOn(tasks.named("jacocoTestCoverageVerification")) }
