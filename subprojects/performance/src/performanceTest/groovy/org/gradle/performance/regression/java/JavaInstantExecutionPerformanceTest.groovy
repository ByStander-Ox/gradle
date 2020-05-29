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

package org.gradle.performance.regression.java

import org.gradle.initialization.StartParameterBuildOptions.ConfigurationCacheOption
import org.gradle.integtests.fixtures.executer.GradleDistribution
import org.gradle.performance.AbstractCrossVersionGradleInternalPerformanceTest
import org.gradle.performance.categories.PerformanceRegressionTest
import org.gradle.performance.fixture.BuildExperimentInvocationInfo
import org.gradle.performance.fixture.BuildExperimentListener
import org.gradle.performance.fixture.BuildExperimentListenerAdapter
import org.gradle.performance.fixture.GradleInvocationSpec
import org.gradle.performance.measure.MeasuredOperation
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.GradleVersion
import org.junit.experimental.categories.Category
import spock.lang.Unroll

import java.nio.file.Files

import static org.gradle.performance.generator.JavaTestProject.LARGE_JAVA_MULTI_PROJECT_NO_BUILD_SRC
import static org.gradle.performance.generator.JavaTestProject.SMALL_JAVA_MULTI_PROJECT_NO_BUILD_SRC
import static org.junit.Assert.assertTrue

@Category(PerformanceRegressionTest)
class JavaInstantExecutionPerformanceTest extends AbstractCrossVersionGradleInternalPerformanceTest {

    private TestFile instantExecutionStateDir

    def setup() {
        instantExecutionStateDir = temporaryFolder.file(".instant-execution-state")
    }

    @Unroll
    def "assemble on #testProject #action instant execution state with #daemon daemon"() {

        given:
        runner.targetVersions = ["6.6-branch-eskatos_cc_rework_build_options-20200527184619+0000"]
        runner.minimumBaseVersion = "5.6"
        runner.testProject = testProject.projectName
        runner.tasksToRun = ["assemble"]
        // runner.args = ["-D${ConfigurationCacheOption.PROPERTY_NAME}=true"] // TODO uncomment on rebaseline

        and:
        runner.useDaemon = daemon == hot
        runner.addBuildExperimentListener(listenerFor(action))
        runner.warmUpRuns = daemon == hot ? 20 : 1
        runner.runs = daemon == hot ? 60 : 25

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        testProject                           | daemon | action
        LARGE_JAVA_MULTI_PROJECT_NO_BUILD_SRC | hot    | loading
        LARGE_JAVA_MULTI_PROJECT_NO_BUILD_SRC | hot    | storing
        LARGE_JAVA_MULTI_PROJECT_NO_BUILD_SRC | cold   | loading
        LARGE_JAVA_MULTI_PROJECT_NO_BUILD_SRC | cold   | storing
        SMALL_JAVA_MULTI_PROJECT_NO_BUILD_SRC | hot    | loading
        SMALL_JAVA_MULTI_PROJECT_NO_BUILD_SRC | hot    | storing
//        SMALL_JAVA_MULTI_PROJECT_NO_BUILD_SRC | cold   | loading
//        SMALL_JAVA_MULTI_PROJECT_NO_BUILD_SRC | cold   | storing
    }

    private BuildExperimentListener listenerFor(String action) {
        return instantInvocationListenerFor(action, instantExecutionStateDir)
    }

    static String loading = "loading"
    static String storing = "storing"
    static String hot = "hot"
    static String cold = "cold"

    static BuildExperimentListener instantInvocationListenerFor(String action, File stateDir) {
        return new BuildExperimentListenerAdapter() {

            @Override
            void beforeInvocation(BuildExperimentInvocationInfo invocationInfo) {
                if (action == storing) {
                    stateDir.deleteDir()
                }
                // TODO remove on rebaseline
                def invocation = invocationInfo.buildExperimentSpec.invocation as GradleInvocationSpec
                def dist = invocation.gradleDistribution as GradleDistribution
                def customizer = invocationInfo.buildExperimentSpec.invocationCustomizer
                if (dist.version < GradleVersion.version("6.5-rc-1")) {
                    customizer.customize(invocationInfo, invocation.withAdditionalArgs(['-Dorg.gradle.unsafe.instant-execution=true']))
                } else if (dist.version < GradleVersion.version("6.6-20200527184619+0000")) {
                    customizer.customize(invocationInfo, invocation.withAdditionalArgs(["--${ConfigurationCacheOption.LONG_OPTION}=on"]))
                } else {
                    customizer.customize(invocationInfo, invocation.withAdditionalArgs(["--${ConfigurationCacheOption.LONG_OPTION}"]))
                }
            }

            @Override
            void afterInvocation(BuildExperimentInvocationInfo invocationInfo, MeasuredOperation operation, BuildExperimentListener.MeasurementCallback measurementCallback) {
                if (invocationInfo.iterationNumber > 1) {
                    // TODO dedupe on rebaseline
                    def tags = action == storing
                        ? ["Calculating task graph as no instant execution cache is available", "Calculating task graph as no configuration cache is available"]
                        : ["Reusing instant execution cache", "Reusing configuration cache"]
                    def found = Files.lines(invocationInfo.buildLog.toPath()).withCloseable { lines ->
                        lines.anyMatch { line -> tags.any { line.contains(it) } }
                    }
                    if (!found) {
                        assertTrue("Configuration cache log '$tags' not found in '$invocationInfo.buildLog'\n\n$invocationInfo.buildLog.text", found)
                    }
                }
            }
        }
    }
}
