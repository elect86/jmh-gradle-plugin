package me.champeau.gradle

import io.kotest.core.spec.style.StringSpec
import org.gradle.testfixtures.ProjectBuilder

class JMHPluginTest : StringSpec() {

    init {

        "plugin applied test" {
            val project = ProjectBuilder.builder().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'java'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//
//            def task = project . tasks . findByName ('jmh')
//            assert task instanceof JmhTask
//
//                    def jmhConfigurations = project . configurations *.name.findAll { it.startsWith('jmh') }
//            println(jmhConfigurations)
//            println project . configurations . jmhCompileClasspath . extendsFrom
//                    println project . configurations . jmhCompileClasspath . files
        }

//        @Test
//        void testPluginIsAppliedWithGroovy () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'groovy'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//
//            def task = project . tasks . findByName ('jmh')
//            assert task instanceof JmhTask
//
//        }
//
//        @Test
//        void testPluginIsAppliedWithoutZip64 () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'groovy'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//
//            def task = project . tasks . findByName ('jmhJar')
//            assert task . zip64 == false
//            assert task instanceof Jar
//
//        }
//
//        @Ignore
//        @Test
//        void testPluginIsAppliedWithZip64 () {
//            Project project = ProjectBuilder . builder ().build()
//            project.repositories {
//                mavenLocal()
//                jcenter()
//            }
//            project.apply plugin : 'groovy'
//            project.apply plugin : 'me.champeau.gradle.jmh'
//
//            project.jmh.zip64 = true
//
//
//            def task = project . tasks . findByName ('jmhJar')
//            assert task instanceof Jar
//                    assert task . zip64
//
//        }
//
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
