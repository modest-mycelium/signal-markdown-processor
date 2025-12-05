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

fun process(body: String, bodyRanges: BodyRangeList? = null): Pair<String, BodyRangeList?> {
    val sumTokens = IntArray(body.length) { 0 }
    val (bodySansMdTokens, mdRanges) = processMarkdownTokens(body, sumTokens)

    return bodySansMdTokens to adjustRanges(bodyRanges?.ranges.orEmpty() + mdRanges, sumTokens)
}

private fun adjustRanges(ranges: List<BodyRange>, sumTokens: IntArray): BodyRangeList? {
    val result = mutableListOf<BodyRange>()

    for (r in ranges) {
        var start = r.start
        while (start > 0 && sumTokens[start] > sumTokens[start - 1] && sumTokens[start - 1] > 0) { // the current start position is, itself, a token
            start -= sumTokens[start - 1]
        }

        result.add(BodyRange( // todo: use a copy method if available in Signal
            start = if (start != r.start) start else r.start - sumTokens[r.start],
            length = r.length - (sumTokens[r.start + r.length - 1] - sumTokens[r.start]) - if (start != r.start) 1 else 0,
            style = r.style,
            mentionAci = r.mentionAci,
            unknownFields = r.unknownFields,
        ))
    }

    return if (result.isEmpty()) null else BodyRangeList(ranges = result)
}

private fun processMarkdownTokens(str: String, sumTokens: IntArray): Pair<String, List<BodyRange>> {
    val resultSb = StringBuilder()
    val resultRanges = mutableListOf<BodyRange>()
    
    var escaped = false
    var remainingBackticks = str.count { it == '`' } // used to determine if a closing backtick exists

    var isToken = false
    val openTokens = mutableMapOf(
        "**" to false, "*" to false, "__" to false, "_" to false, "||" to false, "~~" to false, "`" to false
    )
    val openTokenInd = mutableMapOf<String, Int>()

    for ((i, c) in str.withIndex()) {
        if (isToken) { // previous iteration determined this character is a token (used for double tokens like ~~)
            // sumTokens is not set here; this is handled by the close double token logic
            isToken = false
        } else if (escaped) { // previous character escaped this one
            if (c == '`') remainingBackticks--
            escaped = false
        } else if (c == '`') { // non-escaped backtick
            if (openTokens["`"]!!) { // this is a closing backtick
                sumTokens[i] = 1

                resultRanges.add(BodyRange(
                    start = openTokenInd["`"]!! + 1,
                    length = i - openTokenInd["`"]!!,
                    style = Style.MONOSPACE,
                ))

                openTokens["`"] = false
                openTokenInd.remove("`")
                remainingBackticks--
            } else if (remainingBackticks > 1) { // this is not the last backtick, so it opens a monospace style
                sumTokens[i] = 1

                openTokens["`"] = true
                openTokenInd["`"] = i
                remainingBackticks--
            } else { // this is the final backtick, with no open backtick to close, so it is not a token
                remainingBackticks--
            }
        } else if (openTokens["`"]!!) { // all characters after an openBacktick should be treated as raw characters
            // we don't need to do anything here except make sure no conditionals further down the chain are run
        } else if (c == '\\' && str.length > i + 1 && str[i + 1] in """\`*_~|""") { // is escaping next character
            sumTokens[i] = 1
            escaped = true
        } else if (c in "*_~|") { // is a style token character
            val dt = "$c$c"
            if (openTokens[dt]!! && str.length > i + 1 && c == str[i + 1]) { // dt is a closing token
                sumTokens[openTokenInd[dt]!!] = 1
                sumTokens[openTokenInd[dt]!! + 1] = 1
                sumTokens[i] = 1
                sumTokens[i + 1] = 1

                resultRanges.add(BodyRange(
                    start = openTokenInd[dt]!! + 2,
                    length = (i - 1) - (openTokenInd[dt]!! + 1),
                    style = when (dt) {
                        "**", "__" -> Style.BOLD
                        "~~" -> Style.STRIKETHROUGH
                        "||" -> Style.SPOILER
                        else -> null // unreachable
                    }
                ))

                openTokens[dt] = false
                openTokenInd.remove(dt)
                isToken = true // this is to skip the next character that we already know is a token
            } else if (!openTokens[dt]!! && str.length > i + 1 && c == str[i + 1]) { // dt is an opening token
                // we do NOT set sumTokens as this token may never be closed, and thus should be ignored
                openTokens[dt] = true
                openTokenInd[dt] = i
                isToken = true // this is to skip the next character that we already know is a token
            } else if (c in "*_") { // is an italic token (checked after double tokens to ensure bold is processed)
                if (openTokens["$c"]!!) { // this is a closing italic token
                    sumTokens[i] = 1

                    resultRanges.add(BodyRange(
                        start = openTokenInd["$c"]!! + 1,
                        length = i - openTokenInd["$c"]!!,
                        style = Style.ITALIC,
                    ))

                    openTokens["$c"] = false
                    openTokenInd.remove("$c")
                } else { // this is an opening italic token
                    sumTokens[i] = 1

                    openTokens["$c"] = true
                    openTokenInd["$c"] = i
                }
            }
        }
    }

    var runningTokenSum = 0
    for ((ind, num) in sumTokens.withIndex()) {
        if (num == 0) {
            resultSb.append(str[ind])
            sumTokens[ind] = runningTokenSum
        } else { sumTokens[ind] += runningTokenSum++ }
    }

    return resultSb.toString() to resultRanges
}
