package projects

import configurations.FunctionalTest
import configurations.PerformanceTestCoordinator
import configurations.SanityCheck
import configurations.buildReportTab
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.FailureAction
import jetbrains.buildServer.configs.kotlin.v2019_2.IdOwner
import jetbrains.buildServer.configs.kotlin.v2019_2.Project
import model.CIBuildModel
import model.SpecificBuild
import model.Stage
import model.TestType

class StageProject(model: CIBuildModel, stage: Stage, rootProjectUuid: String) : Project({
    this.uuid = "${model.projectPrefix}Stage_${stage.stageName.uuid}"
    this.id = AbsoluteId("${model.projectPrefix}Stage_${stage.stageName.id}")
    this.name = stage.stageName.stageName
    this.description = stage.stageName.description
}) {
    val specificBuildTypes: List<BuildType>

    val performanceTests: List<PerformanceTestCoordinator>

    val functionalTests: List<FunctionalTest>

    init {
        features {
            if (stage.specificBuilds.contains(SpecificBuild.SanityCheck)) {
                buildReportTab("API Compatibility Report", "report-distributions-binary-compatibility-report.html")
                buildReportTab("Incubating APIs Report", "incubation-reports/all-incubating.html")
            }
            if (stage.performanceTests.isNotEmpty()) {
                buildReportTab("Performance", "report-performance-performance-tests.zip!report/index.html")
            }
        }

        specificBuildTypes = stage.specificBuilds.map {
            it.create(model, stage)
        }
        specificBuildTypes.forEach(this::buildType)

        performanceTests = stage.performanceTests.map { PerformanceTestCoordinator(model, it, stage) }
        performanceTests.forEach(this::buildType)

        val (topLevelCoverage, allCoverage) = stage.functionalTests.partition { it.testType == TestType.soak }
        val topLevelFunctionalTests = topLevelCoverage
            .map { FunctionalTest(model, it.asConfigurationId(model), it.asName(), it.asName(), it, stage = stage) }

        val coverageFunctionalTests = allCoverage
            .map { testCoverage ->
                val coverageFunctionalTest = FunctionalTest(
                    model,
                    testCoverage.asId(model),
                    testCoverage.asName(),
                    testCoverage.asName(),
                    testCoverage,
                    stage)

                if (stage.functionalTestsDependOnSpecificBuilds) {
                    specificBuildTypes.forEach(coverageFunctionalTest::dependsOn)
                }
                if (!(stage.functionalTestsDependOnSpecificBuilds && stage.specificBuilds.contains(SpecificBuild.SanityCheck)) && stage.dependsOnSanityCheck) {
                    coverageFunctionalTest.dependsOn(AbsoluteId(SanityCheck.buildTypeId(model)))
                }
                coverageFunctionalTest
            }

        functionalTests = topLevelFunctionalTests + coverageFunctionalTests
        functionalTests.forEach(this::buildType)
    }
}

private fun FunctionalTest.dependsOn(dependency: IdOwner) {
//    dependencies {
//        dependency(dependency) {
//            snapshot {
//                onDependencyFailure = FailureAction.CANCEL
//                onDependencyCancel = FailureAction.CANCEL
//            }
//        }
//    }
}
