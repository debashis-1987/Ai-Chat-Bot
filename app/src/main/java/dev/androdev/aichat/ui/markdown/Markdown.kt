package dev.androdev.aichat.ui.markdown

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

fun parseInline(source: String, codeBackground: Color, linkColor: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val end = source.indexOf("**", index + 2)
                if (end == -1) {
                    append("**")
                    index += 2
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(source.substring(index + 2, end))
                    }
                    index = end + 2
                }
            }

            source.startsWith("[", index) -> {
                val labelEnd = source.indexOf(']', index + 1)
                val urlStart = if (labelEnd != -1 && labelEnd + 1 < source.length && source[labelEnd + 1] == '(') {
                    labelEnd + 2
                } else {
                    -1
                }
                val urlEnd = if (urlStart != -1) source.indexOf(')', urlStart) else -1
                if (labelEnd == -1 || urlStart == -1 || urlEnd == -1) {
                    append(source[index])
                    index += 1
                } else {
                    val label = source.substring(index + 1, labelEnd)
                    val url = source.substring(urlStart, urlEnd).trim()
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            ),
                        ),
                    ) {
                        append(label)
                    }
                    index = urlEnd + 1
                }
            }

            source[index] == '`' -> {
                val end = source.indexOf('`', index + 1)
                if (end == -1) {
                    append('`')
                    index += 1
                } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        ),
                    ) {
                        append(source.substring(index + 1, end))
                    }
                    index = end + 1
                }
            }

            source[index] == '*' -> {
                val end = source.indexOf('*', index + 1)
                if (end == -1) {
                    append('*')
                    index += 1
                } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(source.substring(index + 1, end))
                    }
                    index = end + 1
                }
            }

            else -> {
                append(source[index])
                index += 1
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = parseInline(block.text, inlineCodeBackground, linkColor),
                    style = headingStyle(block.level),
                    fontWeight = FontWeight.SemiBold,
                )

                is MarkdownBlock.Paragraph -> SelectionContainer {
                    Text(
                        text = parseInline(block.text, inlineCodeBackground, linkColor),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                is MarkdownBlock.ListItem -> Row {
                    Text(
                        text = block.marker,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(20.dp),
                    )
                    SelectionContainer {
                        Text(
                            text = parseInline(block.text, inlineCodeBackground, linkColor),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                is MarkdownBlock.Quote -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Spacer(
                        Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text = parseInline(block.text, inlineCodeBackground, linkColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is MarkdownBlock.Table -> TableBlock(block, inlineCodeBackground, linkColor)

                is MarkdownBlock.Code -> CodeBlock(block.language, block.code)
            }
        }
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineSmall
    2 -> MaterialTheme.typography.titleLarge
    3 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}

@Composable
private fun TableBlock(block: MarkdownBlock.Table, inlineCodeBackground: Color, linkColor: Color) {
    val columns = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { it.size } ?: 0,
    )
    if (columns == 0) return

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
        ) {
            Row {
                repeat(columns) { column ->
                    Text(
                        text = parseInline(block.header.getOrElse(column) { "" }, inlineCodeBackground, linkColor),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            block.rows.forEach { row ->
                Row {
                    repeat(columns) { column ->
                        Text(
                            text = parseInline(row.getOrElse(column) { "" }, inlineCodeBackground, linkColor),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .widthIn(min = 88.dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(language: String, code: String) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Code", code))
                    },
                ) {
                    Text("Copy")
                }
            }
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}
