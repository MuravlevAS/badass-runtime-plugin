/*
 * Copyright 2018 the original author or authors.
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
package org.beryx.runtime

import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

class RuntimePlugin implements Plugin<Project> {
    final static String EXTENSION_NAME = 'runtime'
    final static String TASK_NAME_JRE = 'jre'
    final static String TASK_NAME_RUNTIME = 'runtime'
    final static String TASK_NAME_RUNTIME_ZIP = 'runtimeZip'
    final static String TASK_NAME_SUGGEST_MODULES = 'suggestModules'
    final static String TASK_NAME_JPACKAGE_IMAGE = 'jpackageImage'
    final static String TASK_NAME_JPACKAGE = 'jpackage'

    @CompileStatic
    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('7.0')) {
            throw new GradleException("This plugin requires Gradle 7.0 or newer. Try org.beryx.runtime 1.12.7 if you must use an older version of Gradle.")
        }
        project.getPluginManager().apply('application');
        if(hasModuleInfo(project)) {
            throw new GradleException("This plugin works only with non-modular applications.\n" +
                    "For modular applications use https://github.com/beryx/badass-jlink-plugin/.")
        }
        project.extensions.create(EXTENSION_NAME, RuntimePluginExtension, project)
        project.tasks.create(TASK_NAME_JRE, JreTask)
        project.tasks.create(TASK_NAME_RUNTIME, RuntimeTask)
        project.tasks.create(TASK_NAME_RUNTIME_ZIP, RuntimeZipTask)
        project.tasks.create(TASK_NAME_SUGGEST_MODULES, SuggestModulesTask)
        project.tasks.create(TASK_NAME_JPACKAGE_IMAGE, JPackageImageTask)
        project.tasks.create(TASK_NAME_JPACKAGE, JPackageTask)
    }

    static boolean hasModuleInfo(Project project) {
        Set<File> srcDirs = project.sourceSets.main?.java?.srcDirs
        srcDirs?.any { it.list()?.contains('module-info.java')}
    }

}
