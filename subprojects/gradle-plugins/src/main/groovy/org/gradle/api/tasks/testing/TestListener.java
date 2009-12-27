/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.tasks.testing;

// todo: consider multithreading/multiprocess issues
// Teamcity has the concept of a "wave" of messages
// where each thread/process uses a unique wave id

/**
 * Interface for listening to test execution.  The intent is to be
 * framework agnostic.  Currently this interface can support feedback
 * from JUnit and TestNG tests.
 */
public interface TestListener {

    /**
     * Called before a test suite is started.
     * @param suite The suite whose tests are about to be executed.
     */
    void beforeSuite(TestSuite suite);

    /**
     * Called after a test suite is finished.
     * @param suite The suite whose tests have finished being executed.
     */
    void afterSuite(TestSuite suite);

    /**
     * Called before a test is started.
     * @param test The test which is about to be executed.
     */
    void beforeTest(Test test);

    /**
     * Called after a test is finished.
     * @param test The test which has finished executing.
     */
    void afterTest(Test test, TestResult result);
}