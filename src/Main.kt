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

fun main() {
    registerTests()
    Tester.runTests()
}

private fun registerTests() {
    val testExceptMono = "*italic* or _italic_ **bold** or __bold__ ~~strikethrough~~ ||spoiler||"
    val expExceptMono = "italic or italic bold or bold strikethrough spoiler"
    Tester.registerTest(
        title = "exceptMono",
        testFun = { process(testExceptMono).first },
        expected = expExceptMono,
    )

    val testWithMono = "`mono` `*i* _i_ **b** __b__ ~~st~~ ||sp||`"
    val expWithMono = "mono *i* _i_ **b** __b__ ~~st~~ ||sp||"
    Tester.registerTest(
        title = "withMono",
        testFun = { process(testWithMono).first },
        expected = expWithMono,
    )

    val testWithEsc =  """\*e\* \_s\_ \`c\` \~~a\~~ \||p\|| \\e\\ \\ \\\* \\\_ \\\~ \\\|"""
    val expWithEsc = """*e* _s_ `c` ~~a~~ ||p|| \e\ \ \* \_ \~ \|"""
    Tester.registerTest(
        title = "withEsc",
        testFun = { process(testWithEsc) },
        expected = expWithEsc to null,
        hint = "no provided BodyRangeList and no styles encoded in the string, thus process should return a null value" +
                " for the BodyRangeList?",
    )

    val testIgnoreEscIfNotBeforeCtrlChar = """these \ slashes \ should \ remain \ but \*these\* two will vanish"""
    val expIgnoreEscIfNotBeforeCtrlChar = """these \ slashes \ should \ remain \ but *these* two will vanish"""
    Tester.registerTest(
        title = "ignoreEscIfNotBeforeCtrlChar",
        testFun = { process(testIgnoreEscIfNotBeforeCtrlChar).first },
        expected = expIgnoreEscIfNotBeforeCtrlChar,
    )

    // hello \* @mention
    val testEscWithExistingStyleRanges = BodyRangeList(listOf(BodyRange(start = 9, length = 8)))
    val expEscWithExistingStyleRanges = BodyRangeList(listOf(BodyRange(start = 8, length = 8)))
    Tester.registerTest(
        title = "escWithExistingStyleStr",
        testFun = { process(body = """hello \* @mention""", bodyRanges = testEscWithExistingStyleRanges) },
        expected = "hello * @mention" to expEscWithExistingStyleRanges,
    )

    val testWithMonoWithEscA = """`\*e\* \_s\_ \`c\\\` \~~a\~~ \||p\|| \\e\\ \\ \\\* \\\_ \\\~ \\\| \`"""
    val expWithMonoWithEscA =  """\*e\* \_s\_ \c\` ~~a~~ ||p|| \e\ \ \* \_ \~ \| `"""
    Tester.registerTest(
        title = "withMonoWithEscA",
        testFun = { process(testWithMonoWithEscA).first },
        expected = expWithMonoWithEscA,
    )

    Tester.registerTest(
        title = "withMonoWithEscB",
        testFun = { process("""~~word~~`~~word~~``~~word~~\`\`~~word~~\`\~~word~~\~~word\~~word~~word~~` `~~word~~` ~~word~~`\`~~word~~\` \\\\`~~word~~\\\\``""").first },
        expected = """word~~word~~~~word~~\`word`~~word~~word~~wordword word ~~word~~`word~~` \\~~word~~\\\\`""",
    )

    /**
     * 0....,....1....,....2....,....3....,....4....,....5....,....6
     * italic or italic bold or bold monospace strikethrough spoiler
     * <    >    <    > <  >    <  > <       > <           > <     >
     * 0         10     17      25   30        40            54         .start
     *      6         6    4       4         9            13       7    .length
     */
    val testMdBodyRanges = "*italic* or _italic_ **bold** or __bold__ `monospace` ~~strikethrough~~ ||spoiler||"
    val expMdBodyRanges = BodyRangeList(listOf(
        BodyRange(start = 0,  length = 6,  style = Style.ITALIC),
        BodyRange(start = 10, length = 6,  style = Style.ITALIC),
        BodyRange(start = 17, length = 4,  style = Style.BOLD),
        BodyRange(start = 25, length = 4,  style = Style.BOLD),
        BodyRange(start = 30, length = 9,  style = Style.MONOSPACE),
        BodyRange(start = 40, length = 13, style = Style.STRIKETHROUGH),
        BodyRange(start = 54, length = 7,  style = Style.SPOILER),
    ))
    Tester.registerTest(
        title = "markdownBodyRanges",
        testFun = { process(testMdBodyRanges).second },
        expected = expMdBodyRanges,
    )

    /**
     * 0....,....1....,....2....,....3....
     * **bold** followed by existing style (original string)
     *                      <-- italic --> (style)
     *                      21             (start)
     *                                  14 (length)
     *
     * 0....,....1....,....2....,....3
     * bold followed by existing style (processed string)
     * < b>             <-- italic --> (style)
     * 0                17             (start)
     *    4                         14 (length)
     */
    val testExistingStyleStr = "**bold** followed by existing style"
    val testExistingStyleRanges = BodyRangeList(listOf(BodyRange(start = 21, length = 14, style = Style.ITALIC)))
    val expExistingStyleStr = "bold followed by existing style"
    val expExistingStyleRanges = BodyRangeList(listOf(
        BodyRange(start = 17, length = 14, style = Style.ITALIC),
        BodyRange(start = 0, length = 4, style = Style.BOLD)
    ))
    Tester.registerTest(
        title = "existingStyle",
        testFun = { process(testExistingStyleStr, testExistingStyleRanges) },
        expected = expExistingStyleStr to expExistingStyleRanges,
    )

    val testExistingStyleNoMdStr = "existing style"
    val testExistingStyleNoMdRanges = BodyRangeList(listOf(BodyRange(start = 0, length = 14, style = Style.ITALIC)))
    Tester.registerTest(
        title = "existingStyleNoMd",
        testFun = { process(testExistingStyleNoMdStr, testExistingStyleNoMdRanges) },
        expected = testExistingStyleNoMdStr to testExistingStyleNoMdRanges,
        hint = "no changes should occur; input should be output"
    )

    val testMdStyleAdjustmentStr = "h**`l`**o *i*"
    val expMdStyleAdjustmentStr = "hlo i"
    val expMdStyleAdjustmentRanges = BodyRangeList(listOf(
        BodyRange(start = 1, length = 1, style = Style.BOLD),
        BodyRange(start = 1, length = 1, style = Style.MONOSPACE),
        BodyRange(start = 4, length = 1, style = Style.ITALIC),
    ))
    Tester.registerTest(
        title = "testMdStyleAdjustment",
        testFun = { process(body = testMdStyleAdjustmentStr) },
        expected = expMdStyleAdjustmentStr to expMdStyleAdjustmentRanges,
    )

    /**
     * 0....,....1....,....2....,....3....
     * bold and strikethrough no more bold
     * <        bold        >               start:0, length:22
     *      <        strikethrough       >  start:5, length:30
     */
    val testStaggeredTokensStr = "**bold ~~and strikethrough** no more bold~~"
    val expStaggeredTokensStr = "bold and strikethrough no more bold"
    val expStaggeredTokensRanges = BodyRangeList(listOf(
        BodyRange(start = 0, length = 22, style = Style.BOLD),
        BodyRange(start = 5, length = 30, style = Style.STRIKETHROUGH),
    ))
    Tester.registerTest(
        title = "staggeredTokens",
        testFun = { process(body = testStaggeredTokensStr) },
        expected = expStaggeredTokensStr to expStaggeredTokensRanges,
        hint = "range lengths should be reduced"
    )

    val testNestedFormatStr = "***~~||`nested`||~~***"
    val expNestedFormatStr = "nested"
    val expNestedFormatRanges = BodyRangeList(listOf(
        BodyRange(start = 0, length = 6, style = Style.BOLD),
        BodyRange(start = 0, length = 6, style = Style.ITALIC),
        BodyRange(start = 0, length = 6, style = Style.STRIKETHROUGH),
        BodyRange(start = 0, length = 6, style = Style.SPOILER),
        BodyRange(start = 0, length = 6, style = Style.MONOSPACE),
    ))
    Tester.registerTest(
        title = "nestedFormat",
        testFun = { process(body = testNestedFormatStr) },
        expected = expNestedFormatStr to expNestedFormatRanges,
    )

    val testStaggerNestedStr = "**~~nested||stagger~~** no more nesting||"
    val expStaggerNestedStr = "nestedstagger no more nesting"
    val expStaggerNestedRanges = BodyRangeList(listOf(
        BodyRange(start = 0, length = 13, style = Style.BOLD),
        BodyRange(start = 0, length = 13, style = Style.STRIKETHROUGH),
        BodyRange(start = 6, length = 23, style = Style.SPOILER),
    ))
    Tester.registerTest(
        title = "staggerNested",
        testFun = { process(body = testStaggerNestedStr) },
        expected = expStaggerNestedStr to expStaggerNestedRanges,
    )
}
