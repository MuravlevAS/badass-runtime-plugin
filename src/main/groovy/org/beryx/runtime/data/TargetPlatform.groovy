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
package org.beryx.runtime.data


import groovy.transform.CompileStatic
import org.beryx.runtime.util.JdkUtil
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class TargetPlatform implements Serializable {
    private static final Logger LOGGER = Logging.getLogger(TargetPlatform.class)

    transient private final Project project
    final String name
    private Serializable jdkHome
    List<String> options = []

    TargetPlatform(Project project, String name, String jdkHome = '', List<String> options = []) {
        this.project = project
        this.name = name
        this.@jdkHome = jdkHome
        this.options.addAll(options)
    }
    String getJdkHome() {
        (this.@jdkHome == null) ? null : this.@jdkHome.toString()
    }

    void setJdkHome(Serializable jdkHome) {
        this.@jdkHome = jdkHome
    }

    void addOptions(String... opts) {
        opts.each { String opt -> options.add(opt) }
    }

    private static class LazyString implements Serializable {
        final Closure<String> closure
        LazyString(Closure<String> closure) {
            this.closure = closure
        }

        @Lazy String string = closure.call()

        @Override
        String toString() {
            string
        }
    }

    LazyString jdkDownload(String downloadUrl, Closure downloadConfig = null) {
        def options = new JdkUtil.JdkDownloadOptions(project, name, downloadUrl)
        if(downloadConfig) {
            downloadConfig.delegate = options
            downloadConfig(options)
        }
        return new LazyString({
            def relativePathToHome = JdkUtil.downloadFrom(downloadUrl, options)
            def pathToHome = "$options.downloadDir/$relativePathToHome"
            LOGGER.info("Home of downloaded JDK distribution: $pathToHome")
            return pathToHome as String
        })
    }
}
