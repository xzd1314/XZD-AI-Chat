package com.xzd1314.aichat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染器
 * 支持：代码块、行内代码、粗体、斜体、有序/无序列表、链接、标题
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: Int = 15
) {
    val context = LocalContext.current
    val blocks = parseMarkdown(text)

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> {
                    var copied by remember { mutableStateOf(false) }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    android.content.ClipData.newPlainText("code", block.code).let {
                                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(it)
                                    }
                                    copied = true
                                }
                                .padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp).width(14.dp)
                            )
                            Text(
                                if (copied) "已复制" else "复制代码",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (fontSize - 1).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = (fontSize * 1.4).sp
                        )
                    }
                }
                is MarkdownBlock.MdList -> {
                    Column(Modifier.padding(start = 4.dp)) {
                        block.items.forEachIndexed { idx, item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                Text(
                                    if (block.ordered) "${idx + 1}. " else "• ",
                                    color = textColor,
                                    fontSize = fontSize.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    renderInline(item),
                                    color = textColor,
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.45).sp
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.Heading -> {
                    Text(
                        renderInline(block.text),
                        color = textColor,
                        fontSize = (fontSize + block.level * 2).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (fontSize * 1.5).sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        renderInline(block.text),
                        color = textColor,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.45).sp
                    )
                }
            }
            Spacer(Modifier.padding(2.dp))
        }
    }
}

// ===== Markdown 解析 =====

private sealed class MarkdownBlock {
    data class Code(val code: String) : MarkdownBlock()
    data class MdList(val items: kotlin.collections.List<String>, val ordered: Boolean) : MarkdownBlock()
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 代码块
        if (line.trimStart().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            i++ // 跳过结束 ```
            blocks.add(MarkdownBlock.Code(codeLines.joinToString("\n")))
            continue
        }

        // 标题
        val headingMatch = Regex("^(#{1,4})\\s+(.*)").find(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            blocks.add(MarkdownBlock.Heading(headingMatch.groupValues[2], level))
            i++
            continue
        }

        // 无序列表
        if (line.trimStart().matches(Regex("^[-*+]\\s+.*"))) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^[-*+]\\s+.*"))) {
                items.add(lines[i].trimStart().removePrefix("-").removePrefix("*").removePrefix("+").trim())
                i++
            }
            blocks.add(MarkdownBlock.MdList(items, ordered = false))
            continue
        }

        // 有序列表
        if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
            val items = mutableListOf<String>()
            while (i < lines.size && lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                items.add(lines[i].trimStart().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
                i++
            }
            blocks.add(MarkdownBlock.MdList(items, ordered = true))
            continue
        }

        // 空行
        if (line.isBlank()) {
            i++
            continue
        }

        // 普通段落（合并连续非空行）
        val paraLines = mutableListOf<String>()
        while (i < lines.size && lines[i].isNotBlank() &&
            !lines[i].trimStart().startsWith("```") &&
            !lines[i].trimStart().matches(Regex("^#{1,4}\\s+.*")) &&
            !lines[i].trimStart().matches(Regex("^[-*+]\\s+.*")) &&
            !lines[i].trimStart().matches(Regex("^\\d+\\.\\s+.*"))
        ) {
            paraLines.add(lines[i])
            i++
        }
        blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
    }

    return blocks
}

// 行内格式：粗体、斜体、行内代码、链接
private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        // 行内代码
        val codeMatch = Regex("`([^`]+)`").find(remaining)
        // 粗体
        val boldMatch = Regex("\\*\\*([^*]+)\\*\\*").find(remaining)
        // 斜体
        val italicMatch = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").find(remaining)

        val matches = listOfNotNull(codeMatch, boldMatch, italicMatch).sortedBy { it.range.first }
        if (matches.isEmpty()) {
            append(remaining)
            break
        }

        val first = matches.first()
        if (first.range.first > 0) {
            append(remaining.substring(0, first.range.first))
        }

        when (first) {
            codeMatch -> {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x33888888))) {
                    append(first.groupValues[1])
                }
            }
            boldMatch -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(first.groupValues[1])
                }
            }
            italicMatch -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(first.groupValues[1])
                }
            }
        }
        remaining = remaining.substring(first.range.last + 1)
    }
}
