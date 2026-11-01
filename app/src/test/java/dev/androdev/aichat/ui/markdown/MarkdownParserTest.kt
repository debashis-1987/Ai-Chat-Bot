package dev.androdev.aichat.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parsesHeadingsWithLevels() {
        val blocks = parseMarkdown("# One\n\n### Three")

        assertEquals(
            listOf(
                MarkdownBlock.Heading(1, "One"),
                MarkdownBlock.Heading(3, "Three"),
            ),
            blocks,
        )
    }

    @Test
    fun parsesBulletAndOrderedLists() {
        val blocks = parseMarkdown("- first\n- second\n\n1. one\n2. two")

        assertEquals(
            listOf(
                MarkdownBlock.ListItem("•", "first"),
                MarkdownBlock.ListItem("•", "second"),
                MarkdownBlock.ListItem("1.", "one"),
                MarkdownBlock.ListItem("2.", "two"),
            ),
            blocks,
        )
    }

    @Test
    fun parsesFencedCodeWithLanguage() {
        val blocks = parseMarkdown("```kotlin\nval x = 1\n```")

        assertEquals(listOf(MarkdownBlock.Code("kotlin", "val x = 1")), blocks)
    }

    @Test
    fun closesUnterminatedCodeFenceAtEnd() {
        val blocks = parseMarkdown("```\nunclosed")

        assertEquals(listOf(MarkdownBlock.Code("", "unclosed")), blocks)
    }

    @Test
    fun parsesTableWithHeaderAndRows() {
        val blocks = parseMarkdown("| Name | Value |\n| --- | --- |\n| a | 1 |\n| b | 2 |")

        assertEquals(
            listOf(
                MarkdownBlock.Table(
                    header = listOf("Name", "Value"),
                    rows = listOf(listOf("a", "1"), listOf("b", "2")),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun doesNotTreatPlainPipeTextAsTable() {
        val blocks = parseMarkdown("a | b")

        assertEquals(listOf(MarkdownBlock.Paragraph("a | b")), blocks)
    }

    @Test
    fun mergesConsecutiveQuoteLines() {
        val blocks = parseMarkdown("> first\n> second")

        assertEquals(listOf(MarkdownBlock.Quote("first\nsecond")), blocks)
    }

    @Test
    fun groupsParagraphLinesAndSplitsOnBlankLine() {
        val blocks = parseMarkdown("line one\nline two\n\nnext")

        assertEquals(
            listOf(
                MarkdownBlock.Paragraph("line one\nline two"),
                MarkdownBlock.Paragraph("next"),
            ),
            blocks,
        )
    }
}
