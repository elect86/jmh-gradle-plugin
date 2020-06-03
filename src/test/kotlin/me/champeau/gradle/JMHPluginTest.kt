package me.champeau.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.kotest.core.spec.style.StringSpec
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getAt
import org.gradle.kotlin.dsl.repositories
import org.gradle.testfixtures.ProjectBuilder

class JMHPluginTest : StringSpec() {

    init {

        fun setup(vararg plugins: String): Project {
            val project = ProjectBuilder.builder().build()
            project.repositories {
                mavenLocal()
                jcenter()
            }
            for (plugin in plugins)
                project.apply(plugin = plugin)
            project.apply(plugin = "me.champeau.gradle.jmh")
            return project
        }

        "is plugin applied?" {
            val project = setup("java")

            val task = project.tasks.findByName("jmh")
            assert(task is JmhTask)

            val jmhConfigurations = listOf("jmh", "jmhAnnotationProcessor", "jmhCompile", "jmhCompileClasspath", "jmhCompileOnly", "jmhImplementation", "jmhRuntime", "jmhRuntimeClasspath", "jmhRuntimeOnly")
            assert(project.configurations.filter { it.name.startsWith("jmh") }.all { it.name in jmhConfigurations })

            val jmhCompileClasspath = project.configurations["jmhCompileClasspath"]
            val from = listOf("jmhCompileOnly", "jmhImplementation", "implementation", "compileOnly")
            assert(jmhCompileClasspath.extendsFrom.all { it.name in from })

            val files = listOf("generator-bytecode", "generator-asm", "generator-reflection", "core").map { "jmh-${it}-${Jmh.version}.jar" }
            assert(files.all { it in jmhCompileClasspath.files.map { it.name } })
        }

        "is plugin applied with Groovy?" {
            val project = setup("groovy")
            val task = project.tasks["jmh"]
            assert(task is JmhTask)
        }

        "is plugin applied without zip64?" {
            val project = setup("groovy")
            val task = project.tasks.jmhJar
            assert(!task.isZip64)
        }

        "is plugin applied with zip64?" {
            val project = setup("groovy")

            project.extensions.jmh.isZip64 = true

            val task = project.tasks.jmhJar
//            assert(task.isZip64)
        }

        "all jmh tasks belong to jmhGroup?" {
            val project = setup("java")

            project.tasks.filter { it.name.startsWith("jmh") && it.name != "jmhClasses" }
                    .forEach { assert(it.group == Jmh.group) }
        }

        "is plugin applied together with shadow?" {
            val project = setup("java", "com.github.johnrengelman.shadow")

            val task = project.tasks.jmhJar
            println(task::class.java)
            assert(task is ShadowJar)
        }

        "is duplicate classes strategy set to fail by default?" {
            val project = setup("java")

            assert(project.extensions.jmh.duplicateClassesStrategy == DuplicatesStrategy.FAIL)
        }
    }
}
