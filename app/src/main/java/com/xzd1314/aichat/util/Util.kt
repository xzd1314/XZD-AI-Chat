package com.xzd1314.aichat.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Util {

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun readClipboard(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
    }

    fun openBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 无浏览器时静默失败
        }
    }

    fun formatTime(ts: Long): String =
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

    fun formatClock(ts: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

    /** 从 content:// 或文件路径读取图片 Bitmap（缩放至合理尺寸） */
    suspend fun loadBitmap(context: Context, uriStr: String, maxSide: Int = 512): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriStr)
                val resolver = context.contentResolver
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                var sample = 1
                while (opts.outWidth / sample > maxSide * 2 || opts.outHeight / sample > maxSide * 2) sample *= 2
                val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
            } catch (e: Exception) {
                null
            }
        }

    fun mediaStoreId(uri: Uri): Long? =
        uri.lastPathSegment?.toLongOrNull()

    fun pickImageIntent(): Intent =
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    /** 会话头像默认图标（内置 AI 头像用组合图，此处返回 null 表示默认） */
    const val DEFAULT_AVATAR = "default"

    // ---------- 风格分享文本编码 ----------
    // 格式: XZD-STYLE:v1.<base64url(JSON)>

    const val SHARE_PREFIX = "XZD-STYLE:v1."

    fun encodeStyle(json: JSONObject): String {
        val b64 = Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return SHARE_PREFIX + b64
    }

    fun decodeStyle(text: String): JSONObject? {
        val t = text.trim()
        if (!t.startsWith(SHARE_PREFIX)) return null
        val b64 = t.removePrefix(SHARE_PREFIX)
        return try {
            val raw = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            JSONObject(String(raw, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    fun argbColor(color: Long): String {
        val c = color.toInt()
        return String.format("#%06X", c and 0xFFFFFF)
    }

    fun parseColor(hex: String): Long? = try {
        java.lang.Long.parseLong(hex.removePrefix("#"), 16) or 0xFF000000
    } catch (e: Exception) { null }
}
