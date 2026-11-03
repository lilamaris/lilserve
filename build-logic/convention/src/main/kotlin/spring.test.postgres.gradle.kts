import extension.libs
import extension.requireLibrary

plugins { id("spring.postgresql") }

dependencies { testImplementation(libs.requireLibrary("spring-boot-starter-data-jpa-test")) }
