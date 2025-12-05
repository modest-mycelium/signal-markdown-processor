// Copyright (C) 2025 Modest Mycelium
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

object Tester {
    private val testRegistry = mutableListOf<Test<*>>()
    private var testsLog = ""
    private var debugLog = ""

    fun d(str: String) { debugLog += "$str\n" }

    fun <T : Any?> registerTest(title: String, testFun: () -> T, expected: T, hint: String? = null) {
        testRegistry.add(Test(title, testFun, expected, hint))
    }

    fun runTests() {
        var failed = 0

        testRegistry.forEach { test ->
            debugLog = ""
            l("running test (${test.title}): ")

            if (test.passed) {
                ln("success")
            } else {
                failed++
                ln("failure")
                ln("\texpected: ${test.expected}")
                ln("\tcomputed: ${test.computed}")
                if (test.hint != null) { ln("\thint: ${test.hint}") }

                if (debugLog.isNotEmpty()) {
                    println("\n=== start log (${test.title}) ===")
                    print(debugLog)
                    println("==== end log (${test.title}) ====")
                } else {
                    println("\n=== no log msgs for (${test.title}) ===")
                }
            }
        }

        println("\n=== test results ===")
        println(testsLog)
        if (failed == 0) println("passed all tests!") else println("failed $failed/${testRegistry.size} tests!")
    }

    private fun ln(str: String) { l(str + "\n") }
    private fun l(str: String) { testsLog += str }

    private data class Test <T> (
        val title: String,
        val testFun: () -> T,
        val expected: T,
        val hint: String? = null,
    ) {
        val computed by lazy { testFun() }
        val passed by lazy { computed == expected }
    }
}
