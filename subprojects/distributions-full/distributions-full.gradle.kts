plugins {
    gradlebuild.distribution.packaging
    gradlebuild.`add-verify-production-environment-task`
    gradlebuild.install
}

dependencies {
    coreRuntimeOnly(platform(project(":corePlatform")))

    pluginsRuntimeOnly(platform(project(":distributionsPublishing")))
    pluginsRuntimeOnly(platform(project(":distributionsJvm")))
    pluginsRuntimeOnly(platform(project(":distributionsNative")))

    pluginsRuntimeOnly(project(":buildInit"))
    pluginsRuntimeOnly(project(":buildProfile"))
    pluginsRuntimeOnly(project(":antlr"))

    // The following are scheduled to be removed from the distribution completely in Gradle 7.0
    pluginsRuntimeOnly(project(":javascript"))
    pluginsRuntimeOnly(project(":platformPlay"))
    pluginsRuntimeOnly(project(":idePlay"))
}
