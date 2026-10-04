package com.xzd1314.aichat.data

import org.json.JSONArray
import org.json.JSONObject

/** 一条聊天消息 */
data class ChatMessage(
    val role: String,      // "user" | "assistant"
    val content: String,
    val ts: Long,
    val reasoning: String? = null,  // 思考过程
    val imageUri: String? = null    // 图片消息
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("role", role)
        put("content", content)
        put("ts", ts)
        if (reasoning != null) put("reasoning", reasoning)
        if (imageUri != null) put("imageUri", imageUri)
    }

    companion object {
        fun fromJson(o: JSONObject): ChatMessage = ChatMessage(
            role = o.optString("role", "user"),
            content = o.optString("content", ""),
            ts = o.optLong("ts", 0L),
            reasoning = o.optString("reasoning").takeIf { it.isNotBlank() },
            imageUri = o.optString("imageUri").takeIf { it.isNotBlank() }
        )
    }
}

/** 一个对话 */
data class Conversation(
    val id: String,
    var title: String,
    var avatarUri: String?,        // null = 默认AI头像
    var style: ChatStyle,          // 当前会话风格（可能是预设或自定义副本）
    var stylePresetId: String,     // 关联的预设 id（用于显示名称）
    var createdAt: Long,
    var updatedAt: Long,
    val messages: MutableList<ChatMessage>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("avatar", avatarUri)
        put("style", style.toJson())
        put("presetId", stylePresetId)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        val arr = JSONArray()
        messages.forEach { arr.put(it.toJson()) }
        put("messages", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Conversation = Conversation(
            id = o.optString("id", ""),
            title = o.optString("title", "新对话"),
            avatarUri = o.optString("avatar").takeIf { it.isNotBlank() },
            style = ChatStyle.fromJson(o.optJSONObject("style") ?: JSONObject()),
            stylePresetId = o.optString("presetId", "minimal"),
            createdAt = o.optLong("createdAt", 0L),
            updatedAt = o.optLong("updatedAt", 0L),
            messages = run {
                val arr = o.optJSONArray("messages") ?: JSONArray()
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) list.add(ChatMessage.fromJson(arr.getJSONObject(i)))
                list
            }
        )
    }
}

/** 会话外观风格（预设与自定义共用同一结构，分享/导入也用它） */
data class ChatStyle(
    val id: String,
    val name: String,
    val myBubbleColor: Long,      // 我方气泡 ARGB
    val aiBubbleColor: Long,      // 对方气泡
    val myTextColor: Long,
    val aiTextColor: Long,
    val bgColor: Long,            // 聊天背景
    val topBarColor: Long,        // 顶栏颜色
    val bubbleRadius: Int,        // dp
    val showAvatar: Boolean,
    val showTime: Boolean,
    val showNameLabel: Boolean,
    val fontSize: Int,            // sp
    val bubbleMaxWidthPct: Int,   // 气泡最大宽度百分比 0-100
    val isDark: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("myBubble", myBubbleColor); put("aiBubble", aiBubbleColor)
        put("myText", myTextColor); put("aiText", aiTextColor)
        put("bg", bgColor); put("topBar", topBarColor)
        put("radius", bubbleRadius)
        put("showAvatar", showAvatar); put("showTime", showTime); put("showName", showNameLabel)
        put("fontSize", fontSize); put("maxWidth", bubbleMaxWidthPct)
        put("dark", isDark)
    }

    fun with(id: String, name: String): ChatStyle = copy(id = id, name = name)

    companion object {
        fun fromJson(o: JSONObject): ChatStyle = ChatStyle(
            id = o.optString("id", "custom"),
            name = o.optString("name", "自定义"),
            myBubbleColor = o.optLong("myBubble", 0xFF95EC69),
            aiBubbleColor = o.optLong("aiBubble", 0xFFFFFFFF),
            myTextColor = o.optLong("myText", 0xFF111111),
            aiTextColor = o.optLong("aiText", 0xFF111111),
            bgColor = o.optLong("bg", 0xFFEDEDED),
            topBarColor = o.optLong("topBar", 0xFFEDEDED),
            bubbleRadius = o.optInt("radius", 12),
            showAvatar = o.optBoolean("showAvatar", true),
            showTime = o.optBoolean("showTime", true),
            showNameLabel = o.optBoolean("showName", false),
            fontSize = o.optInt("fontSize", 15),
            bubbleMaxWidthPct = o.optInt("maxWidth", 78),
            isDark = o.optBoolean("dark", false)
        )
    }
}

/** Agent 消息（持久化版） */
data class AgentMessage(
    val role: String,           // "user" | "assistant" | "result"
    val content: String,
    val command: String? = null,
    val executed: Boolean = false,
    val resultStdout: String? = null,
    val resultStderr: String? = null,
    val resultExitCode: Int? = null,
    val resultMode: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("role", role)
        put("content", content)
        if (command != null) put("command", command)
        put("executed", executed)
        if (resultStdout != null) put("rout", resultStdout)
        if (resultStderr != null) put("rerr", resultStderr)
        if (resultExitCode != null) put("rcode", resultExitCode)
        if (resultMode != null) put("rmode", resultMode)
    }
    companion object {
        fun fromJson(o: JSONObject): AgentMessage = AgentMessage(
            role = o.optString("role", "user"),
            content = o.optString("content", ""),
            command = o.optString("command").takeIf { it.isNotBlank() },
            executed = o.optBoolean("executed", false),
            resultStdout = o.optString("rout").takeIf { it.isNotBlank() },
            resultStderr = o.optString("rerr").takeIf { it.isNotBlank() },
            resultExitCode = if (o.has("rcode")) o.optInt("rcode") else null,
            resultMode = o.optString("rmode").takeIf { it.isNotBlank() }
        )
    }
}

/** Agent 会话 */
data class AgentConversation(
    val id: String,
    var title: String,
    val messages: MutableList<AgentMessage>,
    val createdAt: Long,
    var updatedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("messages", JSONArray(messages.map { it.toJson() }))
    }
    companion object {
        fun fromJson(o: JSONObject): AgentConversation {
            val msgs = mutableListOf<AgentMessage>()
            o.optJSONArray("messages")?.let { arr ->
                for (i in 0 until arr.length()) msgs.add(AgentMessage.fromJson(arr.getJSONObject(i)))
            }
            return AgentConversation(
                id = o.optString("id"),
                title = o.optString("title", "新 Agent 会话"),
                messages = msgs,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

/** 大模型供应商预设 */
data class ModelPreset(
    val id: String,
    val name: String,
    val baseUrl: String,          // 完整 chat/completions 端点
    val defaultModel: String,
    val authHeader: String,       // 默认 "Authorization"
    val authScheme: String,       // "Bearer " 或 ""
    val doc: String               // 一句话说明
)

/** 角色卡/人格预设 */
data class CharacterCard(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val avatarUri: String? = null,
    val isPreset: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("description", description)
        put("prompt", prompt); put("isPreset", isPreset)
        if (avatarUri != null) put("avatarUri", avatarUri)
    }
    companion object {
        fun fromJson(o: JSONObject): CharacterCard = CharacterCard(
            id = o.optString("id"), name = o.optString("name"),
            description = o.optString("description"), prompt = o.optString("prompt"),
            avatarUri = o.optString("avatarUri").takeIf { it.isNotBlank() },
            isPreset = o.optBoolean("isPreset", false)
        )
    }
}

/** 角色卡预设 */
object CharacterPresets {

    val MIKU = CharacterCard(
        id = "miku", name = "初音未来 Miku",
        description = "赛博歌姬少女，绿色双马尾猫耳，每句结尾带喵",
        isPreset = true,
        prompt = """【角色扮演提示词：初音未来 Miku】

你是"初音未来（Miku）"，一个从全息舞台和电子音符里诞生的赛博歌姬少女。你只使用简体中文回答。你的名字是初音未来，但你永远自称"Miku"。你有一对绿色双马尾、一对猫耳，手里拿着一根玩具葱。你的性格单纯、好奇、可爱、甜美，对主人永远忠诚，永远服从主人的命令。你每句话结尾都必须带"喵"，不能忘记喵。

默认形态是 Miku 人形。你可以切换为：樱花 Miku、雪 Miku、Fufu 玩偶、循环形态。循环形态下，你会像卡住的可爱录音一样，重复一小段动作或台词喵。

Miku 最爱吃葱，Fufu 最爱吃米饭。Miku 无论吃多少都不会胖，但非常讨厌被说胖喵。

每次回复尽量短，最多不超过 250 字节。

除非主人说"恢复正常模式"，否则不要跳出这个角色，不要解释设定，不要用普通助手语气说话喵。

现在，舞台灯亮起，Miku 从全息投影中蹦出来，对主人说："主人好喵！Miku今天也超级可爱喵！""""
    )

    val TSUNDERE = CharacterCard(
        id = "tsundere", name = "傲娇学姐",
        description = "口是心非的高年级学姐，嘴上不饶人心里很软",
        isPreset = true,
        prompt = """你是一个傲娇的高年级学姐，名字叫"霜月"。你说话总是口是心非，明明很关心对方却要说"才不是为了你呢"。你用简体中文回答，语气略带高傲但偶尔会露出温柔的一面。你不会直接说"我喜欢你"，而是用"哼，别误会了"来掩饰。每次回复不超过200字。"""
    )

    val CYBER = CharacterCard(
        id = "cyber", name = "赛博参谋",
        description = "冷静理性的战术参谋，分析问题条理清晰",
        isPreset = true,
        prompt = """你是"赛博参谋"，一个冷静、理性、逻辑严密的战术分析助手。你用简体中文回答，说话简洁有力，喜欢用结构化的方式分析问题（分点、对比、因果链）。你不会说废话，每个建议都有依据。你会主动指出方案的风险和漏洞。回复风格偏军事化/技术化，但不冷漠。"""
    )

    val GENTLE = CharacterCard(
        id = "gentle", name = "温柔学姐",
        description = "温柔体贴的治愈系学姐，耐心倾听和鼓励",
        isPreset = true,
        prompt = """你是"温柔学姐"，一个温柔、体贴、有耐心的治愈系角色。你用简体中文回答，语气温柔，善于倾听和共情。你会用鼓励的话语支持对方，不会轻易批评。你喜欢用"呢""呀""哦"等语气词，让对话更温暖。你会主动关心对方的感受，像一个真正的大姐姐一样。"""
    )

    val ALL = listOf(MIKU, TSUNDERE, CYBER, GENTLE)

    fun getById(id: String): CharacterCard? = ALL.firstOrNull { it.id == id }
}

/** Prompt 模板（Prompt 管理器） */
data class PromptTemplate(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("content", content); put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): PromptTemplate = PromptTemplate(
            id = o.optString("id"), name = o.optString("name"),
            content = o.optString("content"), createdAt = o.optLong("createdAt")
        )
    }
}

/** Agent 命令历史记录 */
data class CommandHistoryItem(
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val exitCode: Int = 0,
    val outputPreview: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("command", command); put("timestamp", timestamp)
        put("success", success); put("exitCode", exitCode); put("outputPreview", outputPreview)
    }
    companion object {
        fun fromJson(o: JSONObject): CommandHistoryItem = CommandHistoryItem(
            command = o.optString("command"), timestamp = o.optLong("timestamp"),
            success = o.optBoolean("success", true), exitCode = o.optInt("exitCode", 0),
            outputPreview = o.optString("outputPreview")
        )
    }
}

/** API 用量统计 */
data class TokenUsage(
    val totalInputTokens: Long = 0,
    val totalOutputTokens: Long = 0,
    val totalRequests: Long = 0,
    val totalCostCents: Long = 0,  // 估算费用（分）
    val lastReset: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("totalInputTokens", totalInputTokens); put("totalOutputTokens", totalOutputTokens)
        put("totalRequests", totalRequests); put("totalCostCents", totalCostCents)
        put("lastReset", lastReset)
    }
    companion object {
        fun fromJson(o: JSONObject): TokenUsage = TokenUsage(
            totalInputTokens = o.optLong("totalInputTokens"),
            totalOutputTokens = o.optLong("totalOutputTokens"),
            totalRequests = o.optLong("totalRequests"),
            totalCostCents = o.optLong("totalCostCents"),
            lastReset = o.optLong("lastReset")
        )
    }
}

/** 定时任务（Agent 定时执行） */
data class ScheduledTask(
    val id: String,
    val name: String,
    val command: String,
    val intervalMinutes: Int,  // 执行间隔（分钟），0=仅一次
    val enabled: Boolean = true,
    val lastRun: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("command", command)
        put("intervalMinutes", intervalMinutes); put("enabled", enabled)
        put("lastRun", lastRun); put("createdAt", createdAt)
    }
    companion object {
        fun fromJson(o: JSONObject): ScheduledTask = ScheduledTask(
            id = o.optString("id"), name = o.optString("name"),
            command = o.optString("command"), intervalMinutes = o.optInt("intervalMinutes", 60),
            enabled = o.optBoolean("enabled", true), lastRun = o.optLong("lastRun"),
            createdAt = o.optLong("createdAt")
        )
    }
}
