plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.appengine)
    alias(libs.plugins.versions)
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

application {
    mainClass.set("com.ramitsuri.ApplicationKt")
}

group = "com.ramitsuri"
version = "0.0.35"

appengine {
    stage {
        setArtifact("build/libs/${project.name}-${project.version}-all.jar")
    }
    deploy {
        version = "GCLOUD_CONFIG"
        projectId = "GCLOUD_CONFIG"
    }
}

dependencies {
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.gson)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)

    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logback.core)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    // Hash
    implementation(libs.bcrypt)

    // Firebase
    implementation(libs.firebase.admin)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}

tasks.shadowJar {
    archiveClassifier.set("all")
    // Combine META-INF/services files from all dependencies instead of keeping only one (e.g. java.sql.Driver).
    // Shadow 9 defaults to EXCLUDE, which drops duplicates before the merge transformer sees them,
    // so only let service files through as duplicates.
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
}

// The App Engine Gradle plugin doesn't support the configuration cache (enabled in gradle.properties)
tasks.matching { it.name.startsWith("appengine") || it.name.endsWith("CloudSdk") }.configureEach {
    notCompatibleWithConfigurationCache("App Engine Gradle plugin is not configuration cache compatible")
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !regex.matches(version)
}

// Run `./gradlew dependencyUpdates` to report dependencies with newer stable versions
tasks.dependencyUpdates {
    rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
}
