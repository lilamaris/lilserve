import extension.libs
import extension.requireLibrary
import java.math.BigDecimal
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("java.base")
    id("jacoco")
}

jacoco { toolVersion = libs.requireLibrary("jacoco-agent").get().versionConstraint.requiredVersion }

val coverageMinimum =
    providers
        .gradleProperty("coverage.minimum")
        .map { BigDecimal(it) }
        .getOrElse(BigDecimal("0.80"))

val coverageExcludes =
    providers
        .gradleProperty("coverage.excludes")
        .map { value -> value.split(",").map { it.trim() } }
        .getOrElse(emptyList())

tasks.named<Test>("test") { finalizedBy(tasks.named("jacocoTestReport")) }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = coverageMinimum
            }
            excludes = coverageExcludes
        }
    }
}

tasks.named("check") { dependsOn(tasks.named("jacocoTestCoverageVerification")) }
