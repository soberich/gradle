/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Gradle_Check.configurations

import common.Os
import common.applyDefaultSettings
import configurations.BaseGradleBuildType
import configurations.gradleRunnerStep
import configurations.publishBuildStatusToGithub
import configurations.snapshotDependencies
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.ReuseBuilds
import model.CIBuildModel
import model.PerformanceTestType
import projects.PerformanceTestProject

class PerformanceTestsPass(model: CIBuildModel, performanceTestProject: PerformanceTestProject) : BaseGradleBuildType(model, init = {
    uuid = performanceTestProject.uuid + "_Trigger"
    id = AbsoluteId(uuid)
    name = performanceTestProject.name + " (Trigger)"

    val os = Os.LINUX
    val type = performanceTestProject.performanceTestCoverage.performanceTestType

    applyDefaultSettings(os)
    params {
        param("env.GRADLE_OPTS", "-Xmx1536m -XX:MaxPermSize=384m")
        param("env.JAVA_HOME", os.buildJavaHome())
        param("env.BUILD_BRANCH", "%teamcity.build.branch%")
        param("performance.db.username", "tcagent")
        param("performance.channel", performanceTestProject.performanceTestCoverage.channel())
    }

    features {
        publishBuildStatusToGithub(model)
    }

    val performanceResultsDir = "perf-results"
    val performanceProjectName = "performance"

    val taskName = if (performanceTestProject.performanceTestCoverage.performanceTestType == PerformanceTestType.flakinessDetection)
        "performanceTestFlakinessReport"
    else
        "performanceTestReport"

    artifactRules = """
$performanceResultsDir => perf-results/
subprojects/$performanceProjectName/build/$taskName => report/
"""

    gradleRunnerStep(
        model,
        ":$performanceProjectName:$taskName --channel %performance.channel%",
        extraParameters = listOf(
            "-Porg.gradle.performance.branchName" to "%teamcity.build.branch%",
            "-Porg.gradle.performance.db.url" to "%performance.db.url%",
            "-Porg.gradle.performance.db.username" to "%performance.db.username%",
            "-Porg.gradle.performance.db.password" to "%performance.db.password.tcagent%"
        ).joinToString(" ") { (key, value) -> os.escapeKeyValuePair(key, value) }
    )

    dependencies {
        snapshotDependencies(performanceTestProject.performanceTests) {
            if (type == PerformanceTestType.flakinessDetection) {
                reuseBuilds = ReuseBuilds.NO
            }
        }
        performanceTestProject.performanceTests.forEach {
            if (it.testProjects.isNotEmpty()) {
                artifacts(it.id!!) {
                    id = "ARTIFACT_DEPENDENCY_${it.id!!}"
                    cleanDestination = true
                    artifactRules = "results/performance/build/test-results-*.zip!performance-tests/perf-results*.json => $performanceResultsDir/${it.bucketIndex}/"
                }
            }
        }
    }
})
