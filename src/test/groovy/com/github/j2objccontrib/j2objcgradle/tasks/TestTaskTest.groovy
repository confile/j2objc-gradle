/*
 * Copyright (c) 2015 the authors of j2objc-gradle (see AUTHORS file)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.j2objccontrib.j2objcgradle.tasks

import com.github.j2objccontrib.j2objcgradle.J2objcConfig
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * TestTask tests.
 */
class TestTaskTest {

    // Configured with setupTask()
    private Project proj
    private String j2objcHome
    private J2objcConfig j2objcConfig
    private TestTask j2objcTest

    @Test
    void testGetTestNames_Simple() {

        // These are nonsense paths for files that don't exist
        proj = ProjectBuilder.builder().build()
        FileCollection srcFiles = proj.files([
                "${proj.rootDir}/src/test/java/com/example/parent/ParentClass.java",
                "${proj.rootDir}/src/test/java/com/example/parent/subdir/SubdirClass.java",
                "${proj.rootDir}/src/test/java/com/example/other/OtherClass.java"])
        Properties noPackagePrefixes = new Properties()

        List<String> testNames = TestTask.getTestNames(proj, srcFiles, noPackagePrefixes)

        List<String> expectedTestNames = [
                "com.example.parent.ParentClass",
                "com.example.parent.subdir.SubdirClass",
                "com.example.other.OtherClass"]

        assert expectedTestNames == testNames
    }

    @Test
    void testGetTestNames_PackagePrefixes() {
        Properties packagePrefixes = new Properties()
        packagePrefixes.setProperty('com.example.parent', 'PrntPrefix')
        packagePrefixes.setProperty('com.example.parent.subdir', 'SubPrefix')
        packagePrefixes.setProperty('com.example.other', 'OthPrefix')

        // These are nonsense paths for files that don't exist
        proj = ProjectBuilder.builder().build()
        FileCollection srcFiles = proj.files([
                "${proj.rootDir}/src/test/java/com/example/parent/ParentOneClass.java",
                "${proj.rootDir}/src/test/java/com/example/parent/ParentTwoClass.java",
                "${proj.rootDir}/src/test/java/com/example/parent/subdir/SubdirClass.java",
                "${proj.rootDir}/src/test/java/com/example/other/OtherClass.java",
                "${proj.rootDir}/src/test/java/com/example/noprefix/NoPrefixClass.java"])

        List<String> testNames = TestTask.getTestNames(proj, srcFiles, packagePrefixes)

        List<String> expectedTestNames = [
                "PrntPrefixParentOneClass",
                "PrntPrefixParentTwoClass",
                "SubPrefixSubdirClass",
                "OthPrefixOtherClass",
                // No package prefix in this case
                "com.example.noprefix.NoPrefixClass"]

        assert expectedTestNames == testNames
    }

    private void setupTask() {
        (proj, j2objcHome, j2objcConfig) = TestingUtils.setupProject(new TestingUtils.ProjectConfig(
                applyJavaPlugin: true,
                createJ2objcConfig: true,
                createReportsDir: true,
        ))

        j2objcTest = (TestTask) proj.tasks.create(name: 'j2objcTest', type: TestTask) {
            testBinaryFile = proj.file("${proj.buildDir}/testJ2objc")
        }
    }

    @Test
    void test_NoTests() {
        setupTask()

        MockProjectExec mockProjectExec = new MockProjectExec(proj, j2objcHome)
        mockProjectExec.demandExecAndReturn(
                [
                        "${proj.buildDir}/testJ2objc",
                        "org.junit.runner.JUnitCore",
                        "[]"
                ],
                // Fake test output
                'IGNORE\nOK (2 tests)\nIGNORE',
                // stderr
                '',
                null)

        j2objcTest.test()

        mockProjectExec.verify()
    }

    @Test
    void test_OneTest() {
        setupTask()

        MockProjectExec mockProjectExec = new MockProjectExec(proj, j2objcHome)
        mockProjectExec.demandExecAndReturn(
                [
                        "${proj.buildDir}/testJ2objc",
                        "org.junit.runner.JUnitCore",
                        "[]"
                ],
                // NOTE: 'test' instead of 'tests'
                'OK (1 test)',
                // stderr
                '',
                null)

        j2objcTest.test()

        mockProjectExec.verify()
    }

    // TODO: test_Simple() - with some unit tests

    // TODO: test_Complex() - preferably using real project in src/test/resources
}
