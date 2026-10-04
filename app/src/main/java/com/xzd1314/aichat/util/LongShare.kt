package com.xzd1314.aichat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.xzd1314.aichat.data.Conversation
import java.io.File
import java.io.FileOutputStream

/**
 * "痕迹"分享：将会话渲染为带水印的长图
 */
object LongShare {

    private const val WIDTH = 1080
    private const val PADDING = 60
    private const val AVATAR_SIZE = 80
    private const val BUBBLE_RADIUS = 30
    private const val TEXT_SIZE = 36f
    private const val LINE_SPACING = 1.5f

    fun renderConversation(conv: Conversation, context: Context): File {
        // 第一遍：计算总高度
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TEXT_SIZE
            color = Color.BLACK
            typeface = Typeface.DEFAULT
        }
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val userBubbleColor = Color.parseColor("#95EC69")  // 微信绿
        val aiBubbleColor = Color.parseColor("#FFFFFF")
        val bgColor = Color.parseColor("#EDEDED")

        var totalHeight = PADDING * 2 + 120  // 顶部标题+底部水印
        val layouts = mutableListOf<StaticLayout>()
        val isUserList = mutableListOf<Boolean>()

        conv.messages.forEach { msg ->
            val isUser = msg.role == "user"
            val content = if (msg.content.isBlank()) "[图片]" else msg.content
            val maxWidth = WIDTH - PADDING * 2 - AVATAR_SIZE - 30
            val layout = StaticLayout.Builder
                .obtain(content, 0, content.length, textPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, LINE_SPACING)
                .build()
            val bubbleHeight = layout.height + 40
            totalHeight += bubbleHeight + 20
            layouts.add(layout)
            isUserList.add(isUser)
        }

        // 第二遍：绘制
        val bitmap = Bitmap.createBitmap(WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        // 顶部标题
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            color = Color.DKGRAY
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(conv.title, PADDING.toFloat(), 80f, titlePaint)
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            color = Color.GRAY
        }
        canvas.drawText("XZD AI Chat · 痕迹分享", PADDING.toFloat(), 120f, subPaint)

        var y = 160f
        layouts.forEachIndexed { index, layout ->
            val isUser = isUserList[index]
            val bubbleWidth = layout.width + 40
            val bubbleHeight = layout.height + 40

            if (isUser) {
                // 用户消息：右侧绿色气泡
                val left = (WIDTH - PADDING - bubbleWidth - AVATAR_SIZE - 20).toFloat()
                bubblePaint.color = userBubbleColor
                canvas.drawRoundRect(left, y, left + bubbleWidth, y + bubbleHeight,
                    BUBBLE_RADIUS.toFloat(), BUBBLE_RADIUS.toFloat(), bubblePaint)
                canvas.save()
                canvas.translate(left + 20, y + 20)
                layout.draw(canvas)
                canvas.restore()
                // 头像（右侧）
                canvas.drawCircle((WIDTH - PADDING - AVATAR_SIZE / 2).toFloat(), y + AVATAR_SIZE / 2,
                    AVATAR_SIZE / 2f, Paint().apply { color = Color.LTGRAY })
            } else {
                // AI 消息：左侧白色气泡
                val left = (PADDING + AVATAR_SIZE + 20).toFloat()
                bubblePaint.color = aiBubbleColor
                canvas.drawRoundRect(left, y, left + bubbleWidth, y + bubbleHeight,
                    BUBBLE_RADIUS.toFloat(), BUBBLE_RADIUS.toFloat(), bubblePaint)
                canvas.save()
                canvas.translate(left + 20, y + 20)
                layout.draw(canvas)
                canvas.restore()
                // 头像（左侧）
                canvas.drawCircle((PADDING + AVATAR_SIZE / 2).toFloat(), y + AVATAR_SIZE / 2,
                    AVATAR_SIZE / 2f, Paint().apply { color = Color.parseColor("#5B8DEF") })
            }
            y += bubbleHeight + 20
        }

        // 底部水印
        val watermarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            color = Color.GRAY
        }
        canvas.drawText("由 XZD AI Chat 生成 · xzd1314.top",
            PADDING.toFloat(), (totalHeight - 40).toFloat(), watermarkPaint)

        // 保存到文件
        val file = File(context.cacheDir, "trace_${conv.id}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
