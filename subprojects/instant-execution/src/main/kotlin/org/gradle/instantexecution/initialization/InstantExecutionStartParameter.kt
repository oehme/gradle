/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.instantexecution.initialization

import org.gradle.StartParameter
import org.gradle.api.internal.StartParameterInternal
import org.gradle.initialization.StartParameterBuildOptions.ConfigurationCacheOption
import org.gradle.initialization.layout.BuildLayout
import org.gradle.instantexecution.extensions.unsafeLazy
import org.gradle.internal.classpath.BuildLogicTransformStrategy
import org.gradle.internal.classpath.CachedClasspathTransformer.StandardTransform.BuildLogic
import org.gradle.internal.classpath.CachedClasspathTransformer.StandardTransform.None
import org.gradle.internal.service.scopes.Scopes
import org.gradle.internal.service.scopes.ServiceScope
import java.io.File


@ServiceScope(Scopes.BuildTree)
class InstantExecutionStartParameter(
    private val buildLayout: BuildLayout,
    startParameter: StartParameter
) : BuildLogicTransformStrategy {

    private
    val startParameter = startParameter as StartParameterInternal

    val isEnabled: Boolean
        get() = startParameter.configurationCache != ConfigurationCacheOption.Value.OFF

    val isQuiet: Boolean
        get() = startParameter.isConfigurationCacheQuiet

    val maxProblems: Int
        get() = startParameter.configurationCacheMaxProblems

    val failOnProblems: Boolean
        get() = startParameter.configurationCache == ConfigurationCacheOption.Value.ON

    val recreateCache: Boolean
        get() = startParameter.isConfigurationCacheRecreateCache

    val currentDirectory: File
        get() = startParameter.currentDir

    val settingsDirectory: File
        get() = buildLayout.settingsDir

    val rootDirectory: File
        get() = buildLayout.rootDirectory

    val isRefreshDependencies
        get() = startParameter.isRefreshDependencies

    val requestedTaskNames: List<String> by unsafeLazy {
        startParameter.taskNames
    }

    val excludedTaskNames: Set<String>
        get() = startParameter.excludedTaskNames

    override fun transformToApplyToBuildLogic() = if (isEnabled) {
        BuildLogic
    } else {
        // For now, disable instrumentation when configuration caching is not used
        // This means that build logic will use different classpaths when the configuration cache is enabled or disabled
        None
    }

    val allInitScripts: List<File>
        get() = startParameter.allInitScripts
}
