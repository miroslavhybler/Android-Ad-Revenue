package mir.oslav.adrevenue.ui.search

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle

internal fun markdownToAnnotatedString(markdown: String): AnnotatedString = buildAnnotatedString {
    val lines = markdown.trim().lines()
    var inCodeBlock = false

    lines.forEachIndexed { index, line ->
        val trimmedLine = line.trimEnd()
        if (trimmedLine.trimStart().startsWith("```")) {
            inCodeBlock = !inCodeBlock
        } else if (inCodeBlock) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(trimmedLine)
            }
        } else {
            val heading = HEADING_PATTERN.matchEntire(trimmedLine)
            val bullet = BULLET_PATTERN.matchEntire(trimmedLine)
            when {
                heading != null -> withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = when (heading.groupValues[1].length) {
                            1 -> 22.sp
                            2 -> 19.sp
                            else -> 17.sp
                        },
                    ),
                ) {
                    appendInlineMarkdown(heading.groupValues[2])
                }

                bullet != null -> {
                    append("• ")
                    appendInlineMarkdown(bullet.groupValues[1])
                }

                else -> appendInlineMarkdown(trimmedLine)
            }
        }
        if (index < lines.lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInlineMarkdown(value: String) {
    var cursor = 0
    INLINE_MARKDOWN_PATTERN.findAll(value).forEach { match ->
        append(value.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removeSurrounding("**"))
            }

            token.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(token.removeSurrounding("*"))
            }

            token.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                append(token.removeSurrounding("`"))
            }

            else -> {
                val link = LINK_PATTERN.matchEntire(token)
                if (link == null) {
                    append(token)
                } else {
                    pushStringAnnotation(LINK_ANNOTATION_TAG, link.groupValues[2])
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(link.groupValues[1])
                    }
                    pop()
                }
            }
        }
        cursor = match.range.last + 1
    }
    append(value.substring(cursor))
}

internal const val LINK_ANNOTATION_TAG = "markdown-link"

private val HEADING_PATTERN = Regex("^(#{1,6})\\s+(.+)$")
private val BULLET_PATTERN = Regex("^[-*+]\\s+(.+)$")
private val LINK_PATTERN = Regex("^\\[([^]]+)]\\(([^)]+)\\)$")
private val INLINE_MARKDOWN_PATTERN = Regex("\\*\\*.+?\\*\\*|`.+?`|\\*[^*]+\\*|\\[[^]]+\\]\\([^)]+\\)")
