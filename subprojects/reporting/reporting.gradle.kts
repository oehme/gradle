plugins {
    gradlebuild.distribution.`plugins-api-java`
}

val implementationResources: Configuration by configurations.creating

repositories {
    googleApisJs()
}

dependencies {
    implementation(project(":baseServices"))
    implementation(project(":logging"))
    implementation(project(":fileCollections"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))

    implementation(library("groovy"))
    implementation(library("guava"))
    implementation(library("inject"))
    implementation(library("jatl"))

    implementationResources("jquery:jquery.min:3.4.1@js")

    testImplementation(project(":processServices"))
    testImplementation(project(":baseServicesGroovy"))
    testImplementation(testLibrary("jsoup"))
    testImplementation(testFixtures(project(":core")))

    testRuntimeOnly(project(":distributionsCore")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }

    integTestDistributionRuntimeOnly(project(":distributionsCore"))
}

strictCompile {
    ignoreRawTypes() // raw types used in public API
    ignoreParameterizedVarargType() // [unchecked] Possible heap pollution from parameterized vararg type: GenerateBuildDashboard.aggregate()
}

classycle {
    excludePatterns.set(listOf("org/gradle/api/reporting/internal/**"))
}

val generatedResourcesDir = gradlebuildJava.generatedResourcesDir

val reportResources by tasks.registering(Copy::class) {
    from(implementationResources)
    into("$generatedResourcesDir/org/gradle/reporting")
}
sourceSets.main {
    output.dir(generatedResourcesDir, "builtBy" to reportResources)
}
