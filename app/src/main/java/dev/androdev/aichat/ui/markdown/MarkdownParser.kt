package dev.androdev.aichat.ui.markdown

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListItem(val marker: String, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private val headingPattern = Regex("^#{1,6}\\s+")
private val bulletPattern = Regex("^\\s*[-*+]\\s+")
private val orderedPattern = Regex("^\\s*\\d+[.)]\\s+")
private val quotePattern = Regex("^\\s*>\\s?")
private val tableSeparatorPattern = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")

fun parseMarkdown(source: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraph = StringBuilder()
    val quote = StringBuilder()
    val code = StringBuilder()
    var inCode = false
    var codeLanguage = ""

    fun flushParagraph() {
        if (paragraph.isNotBlank()) {
            blocks += MarkdownBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }
    }

    fun flushQuote() {
        if (quote.isNotBlank()) {
            blocks += MarkdownBlock.Quote(quote.toString().trimEnd())
            quote.clear()
        }
    }

    val lines = source.lines()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trimEnd()

        if (inCode) {
            if (line.trimStart().startsWith("```")) {
                blocks += MarkdownBlock.Code(codeLanguage, code.toString().trimEnd())
                code.clear()
                codeLanguage = ""
                inCode = false
            } else {
                code.appendLine(line)
            }
            index += 1
            continue
        }

        if (line.trimStart().startsWith("```")) {
            flushParagraph()
            flushQuote()
            inCode = true
            codeLanguage = line.trim().removePrefix("```").trim()
            index += 1
            continue
        }

        if (isTableRow(line) && index + 1 < lines.size && tableSeparatorPattern.matches(lines[index + 1].trimEnd())) {
            flushParagraph()
            flushQuote()
            val header = splitTableRow(line)
            val rows = mutableListOf<List<String>>()
            var rowIndex = index + 2
            while (rowIndex < lines.size && isTableRow(lines[rowIndex])) {
                rows += splitTableRow(lines[rowIndex])
                rowIndex += 1
            }
            blocks += MarkdownBlock.Table(header, rows)
            index = rowIndex
            continue
        }

        when {
            line.isBlank() -> {
                flushParagraph()
                flushQuote()
            }

            headingPattern.containsMatchIn(line) -> {
                flushParagraph()
                flushQuote()
                val level = line.takeWhile { it == '#' }.length
                blocks += MarkdownBlock.Heading(level, line.drop(level).trim())
            }

            bulletPattern.containsMatchIn(line) -> {
                flushParagraph()
                flushQuote()
                blocks += MarkdownBlock.ListItem("•", line.replaceFirst(bulletPattern, ""))
            }

            orderedPattern.containsMatchIn(line) -> {
                flushParagraph()
                flushQuote()
                val number = line.trimStart().takeWhile { it.isDigit() }
                blocks += MarkdownBlock.ListItem("$number.", line.replaceFirst(orderedPattern, ""))
            }

            quotePattern.containsMatchIn(line) -> {
                flushParagraph()
                quote.appendLine(line.replaceFirst(quotePattern, ""))
            }

            else -> {
                flushQuote()
                if (paragraph.isNotEmpty()) paragraph.appendLine()
                paragraph.append(line)
            }
        }
        index += 1
    }

    if (inCode) blocks += MarkdownBlock.Code(codeLanguage, code.toString().trimEnd())
    flushParagraph()
    flushQuote()
    return blocks
}

private fun isTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains('|') && !trimmed.startsWith("```")
}

private fun splitTableRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }
