using System.Collections.Generic;
using System.Linq;

namespace XzdAiChatWinUI.data;

public static class StylePresets
{
    // (id, name, myBubble, aiBubble, myText, aiText, bg, topBar, radius, showAvatar, showTime, fontSize, maxWidthPct, dark)
    public static readonly List<ChatStyle> All = new()
    {
        S("wechat",     "微信",        0xFF95EC69, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFEDEDED, 0xFFEDEDED, 6,  true,  true,  15, 72, false),
        S("qq",         "QQ",          0xFFA8E0FF, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF2F2F2, 0xFFF2F2F2, 12, true,  true,  15, 80, false),
        S("x",          "X (Twitter)", 0xFF1D9BF0, 0xFF2F3336, 0xFFFFFFFF, 0xFFE7E7E7, 0xFF000000, 0xFF16181C, 22, true,  false, 15, 80, true),
        S("imessage",   "iMessage",    0xFFDCF8C6, 0xFFECECEC, 0xFF111111, 0xFF111111, 0xFFFFFFFF, 0xFFF7F7F7, 18, true,  true,  15, 72, false),
        S("telegram",   "Telegram",    0xFF4FA8F5, 0xFF182533, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF0E1621, 0xFF1F2C3C, 18, true,  false, 15, 80, true),
        S("whatsapp",   "WhatsApp",    0xFFDCF8C6, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFECE5DD, 0xFF075E54, 10, true,  true,  15, 78, false),
        S("discord",    "Discord",     0xFF5865F2, 0xFF36393F, 0xFFFFFFFF, 0xFFDCDDDE, 0xFF2C2F33, 0xFF2C2F33, 16, true,  true,  15, 80, true),
        S("line",       "LINE",        0xFFB3E7A8, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF2F2F2, 0xFF06C755, 6,  true,  true,  15, 72, false),
        S("dingtalk",   "钉钉",        0xFFE8F3FF, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFF4F7FB, 0xFF1677FF, 8,  true,  true,  15, 76, false),
        S("bilibili",   "B站私信",     0xFFFB7299, 0xFFFFFFFF, 0xFF111111, 0xFF111111, 0xFFFBFBFB, 0xFFFB7299, 12, true,  true,  15, 80, false),
        S("zhihu",      "知乎",        0xFF0084FF, 0xFFF6F6F6, 0xFFFFFFFF, 0xFF121212, 0xFFFFFFFF, 0xFFFFFFFF, 10, true, true,  15, 80, false),
        S("matrix",     "终端 Matrix", 0xFF002200, 0xFF001800, 0xFF00FF41, 0xFF00CC33, 0xFF000000, 0xFF001100, 4,  false, true,  15, 90, true),
        S("pixel",      "复古像素",    0xFFC0C0C0, 0xFFA8A8A8, 0xFF222222, 0xFF222222, 0xFF888888, 0xFF666666, 2,  true,  true,  15, 70, true),
        S("minimal",    "极简",        0xFFF1F3F4, 0xFFE4E6E8, 0xFF111111, 0xFF111111, 0xFFFFFFFF, 0xFFFFFFFF, 24, false, false, 16, 84, false),
        S("dark",       "深邃暗黑",    0xFF2E2E2E, 0xFF1F1F1F, 0xFFE8E8E8, 0xFFBDBDBD, 0xFF121212, 0xFF1A1A1A, 16, true,  true,  15, 80, true),
        S("cyberpunk",  "赛博朋克",    0xFFFF00E5, 0xFF00E5FF, 0xFF000000, 0xFF000000, 0xFF0A0014, 0xFF2A004A, 6,  true,  true,  15, 80, true),
        S("sakura",     "少女粉",      0xFFFFD1DC, 0xFFFFFFFF, 0xFF8A4A5A, 0xFF6E3B49, 0xFFFFF0F4, 0xFFFFB6C1, 16, true, true,  15, 76, false),
        S("gold",       "商务黑金",    0xFF1E1A10, 0xFF2A2418, 0xFFD4AF37, 0xFFE8C96A, 0xFF0D0B06, 0xFF1E1A10, 4,  true,  true,  15, 78, true),
        S("green",      "护眼绿",      0xFFD7E8C7, 0xFFF0F7E9, 0xFF2E4A1E, 0xFF3A5A2A, 0xFFCBE0C3, 0xFFA8C69F, 10, true, true,  16, 80, false),
        S("paper",      "纸质日记",    0xFFF7EFD6, 0xFFFDF6E3, 0xFF4A3F2A, 0xFF4A3F2A, 0xFFF5EEDC, 0xFFE8DCC0, 8,  true,  true,  15, 78, false),
        S("neon",       "霓虹夜",      0xFFFF6B6B, 0xFF4ECDC4, 0xFF1A1A2E, 0xFF1A1A2E, 0xFF0F0F23, 0xFF16213E, 20, true, true,  15, 80, true),
        S("mono",       "黑白极客",    0xFF111111, 0xFFF2F2F2, 0xFFFFFFFF, 0xFF111111, 0xFFFFFFFF, 0xFF000000, 0,  true,  true,  15, 70, false),
    };

    public static ChatStyle Get(string id) => All.FirstOrDefault(s => s.Id == id) ?? All.First(s => s.Id == "minimal");
    public static ChatStyle Default => Get("minimal");

    private static ChatStyle S(string id, string name, uint myB, uint aiB, uint myT, uint aiT,
        uint bg, uint topBar, int radius, bool showAvatar, bool showTime, int fontSize, int maxW, bool dark)
    {
        return new ChatStyle
        {
            Id = id, Name = name,
            MyBubble = myB, AiBubble = aiB, MyText = myT, AiText = aiT,
            Bg = bg, TopBar = topBar, Radius = radius,
            ShowAvatar = showAvatar, ShowTime = showTime,
            FontSize = fontSize, MaxWidthPct = maxW, IsDark = dark,
        };
    }
}
