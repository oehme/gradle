package DistributedTest

import DistributedTest.vcsRoots.*
import DistributedTest.vcsRoots.DistributedTest_DistributedTest
import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.versionedSettings

object Project : Project({
    uuid = "acb79ff6-062c-43e6-b179-66cb361eb88c"
    id("DistributedTest")
    parentId("_Root")
    name = "DistributedTest"

    vcsRoot(DistributedTest_DistributedTest)

    features {
        versionedSettings {
            id = "PROJECT_EXT_39"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.USE_CURRENT_SETTINGS
            rootExtId = "${DistributedTest_DistributedTest.id}"
            showChanges = false
            settingsFormat = VersionedSettings.Format.KOTLIN
            storeSecureParamsOutsideOfVcs = true
        }
    }
})
