import com.diffplug.spotless.LineEnding
import extension.libs

plugins { id("com.diffplug.spotless") }

val formattingSources =
    fileTree(projectDir) { exclude("**/build/**", "**/.gradle/**", "**/.git/**", "**/.idea/**") }

spotless {
    encoding("UTF-8")
    lineEndings = LineEnding.UNIX

    java {
        target(formattingSources.matching { include("**/src/**/*.java") })
        googleJavaFormat(libs.findVersion("google-java-format").get().requiredVersion).aosp()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlin {
        target(formattingSources.matching { include("**/src/**/*.kt") })
        ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target(formattingSources.matching { include("**/*.gradle.kts") })
        ktfmt(libs.findVersion("ktfmt").get().requiredVersion).kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("misc") {
        target(
            formattingSources.matching {
                include(
                    ".editorconfig",
                    ".gitattributes",
                    ".gitignore",
                    "**/*.yml",
                    "**/*.yaml",
                    "**/*.json",
                    "**/*.toml",
                    "**/*.properties",
                )
            }
        )
        trimTrailingWhitespace()
        endWithNewline()
    }

    format("markdown") {
        target(formattingSources.matching { include("**/*.md") })
        endWithNewline()
    }
}
