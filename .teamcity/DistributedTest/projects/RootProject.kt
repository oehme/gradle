package projects

import DistributedTest.vcsRoots.DistributedTest_DistributedTest
import configurations.StagePasses
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2019_2.projectFeatures.versionedSettings
import model.CIBuildModel
import model.Stage

class RootProject(model: CIBuildModel) : Project({
    uuid = model.projectPrefix.removeSuffix("_")
    id = AbsoluteId(uuid)
    parentId("_Root")
    name = model.rootProjectName

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


    var prevStage: Stage? = null
    model.stages.forEach { stage ->
        val stageProject = StageProject(model, stage, uuid)
        val stagePasses = StagePasses(model, stage, prevStage, stageProject)
        buildType(stagePasses)
        subProject(stageProject)
        prevStage = stage
    }

    if (model.stages.map { stage -> stage.performanceTests }.flatten().isNotEmpty()) {
        subProject(WorkersProject(model))
    }

    buildTypesOrder = buildTypes
    subProjectsOrder = subProjects
})
