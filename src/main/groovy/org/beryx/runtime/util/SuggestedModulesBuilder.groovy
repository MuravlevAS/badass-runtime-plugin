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
package org.beryx.runtime.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class SuggestedModulesBuilder {
    private static final Logger LOGGER = Logging.getLogger(SuggestedModulesBuilder)

    final String javaHome

    SuggestedModulesBuilder(String javaHome) {
        this.javaHome = javaHome
    }

    Set<String> getProjectModules(Project project) {
        Set<String> modules = []
        def mainDistJarFile = Util.getMainDistJarFile(project)
        modules.addAll(getModulesRequiredBy(mainDistJarFile))
        for(ResolvedDependency dep: project.configurations['runtimeClasspath'].resolvedConfiguration.firstLevelModuleDependencies) {
            def f = Util.getArtifact(dep)
            if(f) modules.addAll(getModulesRequiredBy(f))
        }
        if(!modules) {
            modules << 'java.base'
        }
        modules
    }

    Set<String> getModulesRequiredBy(File jarOrDir) {
        LOGGER.debug("Detecting modules required by $jarOrDir")
        def scanner = new PackageUseScanner()
        def invalidFiles = scanner.scan(jarOrDir)
        if(invalidFiles) {
            LOGGER.warn("Failed to scan: $invalidFiles")
        }
        LOGGER.debug("External packages used by the merged service:\n\t${scanner.externalPackages.join('\n\t')}")

        def moduleManager = new ModuleManager(new File("$javaHome/jmods"))
        def modules = new HashSet<String>()

        scanner.externalPackages.each { pkg ->
            def moduleName = moduleManager.exportMap[pkg]
            if(!moduleName) {
                LOGGER.info("Cannot find module exporting $pkg")
            } else if(moduleName != 'java.base'){
                modules << moduleName
            }
        }
        modules
    }
}
