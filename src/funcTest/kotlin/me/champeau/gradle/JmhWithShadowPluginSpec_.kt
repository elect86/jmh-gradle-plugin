package me.champeau.gradle

import io.kotest.core.spec.style.StringSpec
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

class JmhWithShadowPluginSpec_ : StringSpec() {

    init {
        "Run #language benchmarks that are packaged with Shadow plugin" {
            listOf("java", "scala").forEach { language ->
                val projectDir = File("src/funcTest/resources/$language-shadow-project")
                val pluginClasspathResource = this::class.java.classLoader.getResourceAsStream("plugin-classpath.txt")
                        ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
                val pluginClasspath = pluginClasspathResource.reader().readLines().map { File(it) }

                val project = GradleRunner.create()
                        .withProjectDir(projectDir)
                        .withPluginClasspath(pluginClasspath)
                        .withArguments("-S", "clean", "jmh")
                        .build()

                val taskResult = project.task(":jmh")
                val benchmarkResults = File(projectDir, "build/reports/benchmarks.csv").readText()

                assert(taskResult?.outcome == TaskOutcome.SUCCESS)
                assert("${language}Benchmark.sqrtBenchmark" in benchmarkResults)
            }
        }
    }
}