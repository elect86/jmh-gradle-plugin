package me.champeau.gradle

import io.kotest.core.spec.style.StringSpec
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class MultiLanguageSpec_ : StringSpec() {
    init {
        "Execute #language benchmarks" {
            listOf(/*"groovy", */"java"/*, "kotlin", "scala"*/).forEach { language ->
                val projectDir = File("src/funcTest/resources/$language-project")
                val pluginClasspathResource = this::class.java.classLoader.getResourceAsStream("plugin-classpath.txt")
                        ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
                val pluginClasspath = pluginClasspathResource.reader().readLines().map(::File)

                val project = GradleRunner.create()
                        .withProjectDir(projectDir)
                        .withPluginClasspath(pluginClasspath)
                        .withArguments("-S", "clean", "jmh")
                        .forwardOutput()
                        .build()

//        when:
//        BuildTask taskResult = project.task(":jmh")
//        String benchmarkResults = new File(projectDir, "build/reports/benchmarks.csv").text
//
//        then:
//        taskResult.outcome == TaskOutcome.SUCCESS
//        benchmarkResults.contains(language + 'Benchmark.sqrtBenchmark')
            }
        }
    }
}