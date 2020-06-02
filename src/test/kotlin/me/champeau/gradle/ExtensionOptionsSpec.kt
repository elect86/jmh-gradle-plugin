/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.champeau.gradle

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.options.Options
import org.openjdk.jmh.runner.options.TimeValue
import org.openjdk.jmh.runner.options.VerboseMode
import org.openjdk.jmh.util.Optional
import java.io.File

class ExtensionOptionsSpec : StringSpec() {

    val project = ProjectBuilder.builder().build()
    val extension = JmhPluginExtension(project)
    val args: Options
        get() = extension.resolveArgs()

    init {
        "Verify option #optionMethod with #value as #result (Optional)" {

            infix fun <T> Optional<T>.shouldBe(expected: T) = orElse(null) shouldBe expected

            fun output(file: File? = null, res: String? = null) {
                extension.humanOutputFile = file
                args.output shouldBe res
            }
            output()
            output(project.file("foo.txt"), project.file("foo.txt").absolutePath)

            fun resultFormat(string: String? = null) {
                extension.resultFormat = string
                args.resultFormat shouldBe ResultFormatType.TEXT
            }
            resultFormat()
            resultFormat("text")

            fun forceGC(force: Boolean? = null) {
                extension.forceGC = force
                args.shouldDoGC() shouldBe force
            }
            forceGC()
            forceGC(true)
            forceGC(false)

            fun verbosity(verbosity: String? = null, res: VerboseMode? = null) {
                extension.verbosity = verbosity
                args.verbosity() shouldBe res
            }
            verbosity()
            verbosity("extra", VerboseMode.EXTRA)

            fun failOnError(fail: Boolean? = null) {
                extension.failOnError = fail
                args.shouldFailOnError() shouldBe fail
            }
            failOnError()
            failOnError(true)
            failOnError(false)

            fun threads(threads: Int? = null) {
                extension.threads = threads
                args.threads shouldBe threads
            }
            threads()
            threads(100)

            fun threadGroup(threadGroup: List<Int>? = null) {
                extension.threadGroups = threadGroup
                args.threadGroups shouldBe threadGroup?.toIntArray()
            }
            threadGroup()
            threadGroup(listOf(1, 2, 3))

            fun syncIterations(sync: Boolean? = null) {
                extension.synchronizeIterations = sync
                args.shouldSyncIterations() shouldBe sync
            }
            syncIterations()
            syncIterations(true)
            syncIterations(false)

            fun warmupIterations(it: Int? = null) {
                extension.warmupIterations = it
                args.warmupIterations shouldBe it
            }
            warmupIterations()
            warmupIterations(100)

            fun warmup(time: String? = null, res: TimeValue? = null) {
                extension.warmup = time
                args.warmupTime shouldBe res
            }
            warmup()
            warmup("1ns", TimeValue.nanoseconds(1))

            fun iterations(it: Int? = null) {
                extension.iterations = it
                args.measurementIterations shouldBe it
            }
            iterations()
            iterations(100)

            fun batchSize(batch: Int? = null) {
                extension.batchSize = batch
                args.measurementBatchSize shouldBe batch
            }
            batchSize()
            batchSize(1)

            fun fork(fork: Int? = null) {
                extension.fork = fork
                args.forkCount shouldBe fork
            }
            fork()
            fork(2)
            
            fun jvm(jvm: String? = null) {
                extension.jvm = jvm
                args.jvm shouldBe jvm
            }
            jvm()
            jvm("myJvm")

            fun jvmArgs(arg: List<String>? = null) {
                extension.jvmArgs = arg
                args.jvmArgs shouldBe arg
            }
            jvmArgs()
            jvmArgs(listOf("Custom JVM args"))

            fun jvmArgsAppend(arg: List<String>? = null) {
                extension.jvmArgsAppend = arg
                args.jvmArgsAppend shouldBe arg
            }

            jvmArgsAppend()
            jvmArgsAppend(listOf("Custom JVM args"))

            fun jvmArgsPrepend(arg: List<String>? = null) {
                extension.jvmArgsPrepend = arg
                args.jvmArgsPrepend shouldBe arg
            }

            jvmArgsPrepend()
            jvmArgsPrepend(listOf("Custom JVM args"))

            fun result(file: File? = null, res: String) {
                extension.resultsFile = file
                args.result shouldBe res
            }

            result(res = project.file("build/reports/jmh/results.txt").absolutePath)
            result(project.file("res.txt"), project.file("res.txt").absolutePath)

            fun invocation(operations: Int? = null) {
                extension.operationsPerInvocation = operations
                args.operationsPerInvocation shouldBe operations
            }

            invocation()
            invocation(10)
//            'getMeasurementTime'         | 'setTimeOnIteration'         | null                    || null
//            'getMeasurementTime'         | 'setTimeOnIteration'         | '1s'                    || TimeValue.seconds(1)
//            'getTimeout'                 | 'setTimeout'                 | null                    || null
//            'getTimeout'                 | 'setTimeout'                 | '60s'                   || TimeValue.seconds(60)
//            'getTimeUnit'                | 'setTimeUnit'                | null                    || null
//            'getTimeUnit'                | 'setTimeUnit'                | 'ms'                    || TimeUnit.MILLISECONDS
//            'getWarmupBatchSize'         | 'setWarmupBatchSize'         | null                    || null
//            'getWarmupBatchSize'         | 'setWarmupBatchSize'         | 10                      || 10
//            'getWarmupForkCount'         | 'setWarmupForks'             | null                    || null
//            'getWarmupForkCount'         | 'setWarmupForks'             | 0                       || 0
//            'getWarmupMode'              | 'setWarmupMode'              | null                    || null
//            'getWarmupMode'              | 'setWarmupMode'              | 'INDI'                  || WarmupMode.INDI
        }
    }
}