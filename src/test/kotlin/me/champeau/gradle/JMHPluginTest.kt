package me.champeau.gradle

import io.kotest.core.spec.style.StringSpec
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
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
            assert(task.isZip64)
        }

//        @Test
//        void testAllJmhTasksBelongToJmhGroup () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'java'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//            project.tasks.find { it.name.startsWith('jmh') }.each {
//                assert it . group == JMHPlugin . JMH_GROUP
//            }
//        }
//
//        @Test
//        void testPluginIsAppliedTogetherWithShadow () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'java'
//            project.apply plugin : 'com.github.johnrengelman.shadow'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//            def task = project . tasks . findByName ('jmhJar')
//            assert task instanceof ShadowJar
//        }
//
//        @Test
//        void testDuplicateClassesStrategyIsSetToFailByDefault () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'java'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//            assert project . jmh . duplicateClassesStrategy == DuplicatesStrategy . FAIL
//        }
    }
}
