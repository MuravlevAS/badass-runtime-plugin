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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.LauncherData
import org.beryx.runtime.data.RuntimeTaskData
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.RuntimeTaskImpl
import org.gradle.api.GradleException
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.Directory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.jvm.application.scripts.ScriptGenerator
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator
import org.gradle.util.GradleVersion

@CompileStatic
class RuntimeTask extends BaseTask {
    @Input
    Map<String, TargetPlatform> getTargetPlatforms() {
        extension.targetPlatforms.get()
    }

    @Input
    LauncherData getLauncherData() {
        extension.launcherData.get()
    }

    @InputDirectory
    Directory getDistDir() {
        extension.distDir.getOrNull() ?: project.layout.buildDirectory.dir(distTask.destinationDir.path).get()
    }

    @OutputDirectory
    Directory getJreDir() {
        extension.jreDir.get()
    }

    @Internal
    Directory getImageDir() {
        extension.imageDir.get()
    }

    @Internal
    Sync getDistTask() {
        (Sync)(project.tasks.findByName('installShadowDist') ?: project.tasks.getByName('installDist'))
    }

    @CompileDynamic
    RuntimeTask() {
        description = 'Creates a runtime image of your application'
        dependsOn(RuntimePlugin.TASK_NAME_JRE)
        project.afterEvaluate {
            dependsOn(distTask)
        }
        project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
            configureStartScripts(taskGraph.hasTask(this))
        }
    }

    @OutputDirectory
    File getImageDirAsFile() {
        imageDir.asFile
    }

    void configureStartScripts(boolean asRuntimeImage) {
        project.tasks.withType(CreateStartScripts) { CreateStartScripts startScriptTask ->
            startScriptTask.unixStartScriptGenerator
            startScriptTask.doLast {
                startScriptTask.unixScript.text = startScriptTask.unixScript.text.replace('{{BIN_DIR}}', '$APP_HOME/bin')
                startScriptTask.windowsScript.text = startScriptTask.windowsScript.text.replace('{{BIN_DIR}}', '%~dp0')
            }
            // workaround for shadow bug https://github.com/johnrengelman/shadow/issues/572
            if(GradleVersion.current() >= GradleVersion.version('6.4')) {
                if(!startScriptTask.mainClass.present) {
                    startScriptTask.mainClass.set(startScriptTask.mainClassName)
                }
            }
            startScriptTask.inputs.property('asRuntimeImage', asRuntimeImage)
            if(asRuntimeImage) {
                configureTemplate(startScriptTask.unixStartScriptGenerator, launcherData.unixScriptTemplate, '/unixScriptTemplate.txt')
                configureTemplate(startScriptTask.windowsStartScriptGenerator, launcherData.windowsScriptTemplate, '/windowsScriptTemplate.txt')
            }
        }
    }

    void configureTemplate(ScriptGenerator scriptGenerator, File customTemplate, String resourceTemplate) {
        def template = customTemplate ? customTemplate.toURI().toURL() : RuntimePlugin.class.getResource(resourceTemplate)
        if(!template) throw new GradleException("Resource $resourceTemplate not found.")
        ((TemplateBasedScriptGenerator)scriptGenerator).template = project.resources.text.fromString(template.text)
    }

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new RuntimeTaskData()
        taskData.distDir = distDir.asFile
        taskData.jreDir = jreDir.asFile
        taskData.imageDir = imageDir.asFile
        taskData.targetPlatforms = targetPlatforms

        def taskImpl = new RuntimeTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
