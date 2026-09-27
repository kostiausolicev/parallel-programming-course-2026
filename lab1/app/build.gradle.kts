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

fun registerAppRunTask(taskName: String, collectorArg: String) {
    tasks.register(taskName, JavaExec::class) {
        group = "application"
        mainClass.set(application.mainClass)
        classpath = sourceSets.main.get().runtimeClasspath
        args(collectorArg, metricsOutputDir())
    }
}

registerAppRunTask("runSingleThread", "singleThread")
registerAppRunTask("runSingleLock", "singleLock")
registerAppRunTask("runEmptySingleLock", "emptySingleLock")
registerAppRunTask("runSharedLock", "sharedLock")
registerAppRunTask("runThreadLocalSharedLock", "threadLocalSharedLock")

registerAppRunTask("runInconsistencyShared", "inconsistencyShared")
registerAppRunTask("runInconsistencyThreadLocalShared", "inconsistencyThreadLocalShared")

tasks.register("runAllCollectors") {
    group = "application"
    dependsOn(
        "runSingleThread",
        "runSingleLock",
        "runEmptySingleLock",
        "runSharedLock",
        "runThreadLocalSharedLock"
    )
}

tasks.register("runCollectorTests") {
    group = "verification"
    dependsOn(
        "test",
        "runInconsistencyShared",
        "runInconsistencyThreadLocalShared"
    )
}

tasks.register("runAllMetrics") {
    group = "application"
    dependsOn("runAllCollectors")
}
