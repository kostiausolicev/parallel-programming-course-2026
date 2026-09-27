import org.gradle.kotlin.dsl.register

plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "org.example.App"
}

fun metricsOutputDir(): String =
    providers.gradleProperty("metricsDir").orNull
        ?: layout.buildDirectory.dir("metrics").get().asFile.absolutePath

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("runSingleThread", JavaExec::class) {
    group = "application"
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    args("singleThread", metricsOutputDir())
}

tasks.register("runSingleLock", JavaExec::class) {
    group = "application"
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    args("singleLock", metricsOutputDir())
}

tasks.register("runEmptySingleLock", JavaExec::class) {
    group = "application"
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    args("emptySingleLock", metricsOutputDir())
}

tasks.register("runSharedLock", JavaExec::class) {
    group = "application"
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    args("sharedLock", metricsOutputDir())
}

tasks.register("runAllMetrics") {
    group = "application"
    dependsOn("runSingleThread", "runSingleLock", "runEmptySingleLock", "runSharedLock")
}
