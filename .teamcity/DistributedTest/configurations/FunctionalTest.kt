package configurations

import common.Os
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import model.CIBuildModel
import model.Stage
import model.TestCoverage
import model.TestType

class FunctionalTest(
    model: CIBuildModel,
    uuid: String,
    name: String,
    description: String,
    testCoverage: TestCoverage,
    stage: Stage,
    extraParameters: String = "",
    extraBuildSteps: BuildSteps.() -> Unit = {},
    preBuildSteps: BuildSteps.() -> Unit = {}
) : BaseGradleBuildType(model, stage = stage, init = {
    this.uuid = uuid
    this.name = name
    this.description = description
    id = AbsoluteId(uuid)
    val testTaskName = "${testCoverage.testType.name}Test"
    val quickTest = testCoverage.testType == TestType.quick
    val buildScanTags = listOf("FunctionalTest")
    val buildScanValues = mapOf(
        "coverageOs" to testCoverage.os.name,
        "coverageJvmVendor" to testCoverage.vendor.name,
        "coverageJvmVersion" to testCoverage.testJvmVersion.name
    )

    if (name.contains("(instantExecution)")) {
        requirements {
            doesNotContain("teamcity.agent.name", "ec2")
            // US region agents have name "EC2-XXX"
            doesNotContain("teamcity.agent.name", "EC2")
        }
    }

    applyTestDefaults(model, this, testTaskName, notQuick = !quickTest, os = testCoverage.os,
        extraParameters = (
            listOf(""""-PtestJavaHome=%${testCoverage.os}.${testCoverage.testJvmVersion}.${testCoverage.vendor}.64bit%"""") +
                buildScanTags.map { buildScanTag(it) } +
                buildScanValues.map { buildScanCustomValue(it.key, it.value) } "-DenableTestDistribution=true -Dscan.tag.test-distribution -Dgradle.enterprise.url=https://e.grdev.net" +
                    extraParameters
            ).filter { it.isNotBlank() }.joinToString(separator = " "),
        timeout = testCoverage.testType.timeout,
        extraSteps = extraBuildSteps,
        preSteps = preBuildSteps)

    params {
        param("env.JAVA_HOME", "%${testCoverage.os}.${testCoverage.buildJvmVersion}.openjdk.64bit%")
        when (testCoverage.os) {
            Os.linux -> {
                param("env.ANDROID_HOME", "/opt/android/sdk")
            }
            Os.macos -> {
                param("env.ANDROID_HOME", "/opt/android/sdk")
                // Use fewer parallel forks on macOs, since the agents are not very powerful.
                param("maxParallelForks", "2")
            }
            Os.windows -> {
                param("env.ANDROID_HOME", """C:\Program Files\android\sdk""")
            }
        }

        if (testCoverage.testDistribution) {
            param("maxParallelForks", "16")
        }
    }
})
