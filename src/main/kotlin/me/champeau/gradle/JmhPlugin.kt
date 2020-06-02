package me.champeau.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.util.GradleVersion
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class JmhPlugin : Plugin<Project> {

    fun Project.sourceSets(block: SourceSetContainer.() -> Unit) =
            (properties["sourceSets"] as SourceSetContainer).block()

    val Project.sourceSets: SourceSetContainer
        get() = properties["sourceSets"] as SourceSetContainer

    fun SourceSetContainer.jmh(block: SourceSet.() -> Unit) =
            getByName("jmh").block()

    val SourceSetContainer.jmh: SourceSet
        get() = getByName("jmh")

    val SourceSetContainer.main: SourceSet
        get() = getByName("main")

    val SourceSetContainer.test: SourceSet
        get() = getByName("test")

    override fun apply(project: Project) {
        if (!IS_GRADLE_MIN_55)
            throw RuntimeException("This version of the JMH Gradle plugin requires Gradle 5.5+. Please upgrade Gradle or use an older version of the plugin.")
        project.plugins.apply(JavaPlugin::class.java)
        val extension = project.extensions.create(JMH_NAME, JmhPluginExtension::class.java, project)
        val configuration = project.configurations.create(JMH_NAME)
        val runtimeConfiguration = createJmhRuntimeConfiguration(project, extension)

        val dependencyHandler = project.getDependencies()
        configuration.withDependencies {
            it += dependencyHandler.create("${JMH_CORE_DEPENDENCY}${extension.jmhVersion}")
            it += dependencyHandler.create("${JMH_GENERATOR_DEPENDENCY}${extension.jmhVersion}")
        }

        ensureTasksNotExecutedConcurrently(project)

        val hasShadow = project.plugins.findPlugin("com.github.johnrengelman.shadow") != null

        createJmhSourceSet(project)

        registerBuildListener(project, extension)

        val jmhGeneratedSourcesDir = project.file("$project.buildDir/jmh-generated-sources")
        val jmhGeneratedClassesDir = project.file("$project.buildDir/jmh-generated-classes")
        val jmhGeneratedResourcesDir = project.file("$project.buildDir/jmh-generated-resources")
        createJmhRunBytecodeGeneratorTask(project, jmhGeneratedSourcesDir, extension, jmhGeneratedResourcesDir)

        createJmhCompileGeneratedClassesTask(project, jmhGeneratedSourcesDir, jmhGeneratedClassesDir, extension)

        val metaInfExcludes = listOf("module-info.class", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        if (hasShadow)
            createShadowJmhJar(project, extension, jmhGeneratedResourcesDir, jmhGeneratedClassesDir, metaInfExcludes, runtimeConfiguration)
        else
            createStandardJmhJar(project, extension, metaInfExcludes, jmhGeneratedResourcesDir, jmhGeneratedClassesDir, runtimeConfiguration)

        project.tasks.register(JMH_NAME, JmhTask::class.java) {
            it.group = JMH_GROUP
            it.dependsOn(project.task("jmhJar"))
        }

        configureIDESupport(project)
    }

    private fun createJmhSourceSet(project: Project) {
        project.sourceSets {
            jmh {
                java.srcDir("src/jmh/java")
                resources.srcDir("src/jmh/resources")
                compileClasspath += main.output
                runtimeClasspath += main.output
            }
        }
        project.configurations.apply {
            // the following line is for backwards compatibility
            // no one should really add directly to the "jmh" configuration
            getAt("jmhImplementation").extendsFrom(getAt("jmh"))

            getAt("jmhCompileClasspath").extendsFrom(getAt("implementation"), getAt("compileOnly"))
            getAt("jmhRuntimeClasspath").extendsFrom(getAt("implementation"), getAt("runtimeOnly"))
        }
    }

    private fun registerBuildListener(project: Project, extension: JmhPluginExtension) {
        project.gradle.addBuildListener(object : BuildAdapter() {
            override fun projectsEvaluated(gradle: Gradle) {
                if (extension.includeTests.get())
                    project.sourceSets {
                        jmh {
                            compileClasspath += test.output + project.configurations.getAt("testCompileClasspath")
                            runtimeClasspath += test.output + project.configurations.getAt("testRuntimeClasspath")
                        }
                    }

                val task = project.tasks.named(JMH_JAR_TASK_NAME, Zip::class.java)
                task.get().isZip64 = extension.isZip64
            }
        })
    }

    private fun createJmhRunBytecodeGeneratorTask(
            project: Project, jmhGeneratedSourcesDir: File,
            extension: JmhPluginExtension, jmhGeneratedResourcesDir: File
    ) =
            project.tasks.create("jmhRunBytecodeGenerator", JmhBytecodeGeneratorTask::class.java) {
                it.group = JMH_GROUP
                it.dependsOn("jmhClasses")
                it.includeTestsState.set(extension.includeTests.get())
                it.generatedClassesDir = jmhGeneratedResourcesDir
                it.generatedSourcesDir = jmhGeneratedSourcesDir
            }

    private fun createJmhCompileGeneratedClassesTask(
            project: Project, jmhGeneratedSourcesDir: File,
            jmhGeneratedClassesDir: File, extension: JmhPluginExtension
    ) =
            project.tasks.create(JMH_TASK_COMPILE_GENERATED_CLASSES_NAME, JavaCompile::class.java) {
                it.group = JMH_GROUP
                it.dependsOn("jmhRunBytecodeGenerator")

                it.classpath = project.sourceSets.jmh.runtimeClasspath
                if (extension.includeTests.get())
                    it.classpath += project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath
                it.source(jmhGeneratedSourcesDir)
                it.destinationDir = jmhGeneratedClassesDir
            }

    private fun createShadowJmhJar(
            project: Project, extension: JmhPluginExtension, jmhGeneratedResourcesDir: File,
            jmhGeneratedClassesDir: File, metaInfExcludes: List<String>,
            runtimeConfiguration: Configuration
    ) {
        val clazz = Class.forName("com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar", true, JmhPlugin::class.java.classLoader) as Class<Task>
        project.tasks.create(JMH_JAR_TASK_NAME, ShadowJar::class.java) {
            it.group = JMH_GROUP
            it.dependsOn(JMH_TASK_COMPILE_GENERATED_CLASSES_NAME)
            it.description = "Create a combined JAR of project and runtime dependencies"
            it.conventionMapping.map("classifier") { JMH_NAME }
            it.manifest.inheritFrom(project.tasks.named("jar", Jar::class.java).get().manifest)
            it.manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"
            it.from(runtimeConfiguration)
            it.doFirst {
                val task = it as Jar
                fun processLibs(files: MutableSet<File>) {
                    if (files.isNotEmpty()) {
                        val libs = arrayListOf(task.manifest.attributes["Class-Path"] as String) // TODO check me
                        libs += files.map { it.name }
                        task.manifest.attributes["Class-Path"] = libs.distinct().joinToString(separator = " ")
                    }
                }
                processLibs(runtimeConfiguration.files)
                processLibs(project.configurations.getAt("shadow").files)

                if (extension.isIncludeTests)
                    task.from(project.sourceSets.test.output)
                task.eachFile { f: FileCopyDetails ->
                    if (f.name.endsWith(".class"))
                        f.duplicatesStrategy = extension.duplicateClassesStrategy
                }
            }
            it.from(project.sourceSets.jmh.output)
            it.from(project.sourceSets.main.output)
            it.from(project.file(jmhGeneratedClassesDir))
            it.from(project.file(jmhGeneratedResourcesDir))

            it.exclude(metaInfExcludes)
            it.configurations.clear()
        }
    }

    companion object {
        val IS_GRADLE_MIN_55 = GradleVersion.current().compareTo(GradleVersion.version("5.5.0")) >= 0

        val JMH_CORE_DEPENDENCY = "org.openjdk.jmh:jmh-core:"
        val JMH_GENERATOR_DEPENDENCY = "org.openjdk.jmh:jmh-generator-bytecode:"
        val JMH_GROUP = "jmh"
        val JMH_NAME = "jmh"
        val JMH_JAR_TASK_NAME = "jmhJar"
        val JMH_TASK_COMPILE_GENERATED_CLASSES_NAME = "jmhCompileGeneratedClasses"
        val JHM_RUNTIME_CONFIGURATION = "jmhRuntime"

        // TODO: This is really bad. We shouldn't use "runtime", but use the configurations provided by Gradle
        // automatically when creating a source set. That is to say, "jmhRuntimeOnly" for example and wire
        // our classpath properly
        private fun createJmhRuntimeConfiguration(project: Project, extension: JmhPluginExtension): Configuration =
                project.configurations.create(JHM_RUNTIME_CONFIGURATION).apply {
                    isCanBeConsumed = false
                    isCanBeResolved = true
                    isVisible = false
                    extendsFrom(project.configurations.getByName("jmh"))
                    extendsFrom(project.configurations.getByName("runtimeClasspath"))
                    project.afterEvaluate {
                        if (extension.includeTests.get())
                            extendsFrom(project.configurations.getByName("testRuntimeClasspath"))
                    }
                }

        private fun ensureTasksNotExecutedConcurrently(project: Project) {
            val rootExtra = project.rootProject.extensions.extraProperties
            val lastAddedRef = when {
                rootExtra.has("jmhLastAddedTask") -> rootExtra.get("jmhLastAddedTask") as AtomicReference<JmhTask>
                else -> AtomicReference<JmhTask>()
            }
            rootExtra.set("jmhLastAddedTask", lastAddedRef)

            project.tasks.withType(JmhTask::class.java) { task: JmhTask ->
                lastAddedRef.getAndSet(task)?.let { lastAdded -> task.mustRunAfter(lastAdded) }
            }
        }

        private fun <T : Task, A : T> createTask(project: Project, name: String, type: Class<T>, configuration: Action<A>) =
                project.tasks.register(name, type, configuration)
    }

    private fun configureIDESupport(project: Project) {
        project.afterEvaluate {
            val hasIdea = project.plugins.findPlugin(IdeaPlugin::class.java) != null
            if (hasIdea) {
                val idea = project.plugins.getAt(IdeaPlugin::class.java)
                idea.model.module {
                    val a = it.scopes["TEST"]!!["plus"]
//                        it.scopes["TEST"]!!["plus"] += project.configurations.getAt("jmh")
                }
                idea.model.module {
                    project.sourceSets.jmh.java.srcDirs.forEach { dir ->
                        it.testSourceDirs.add(project.file(dir))
                    }
                }
            }
            val hasEclipsePlugin = project.plugins.findPlugin(EclipsePlugin::class.java)
            val hasEclipseWtpPlugin = project.plugins.findPlugin(EclipseWtpPlugin::class.java)
            if (hasEclipsePlugin != null || hasEclipseWtpPlugin != null)
                project.extensions.getByType(EclipseModel::class.java).classpath
                        .plusConfigurations.plus(project.configurations.getAt("jmh"))
        }
    }

    private fun createStandardJmhJar(
            project: Project, extension: JmhPluginExtension, metaInfExcludes: List<String>,
            jmhGeneratedResourcesDir: File, jmhGeneratedClassesDir: File,
            runtimeConfiguration: Configuration
    ) {
        project.tasks.create(JMH_JAR_TASK_NAME, Jar::class.java) {
            it.group = JMH_GROUP
            it.dependsOn(JMH_TASK_COMPILE_GENERATED_CLASSES_NAME)
            it.inputs.files(project.sourceSets.jmh.output)
            it.inputs.files(project.sourceSets.main.output)
            if (extension.includeTests.get())
                it.inputs.files.plus(project.sourceSets.test.output)
            it.from(runtimeConfiguration.asFileTree.map { f ->
                if (f.isDirectory) f else project.zipTree(f)
            }.toTypedArray()).exclude(metaInfExcludes)
            it.doFirst {
                it as Jar
                it.from(project.sourceSets.jmh.output)
                it.from(project.sourceSets.main.output)
                it.from(project.file(jmhGeneratedClassesDir))
                it.from(project.file(jmhGeneratedResourcesDir))
                if (extension.includeTests.get()) {
                    it.from(project.sourceSets.test.output)
                }
                it.eachFile {
                    if (it.name.endsWith(".class"))
                        it.duplicatesStrategy = extension.duplicateClassesStrategy
                }
            }

            it.manifest.attributes["Main-Class"] = "org.openjdk.jmh.Main"

            it.archiveClassifier.set(JMH_NAME)
        }
    }
}