package com.xzd1314.aichat.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 本地存储：SharedPreferences + JSON。
 * 保存内容：全部会话、应用级设置（当前模型供应商/API Key/上下文数量/系统提示词等）。
 */
class Store private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("xzd_ai_chat", Context.MODE_PRIVATE)

    // ---------- 会话 ----------

    @Synchronized
    fun loadConversations(): MutableList<Conversation> {
        val raw = prefs.getString(KEY_CONVS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<Conversation>()
            for (i in 0 until arr.length()) list.add(Conversation.fromJson(arr.getJSONObject(i)))
            list.sortedByDescending { it.updatedAt }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @Synchronized
    fun saveConversations(list: List<Conversation>) {
        val arr = JSONArray()
        list.sortedByDescending { it.updatedAt }.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_CONVS, arr.toString()).apply()
    }

    // ===== Agent 会话 =====
    @Synchronized
    fun getAgentConvs(): List<AgentConversation> {
        val raw = prefs.getString(KEY_AGENT_CONVS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { AgentConversation.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveAgentConvs(list: List<AgentConversation>) {
        val arr = JSONArray()
        list.sortedByDescending { it.updatedAt }.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_AGENT_CONVS, arr.toString()).apply()
    }

    fun createAgentConversation(title: String = "新 Agent 会话"): AgentConversation {
        val now = System.currentTimeMillis()
        return AgentConversation(
            id = UUID.randomUUID().toString(),
            title = title,
            messages = mutableListOf(),
            createdAt = now,
            updatedAt = now
        )
    }

    fun createConversation(title: String = "新对话"): Conversation {
        val now = System.currentTimeMillis()
        return Conversation(
            id = UUID.randomUUID().toString(),
            title = title,
            avatarUri = null,
            style = StylePresets.getDefault(),
            stylePresetId = "minimal",
            createdAt = now,
            updatedAt = now,
            messages = mutableListOf()
        )
    }

    // ---------- 设置 ----------

    fun setProviderId(id: String) = prefs.edit().putString(KEY_PROVIDER, id).apply()
    fun getProviderId(): String = prefs.getString(KEY_PROVIDER, "deepseek") ?: "deepseek"

    fun setApiKey(key: String) = prefs.edit().putString(KEY_APIKEY, key.trim()).apply()
    fun getApiKey(): String = prefs.getString(KEY_APIKEY, "") ?: ""

    fun setModelName(m: String) = prefs.edit().putString(KEY_MODEL, m.trim()).apply()
    fun getModelName(): String = prefs.getString(KEY_MODEL, "") ?: ""

    fun setBaseUrl(u: String) = prefs.edit().putString(KEY_BASEURL, u.trim()).apply()
    fun getBaseUrl(): String = prefs.getString(KEY_BASEURL, "") ?: ""

    /** 发送给模型的最近消息条数（上下文窗口） */
    fun setContextCount(n: Int) = prefs.edit().putInt(KEY_CTX, n.coerceIn(4, 64)).apply()
    fun getContextCount(): Int = prefs.getInt(KEY_CTX, 20)

    /** 输出最大 token 数 */
    fun setMaxTokens(n: Int) = prefs.edit().putInt(KEY_MAXTOKENS, n.coerceIn(256, 8192)).apply()
    fun getMaxTokens(): Int = prefs.getInt(KEY_MAXTOKENS, 2048)

    fun setTemperature(t: Float) = prefs.edit().putFloat(KEY_TEMP, t.coerceIn(0f, 2f)).apply()
    fun getTemperature(): Float = prefs.getFloat(KEY_TEMP, 0.7f)

    fun setSystemPrompt(p: String) = prefs.edit().putString(KEY_SYSPROMPT, p.trim()).apply()
    fun getSystemPrompt(): String = prefs.getString(KEY_SYSPROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT

    /** 缓存优化模式：固定前缀 + 完整稳定序列化 */
    fun setCacheOptimized(b: Boolean) = prefs.edit().putBoolean(KEY_CACHE, b).apply()
    fun isCacheOptimized(): Boolean = prefs.getBoolean(KEY_CACHE, true)

    /** 统计 */
    fun bumpStat(roundTripMs: Long) {
        val count = prefs.getInt(KEY_STAT_COUNT, 0) + 1
        val total = prefs.getLong(KEY_STAT_TOTAL, 0L) + roundTripMs
        prefs.edit().putInt(KEY_STAT_COUNT, count).putLong(KEY_STAT_TOTAL, total).apply()
    }
    fun getStatCount(): Int = prefs.getInt(KEY_STAT_COUNT, 0)
    fun getStatTotal(): Long = prefs.getLong(KEY_STAT_TOTAL, 0L)

    /** 思考程度：""=关闭, "low", "medium", "high" */
    fun setReasoningEffort(e: String) = prefs.edit().putString(KEY_REASONING, e).apply()
    fun getReasoningEffort(): String = prefs.getString(KEY_REASONING, "") ?: ""

    fun getResolvedProvider(): ModelPreset = ModelPresets.get(getProviderId())
    fun getResolvedModelName(): String =
        if (getModelName().isNotBlank()) getModelName() else getResolvedProvider().defaultModel
    fun getResolvedBaseUrl(): String =
        if (getBaseUrl().isNotBlank()) getBaseUrl() else getResolvedProvider().baseUrl

    // ===== API Key XOR 加密存储 =====
    private val xorKey = byteArrayOf(0x5A, 0x33, 0x64, 0x31, 0x33, 0x31, 0x34, 0x78)
    private fun encryptStr(s: String): String {
        val bytes = s.toByteArray(Charsets.UTF_8)
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
    private fun decryptStr(s: String): String {
        return try {
            val bytes = android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
            for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) { "" }
    }
    fun setApiKeyEncrypted(key: String) = prefs.edit().putString(KEY_APIKEY_ENC, encryptStr(key.trim())).apply()
    fun getApiKeyEncrypted(): String {
        val enc = prefs.getString(KEY_APIKEY_ENC, "") ?: ""
        return if (enc.isNotBlank()) decryptStr(enc) else getApiKey()  // 兼容旧版明文
    }

    // ===== Prompt 模板管理器 =====
    @Synchronized
    fun getPromptTemplates(): List<PromptTemplate> {
        val raw = prefs.getString(KEY_PROMPT_TPLS, null) ?: return defaultPromptTemplates()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { PromptTemplate.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { defaultPromptTemplates() }
    }
    @Synchronized
    fun savePromptTemplates(list: List<PromptTemplate>) {
        val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_PROMPT_TPLS, arr.toString()).apply()
    }
    fun addPromptTemplate(name: String, content: String): PromptTemplate {
        val list = getPromptTemplates().toMutableList()
        val tpl = PromptTemplate(id = UUID.randomUUID().toString(), name = name, content = content)
        list.add(tpl); savePromptTemplates(list); return tpl
    }
    fun deletePromptTemplate(id: String) {
        savePromptTemplates(getPromptTemplates().filter { it.id != id })
    }
    private fun defaultPromptTemplates(): List<PromptTemplate> = listOf(
        PromptTemplate("default", "默认助手", "你是 XZD AI Chat 中的 AI 助手，名字叫小蓝。回答简洁、准确、友好，用简体中文。"),
        PromptTemplate("debug", "严谨 Debug", "你是一个资深程序员。回答代码问题时必须：1.先复现问题 2.定位根因 3.给出最小修复代码 4.说明副作用。用简体中文，代码用 ``` 包裹。"),
        PromptTemplate("translator", "翻译官", "你是一个专业翻译。用户输入中文时翻译成英文，输入英文时翻译成中文。只输出翻译结果，不要解释。"),
        PromptTemplate("writer", "写作助手", "你是一个创意写作助手。帮用户润色文字、扩写段落、调整语气。保持原文核心意思，只改表达。用简体中文。")
    )

    // ===== Agent 命令历史 =====
    @Synchronized
    fun getCommandHistory(): List<CommandHistoryItem> {
        val raw = prefs.getString(KEY_CMD_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { CommandHistoryItem.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }
    @Synchronized
    fun addCommandHistory(item: CommandHistoryItem) {
        val list = getCommandHistory().toMutableList()
        list.add(0, item)
        if (list.size > 100) list.removeAt(list.size - 1)  // 最多保留 100 条
        val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_CMD_HISTORY, arr.toString()).apply()
    }
    fun clearCommandHistory() = prefs.edit().remove(KEY_CMD_HISTORY).apply()

    // ===== Token 用量统计 =====
    @Synchronized
    fun getTokenUsage(): TokenUsage {
        val raw = prefs.getString(KEY_TOKEN_USAGE, null) ?: return TokenUsage()
        return try { TokenUsage.fromJson(JSONObject(raw)) } catch (e: Exception) { TokenUsage() }
    }
    @Synchronized
    fun recordTokenUsage(inputTokens: Int, outputTokens: Int) {
        val u = getTokenUsage()
        val costCents = (inputTokens * 0.001 + outputTokens * 0.002).toLong()  // 估算：输入1元/M，输出2元/M
        val new = u.copy(
            totalInputTokens = u.totalInputTokens + inputTokens,
            totalOutputTokens = u.totalOutputTokens + outputTokens,
            totalRequests = u.totalRequests + 1,
            totalCostCents = u.totalCostCents + costCents
        )
        prefs.edit().putString(KEY_TOKEN_USAGE, new.toJson().toString()).apply()
    }
    fun resetTokenUsage() = prefs.edit().putString(KEY_TOKEN_USAGE, TokenUsage().toJson().toString()).apply()

    // ===== 定时任务 =====
    @Synchronized
    fun getScheduledTasks(): List<ScheduledTask> {
        val raw = prefs.getString(KEY_SCHEDULED, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { ScheduledTask.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }
    @Synchronized
    fun saveScheduledTasks(list: List<ScheduledTask>) {
        val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SCHEDULED, arr.toString()).apply()
    }
    fun addScheduledTask(name: String, command: String, intervalMinutes: Int): ScheduledTask {
        val list = getScheduledTasks().toMutableList()
        val task = ScheduledTask(id = UUID.randomUUID().toString(), name = name, command = command, intervalMinutes = intervalMinutes)
        list.add(task); saveScheduledTasks(list); return task
    }
    fun deleteScheduledTask(id: String) = saveScheduledTasks(getScheduledTasks().filter { it.id != id })
    fun toggleScheduledTask(id: String) {
        saveScheduledTasks(getScheduledTasks().map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
    }

    // ===== 会话导出 =====
    fun exportConversationAsMarkdown(conv: Conversation): String {
        val sb = StringBuilder()
        sb.append("# ${conv.title}\n\n")
        sb.append("> 导出时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA).format(java.util.Date())}\n\n")
        sb.append("---\n\n")
        conv.messages.forEach { msg ->
            val role = if (msg.role == "user") "🧑 用户" else if (msg.role == "assistant") "🤖 小蓝" else msg.role
            sb.append("### $role\n\n")
            if (msg.reasoning?.isNotBlank() == true) {
                sb.append("<details><summary>思考过程</summary>\n\n${msg.reasoning}\n\n</details>\n\n")
            }
            sb.append("${msg.content}\n\n")
        }
        return sb.toString()
    }
    fun exportConversationAsJson(conv: Conversation): String = conv.toJson().toString(2)
    fun exportConversationAsText(conv: Conversation): String {
        val sb = StringBuilder()
        conv.messages.forEach { msg ->
            val role = if (msg.role == "user") "用户" else "小蓝"
            sb.append("【$role】${msg.content}\n\n")
        }
        return sb.toString()
    }

    companion object {
        private const val KEY_CONVS = "conversations"
        private const val KEY_AGENT_CONVS = "agent_conversations"
        private const val KEY_PROVIDER = "provider_id"
        private const val KEY_APIKEY = "api_key"
        private const val KEY_MODEL = "model_name"
        private const val KEY_BASEURL = "base_url"
        private const val KEY_CTX = "context_count"
        private const val KEY_MAXTOKENS = "max_tokens"
        private const val KEY_TEMP = "temperature"
        private const val KEY_SYSPROMPT = "system_prompt"
        private const val KEY_CACHE = "cache_optimized"
        private const val KEY_STAT_COUNT = "stat_count"
        private const val KEY_STAT_TOTAL = "stat_total"
        private const val KEY_REASONING = "reasoning_effort"
        private const val KEY_APIKEY_ENC = "api_key_enc"
        private const val KEY_PROMPT_TPLS = "prompt_templates"
        private const val KEY_CMD_HISTORY = "command_history"
        private const val KEY_TOKEN_USAGE = "token_usage"
        private const val KEY_SCHEDULED = "scheduled_tasks"

        private const val DEFAULT_SYSTEM_PROMPT =
            "你是 XZD AI Chat 中的 AI 助手，名字叫小蓝。回答简洁、准确、友好，用简体中文。"

        @Volatile private var instance: Store? = null
        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context).also { instance = it }
        }
    }
}
