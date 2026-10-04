package com.xzd1314.aichat.data

/** 内置聊天风格预设库：微信/QQ/X/iMessage/Telegram 等 24 套 */
object StylePresets {

    val all: List<ChatStyle> = listOf(
        ChatStyle("wechat", "微信", 0xFF95EC69, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFEDEDED, 0xFFEDEDED, 6, true, true, false, 15, 72, false),
        ChatStyle("qq", "QQ", 0xFFA8E0FF, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF2F2F2, 0xFFF2F2F2, 12, true, true, false, 15, 80, false),
        ChatStyle("x", "X (Twitter)", 0xFF1D9BF0, 0xFF2F3336, 0xFFFFFFFF, 0xFFE7E7E7, 0xFF000000, 0xFF16181C, 22, true, false, false, 15, 80, true),
        ChatStyle("imessage", "iMessage", 0xFFDCF8C6, 0xFFECECEC, 0xFF111111, 0xFF111111, 0xFFFFFFFF, 0xFFF7F7F7, 18, true, true, false, 15, 72, false),
        ChatStyle("telegram", "Telegram", 0xFF4FA8F5, 0xFF182533, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF0E1621, 0xFF1F2C3C, 18, true, false, false, 15, 80, true),
        ChatStyle("whatsapp", "WhatsApp", 0xFFDCF8C6, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFECE5DD, 0xFF075E54, 10, true, true, false, 15, 78, false),
        ChatStyle("discord", "Discord", 0xFF5865F2, 0xFF36393F, 0xFFFFFFFF, 0xFFDCDDDE, 0xFF2C2F33, 0xFF2C2F33, 16, true, true, false, 15, 80, true),
        ChatStyle("line", "LINE", 0xFFB3E7A8, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF2F2F2, 0xFF06C755, 6, true, true, false, 15, 72, false),
        ChatStyle("dingtalk", "钉钉", 0xFFE8F3FF, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF4F7FB, 0xFF1677FF, 8, true, true, false, 15, 76, false),
        ChatStyle("wecom", "企业微信", 0xFFD8F0DB, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF5F5F5, 0xFF00875A, 6, true, true, false, 15, 72, false),
        ChatStyle("slack", "Slack", 0xFF4A154B, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1D1C1D, 0xFFFFFFFF, 0xFF4A154B, 12, true, true, true, 15, 76, false),
        ChatStyle("signal", "Signal", 0xFF2E7CF6, 0xFFE5E5E5, 0xFFFFFFFF, 0xFF111111, 0xFFF7F7F7, 0xFF3B3B3B, 16, true, true, false, 15, 76, false),
        ChatStyle("messenger", "Messenger", 0xFF0084FF, 0xFFE7E7E7, 0xFFFFFFFF, 0xFF111111, 0xFFFFFFFF, 0xFF0084FF, 16, true, true, false, 15, 80, false),
        ChatStyle("instagram", "Instagram", 0xFFE1306C, 0xFF262626, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF121212, 20, true, true, false, 15, 80, true),
        ChatStyle("reddit", "Reddit", 0xFFFF6314, 0xFFE9E9E9, 0xFFFFFFFF, 0xFF1A1A1B, 0xFFFFFFFF, 0xFFFF4500, 14, true, true, false, 15, 80, false),
        ChatStyle("bilibili", "B站私信", 0xFFFB7299, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFFBFBFB, 0xFFFB7299, 12, true, true, false, 15, 80, false),
        ChatStyle("xiaohongshu", "小红书", 0xFFFF2442, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF111111, 0xFFFCFCFC, 0xFFFF2442, 14, true, true, false, 15, 78, false),
        ChatStyle("weibo", "微博", 0xFFE6162D, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF111111, 0xFFF7F7F7, 0xFFE6162D, 6, true, true, false, 15, 76, false),
        ChatStyle("zhihu", "知乎", 0xFF0084FF, 0xFFF6F6F6, 0xFFFFFFFF, 0xFF121212, 0xFFFFFFFF, 0xFFFFFFFF, 10, true, true, false, 15, 80, false),
        ChatStyle("douyin", "抖音", 0xFF2A2A2A, 0xFF1F1F1F, 0xFFFE2C55, 0xFFE8E8E8, 0xFF111111, 0xFF111111, 18, true, true, false, 15, 80, true),
        ChatStyle("matrix", "终端 Matrix", 0xFF002200, 0xFF001800, 0xFF00FF41, 0xFF00CC33, 0xFF000000, 0xFF001100, 4, false, true, false, 15, 90, true),
        ChatStyle("pixel", "复古像素", 0xFFC0C0C0, 0xFFA8A8A8, 0xFF222222, 0xFF222222, 0xFF888888, 0xFF666666, 2, true, true, false, 15, 70, true),
        ChatStyle("minimal", "极简", 0xFFF1F3F4, 0xFFE4E6E8, 0xFF111111, 0xFF111111, 0xFFFFFFFF, 0xFFFFFFFF, 24, false, false, false, 16, 84, false),
        ChatStyle("dark", "深邃暗黑", 0xFF2E2E2E, 0xFF1F1F1F, 0xFFE8E8E8, 0xFFBDBDBD, 0xFF121212, 0xFF1A1A1A, 16, true, true, false, 15, 80, true),
        ChatStyle("cyberpunk", "赛博朋克", 0xFFFF00E5, 0xFF00E5FF, 0xFF000000, 0xFF000000, 0xFF0A0014, 0xFF2A004A, 6, true, true, false, 15, 80, true),
        ChatStyle("sakura", "少女粉", 0xFFFFD1DC, 0xFFFFFFFF, 0xFF8A4A5A, 0xFF6E3B49, 0xFFFFF0F4, 0xFFFFB6C1, 16, true, true, false, 15, 76, false),
        ChatStyle("gold", "商务黑金", 0xFF1E1A10, 0xFF2A2418, 0xFFD4AF37, 0xFFE8C96A, 0xFF0D0B06, 0xFF1E1A10, 4, true, true, false, 15, 78, true),
        ChatStyle("green", "护眼绿", 0xFFD7E8C7, 0xFFF0F7E9, 0xFF2E4A1E, 0xFF3A5A2A, 0xFFCBE0C3, 0xFFA8C69F, 10, true, true, false, 16, 80, false),
        ChatStyle("paper", "纸质日记", 0xFFF7EFD6, 0xFFFDF6E3, 0xFF4A3F2A, 0xFF4A3F2A, 0xFFF5EEDC, 0xFFE8DCC0, 8, true, true, false, 15, 78, false),
        ChatStyle("neon", "霓虹夜", 0xFFFF6B6B, 0xFF4ECDC4, 0xFF1A1A2E, 0xFF1A1A2E, 0xFF0F0F23, 0xFF16213E, 20, true, true, false, 15, 80, true),
        ChatStyle("mono", "黑白极客", 0xFF111111, 0xFFF2F2F2, 0xFFFFFFFF, 0xFF111111, 0xFFFFFFFF, 0xFF000000, 0, true, true, false, 15, 70, false)
    )

    private val byId: Map<String, ChatStyle> = all.associateBy { it.id }

    fun get(id: String): ChatStyle = byId[id] ?: all.first()

    fun getDefault(): ChatStyle = get("minimal")
}
