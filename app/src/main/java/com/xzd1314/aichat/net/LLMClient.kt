package com.xzd1314.aichat.net

import com.xzd1314.aichat.data.ChatMessage
import com.xzd1314.aichat.data.ModelPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenAI 兼容大模型客户端。
 *
 * 缓存命中率优化（重点）：
 * 1. 消息序列化完全确定性：role+content 按时间升序拼接，绝不注入当前时间/随机数等易变字段；
 * 2. system 提示词固定置于最前且永不重排，保证前缀稳定，最大化 DeepSeek/GLM 等
 *    自动前缀缓存的命中；
 * 3. 截断只发生在最旧的“尾部”，新消息永远追加在后，前缀不变；
 * 4. 完整历史按序发送（而非每次只发最后一条），让长对话复用已缓存前缀。
 */
object LLMClient {

    private const val CONNECT_TIMEOUT = 20_000
    private const val READ_TIMEOUT = 90_000

    /** 估算 token 数：中文/全角 1 字符≈1.2 token，英文 1 词≈1.3 token */
    fun estimateTokens(text: String): Int {
        if (text.isBlank()) return 0
        var cjk = 0; var ascii = 0; var other = 0
        text.forEach { c ->
            when {
                c.code in 0x4E00..0x9FFF || c.code in 0x3000..0x30FF -> cjk++
                c.code < 0x80 -> ascii++
                else -> other++
            }
        }
        return ((cjk + other) * 1.2 + ascii / 4.0).toInt().coerceAtLeast(1)
    }

    /** 估算整段消息列表的 token 数 */
    fun estimateMessagesTokens(messages: List<ChatMessage>): Int {
        var t = 0
        messages.forEach { m -> t += estimateTokens(m.content) + 4 }
        return t
    }

    /**
     * 构造请求消息列表：system 在前，历史按时间升序，只截断最旧消息以保留前缀。
     * 返回 (messages, 实际发送条数, 估算token)
     */
    fun buildRequestMessages(
        systemPrompt: String,
        history: List<ChatMessage>,
        maxContextCount: Int
    ): Triple<List<ChatMessage>, Int, Int> {
        val trimmed = if (history.size > maxContextCount)
            history.takeLast(maxContextCount) else history
        val all = mutableListOf<ChatMessage>()
        all.add(ChatMessage("system", systemPrompt, 0L))
        all.addAll(trimmed)
        val tokens = all.sumOf { estimateTokens(it.content) + 4 }
        return Triple(all, trimmed.size, tokens)
    }

    /** 发送非流式请求，返回 AI 回复文本；失败返回可读错误信息 */
    suspend fun chat(
        preset: ModelPreset,
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,   // 已含 system
        temperature: Float,
        maxTokens: Int,
        reasoningEffort: String? = null  // "low" | "medium" | "high"，null=关闭
    ): Result<Pair<String, String?>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val url = URL(baseUrl.ifBlank { preset.baseUrl })
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = CONNECT_TIMEOUT
                conn.readTimeout = READ_TIMEOUT
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                if (apiKey.isNotBlank()) {
                    conn.setRequestProperty(preset.authHeader, preset.authScheme + apiKey)
                }

                val body = JSONObject().apply {
                    put("model", model)
                    val arr = JSONArray()
                    messages.forEach { m ->
                        arr.put(JSONObject().put("role", m.role).put("content", m.content))
                    }
                    put("messages", arr)
                    put("temperature", temperature.toDouble())
                    put("max_tokens", maxTokens)
                    put("stream", false)
                    if (!reasoningEffort.isNullOrBlank()) {
                        put("reasoning_effort", reasoningEffort)
                    }
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.let { s ->
                    BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() }
                } ?: ""

                if (code in 200..299) {
                    val obj = JSONObject(text)
                    val msgObj = obj.optJSONArray("choices")?.optJSONObject(0)
                        ?.optJSONObject("message")
                    val content = msgObj?.optString("content", "").orEmpty()
                    val reasoning = msgObj?.optString("reasoning_content", "")
                        ?.takeIf { it.isNotBlank() }
                    if (content.isBlank()) {
                        Result.failure(IllegalStateException("模型返回了空内容，请检查模型名是否正确（如 ${preset.defaultModel}）。"))
                    } else {
                        Result.success(content to reasoning)
                    }
                } else {
                    val msg = friendlyError(code, text)
                    Result.failure(IllegalStateException(msg))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun friendlyError(code: Int, body: String): String {
        val hint = when (code) {
            401 -> "API Key 无效或未填写（状态 401）。"
            403 -> "无权限访问该模型（状态 403）。"
            404 -> "接口或模型不存在，请检查 Base URL / 模型名（状态 404）。"
            429 -> "请求过于频繁或额度不足（状态 429），请稍后再试。"
            400 -> "请求参数有误，请检查模型名（状态 400）。"
            500, 502, 503 -> "服务端繁忙或异常（状态 $code）。"
            else -> "请求失败（状态 $code）。"
        }
        val detail = body.take(300).replace("\n", " ")
        return if (detail.isNotBlank()) "$hint 详情：$detail" else hint
    }

    /** 测试连接：发一条极短消息，返回延迟毫秒 */
    suspend fun testConnection(
        preset: ModelPreset,
        baseUrl: String,
        apiKey: String,
        model: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val url = URL(baseUrl.ifBlank { preset.baseUrl })
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) conn.setRequestProperty(preset.authHeader, preset.authScheme + apiKey)
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().put(
                        JSONObject().put("role", "user").put("content", "你好，请回复：ok")
                    ))
                    put("max_tokens", 8)
                    put("stream", false)
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.let { s -> BufferedReader(InputStreamReader(s, Charsets.UTF_8)).use { it.readText() } }
                    ?: ""
                if (code in 200..299) {
                    Result.success(System.currentTimeMillis() - start)
                } else {
                    Result.failure(IllegalStateException(friendlyError(code, text)))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
