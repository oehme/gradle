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

package org.gradle.gradlebuild.packaging

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.runtimeshaded.PackageListGenerator
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.build.docs.dsl.source.ExtractDslMetaDataTask
import org.gradle.build.docs.dsl.source.GenerateApiMapping
import org.gradle.build.docs.dsl.source.GenerateDefaultImports
import org.gradle.gradlebuild.PublicApi
import org.gradle.gradlebuild.docs.GradleUserManualPlugin
import org.gradle.gradlebuild.versioning.buildVersion
import org.gradle.kotlin.dsl.*


@Suppress("unused")
open class GradleDistributionsPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        val runtimeApiJarName = "gradle-runtime-api-info"

        // Configurations to define dependencies
        val coreRuntimeOnly by bucket()
        val pluginsRuntimeOnly by bucket()
        val gradleScripts by bucket(listOf(":launcher"))

        // Configurations to resolve dependencies
        val runtimeClasspath by libraryResolver(listOf(coreRuntimeOnly, pluginsRuntimeOnly))
        val coreRuntimeClasspath by libraryResolver(listOf(coreRuntimeOnly))
        val gradleScriptPath by startScriptResolver(listOf(gradleScripts))
        val sourcesPath by sourcesResolver(listOf(coreRuntimeOnly, pluginsRuntimeOnly))

        // Tasks to generate metadata about the distribution that is required at runtime
        val generateGradleApiPackageList by tasks.registering(PackageListGenerator::class) {
            classpath = runtimeClasspath
            outputFile = file(generatedTxtFileFor("api-relocated"))
        }

        val dslMetaData by tasks.registering(ExtractDslMetaDataTask::class) {
            source(sourcesPath.incoming.artifactView { lenient(true) }.files.asFileTree.matching {
                // Filter out any non-public APIs
                include(PublicApi.includes)
                exclude(PublicApi.excludes)
            })
            destinationFile.set(generatedBinFileFor("dsl-meta-data.bin"))
        }

        val apiMapping by tasks.registering(GenerateApiMapping::class) {
            metaDataFile.set(dslMetaData.flatMap(ExtractDslMetaDataTask::getDestinationFile))
            mappingDestFile.set(generatedTxtFileFor("api-mapping"))
            excludedPackages.set(GradleUserManualPlugin.getDefaultExcludedPackages())
        }

        val defaultImports = tasks.register("defaultImports", GenerateDefaultImports::class) {
            metaDataFile.set(dslMetaData.flatMap(ExtractDslMetaDataTask::getDestinationFile))
            importsDestFile.set(generatedTxtFileFor("default-imports"))
            excludedPackages.set(GradleUserManualPlugin.getDefaultExcludedPackages())
        }

        val emptyClasspathManifest by tasks.registering(ClasspathManifest::class) {
            // At runtime, Gradle expects each Gradle jar to have a classpath manifest.
            archiveBaseName.set(runtimeApiJarName)
            generatedResourcesDir.set(layout.buildDirectory.dir("generated-resources/classpath-manifest"))
        }

        // Jar task that packages all metadata in 'gradle-runtime-api-info.jar'
        val jar by tasks.registering(Jar::class) {
            val baseVersion = rootProject.buildVersion.baseVersion
            archiveVersion.set(baseVersion)
            archiveBaseName.set(runtimeApiJarName)
            into("org/gradle/api/internal/runtimeshaded") {
                from(generateGradleApiPackageList)
            }
            from(apiMapping)
            from(defaultImports)
            from(emptyClasspathManifest)
        }

        val assembleBinDistribution = tasks.register<Sync>("assembleBinDistribution") {
            group = "distribution"
            into(layout.buildDirectory.dir("bin distribution"))

            from("$rootDir/LICENSE")
            from("src/toplevel")

            into("bin") {
                from(gradleScriptPath)
                fileMode = Integer.parseInt("0755", 8)
            }

            into("lib") {
                from(jar)
                from(coreRuntimeClasspath)
                into("plugins") {
                    from(runtimeClasspath - coreRuntimeClasspath)
                }
            }

            doLast {
                ant.withGroovyBuilder {
                    "chmod"("dir" to "$destinationDir/bin", "perm" to "ugo+rx", "includes" to "**/*")
                }
            }
        }

        // A standard Java runtime variant for embedded integration testing
        consumableVariant("runtime", LibraryElements.JAR, Bundling.EXTERNAL, listOf(coreRuntimeOnly, pluginsRuntimeOnly), jar)
        // To make all source code of a distribution accessible transitively
        consumableSourcesVariant("transitiveSources", listOf(coreRuntimeOnly, pluginsRuntimeOnly))
        // A platform variant without 'runtime-api-info' artifact such that distributions can depend on each other
        consumablePlatformVariant("runtimePlatform", listOf(coreRuntimeOnly, pluginsRuntimeOnly))
        // A variant providing a folder where the distribution is present in the final format for forked integration testing
        consumableVariant("distributionUnpackedRuntime", "gradle-distribution-jars", Bundling.EMBEDDED, emptyList(), mapOf(
            // TODO: https://github.com/gradle/gradle/issues/13275: missing property in Sync task - assembleBinDistribution.flatMap(Sync::getDestinationDirectory())
            "file" to assembleBinDistribution.get().destinationDir,
            "builtBy" to assembleBinDistribution)
        )
    }

    private
    fun Project.generatedBinFileFor(name: String) =
        layout.buildDirectory.file("generated-resources/$name/$name.bin")

    private
    fun Project.generatedTxtFileFor(name: String) =
        layout.buildDirectory.file("generated-resources/$name/$name.txt")

    private
    fun Project.bucket(defaultProjectDependencies: List<String> = emptyList()): NamedDomainObjectContainerCreatingDelegateProvider<Configuration> =
        configurations.creating {
            isCanBeResolved = false
            isCanBeConsumed = false
            isVisible = false
            withDependencies {
                defaultProjectDependencies.forEach { add(this@bucket.dependencies.create(project(it))) }
            }
        }

    private
    fun Project.libraryResolver(extends: List<Configuration>): NamedDomainObjectContainerCreatingDelegateProvider<Configuration> =
        configurations.creating {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            }
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            extends.forEach { extendsFrom(it) }
        }

    private
    fun Project.startScriptResolver(extends: List<Configuration>): NamedDomainObjectContainerCreatingDelegateProvider<Configuration> =
        configurations.creating {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("start-scripts"))
            }
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            extends.forEach { extendsFrom(it) }
        }

    private
    fun Project.sourcesResolver(extends: List<Configuration>): NamedDomainObjectContainerCreatingDelegateProvider<Configuration> =
        configurations.creating {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
                attribute(Attribute.of("org.gradle.docselements", String::class.java), "sources")
            }
            isCanBeResolved = true
            isCanBeConsumed = false
            isVisible = false
            extends.forEach { extendsFrom(it) }
        }

    private
    fun Project.consumableVariant(name: String, elements: String, bundling: String, extends: List<Configuration>, artifact: Any) =
        configurations.create("${name}Elements") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(elements))
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(bundling))
            }
            isCanBeResolved = false
            isCanBeConsumed = true
            isVisible = false
            extends.forEach { extendsFrom(it) }
            outgoing.artifact(artifact)
        }

    private
    fun Project.consumableSourcesVariant(name: String, extends: List<Configuration>) =
        configurations.create("${name}Elements") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            }
            isCanBeResolved = false
            isCanBeConsumed = true
            isVisible = false
            extends.forEach { extendsFrom(it) }
        }

    private
    fun Project.consumablePlatformVariant(name: String, extends: List<Configuration>) =
        configurations.create("${name}Elements") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.REGULAR_PLATFORM))
            }
            isCanBeResolved = false
            isCanBeConsumed = true
            isVisible = false
            extends.forEach { extendsFrom(it) }
        }
}
