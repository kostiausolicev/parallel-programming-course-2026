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

// тесты коллектора
registerAppRunTask("runSingleThread", "singleThread")
registerAppRunTask("runSingleLock", "singleLock")
registerAppRunTask("runEmptySingleLock", "emptySingleLock")
registerAppRunTask("runSharedLock", "sharedLock")
registerAppRunTask("runThreadLocalSharedLock", "threadLocalSharedLock")
registerAppRunTask("runTwoBuffers", "twoBuffers")

// тесты консистентности
registerAppRunTask("runInconsistencyShared", "inconsistencyShared")
registerAppRunTask("runInconsistencyThreadLocalShared", "inconsistencyThreadLocalShared")
registerAppRunTask("runInconsistencyTwoBuffers", "inconsistencyTwoBuffers")
registerAppRunTask("runTwoBuffersInconsistencyTest", "inconsistencyTwoBuffers")
registerAppRunTask("runInconsistencyTwoBuffersNoCheck", "inconsistencyTwoBuffersNoCheck")

val metricTasks = listOf(
    "runSingleThread",
    "runSingleLock",
    "runEmptySingleLock",
    "runSharedLock",
    "runThreadLocalSharedLock",
    "runTwoBuffers"
)

val consistencyTasks = listOf(
    "runInconsistencyShared",
    "runInconsistencyThreadLocalShared",
    "runInconsistencyTwoBuffers",
    "runTwoBuffersInconsistencyTest",
    "runInconsistencyTwoBuffersNoCheck"
)


val allMetricTasks = consistencyTasks + metricTasks

allMetricTasks.zipWithNext().shuffled().forEach { (previousTaskName, nextTaskName) ->
    tasks.named(nextTaskName) {
        mustRunAfter(previousTaskName)
    }
}

tasks.register("runMetricTests") {
    group = "application"
    dependsOn(allMetricTasks)
}
