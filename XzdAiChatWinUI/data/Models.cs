using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json.Serialization;
using Windows.UI;

namespace XzdAiChatWinUI.data;

public class ChatMessage
{
    [JsonPropertyName("role")] public string Role { get; set; } = "user";
    [JsonPropertyName("content")] public string Content { get; set; } = "";
    [JsonPropertyName("ts")] public long Ts { get; set; }
    [JsonPropertyName("reasoning")] public string? Reasoning { get; set; }

    [JsonIgnore]
    public string DisplayReasoning => Reasoning ?? "";
    [JsonIgnore] public bool HasReasoning => !string.IsNullOrEmpty(Reasoning);

    public ChatMessage() { }
    public ChatMessage(string role, string content, long ts, string? reasoning = null)
    {
        Role = role; Content = content; Ts = ts; Reasoning = reasoning;
    }
}

public class ChatStyle
{
    [JsonPropertyName("id")] public string Id { get; set; } = "minimal";
    [JsonPropertyName("name")] public string Name { get; set; } = "极简";
    // ARGB 存储为 uint，与 Android 端 long 兼容
    [JsonPropertyName("myBubble")] public uint MyBubble { get; set; } = 0xFFF1F3F4;
    [JsonPropertyName("aiBubble")] public uint AiBubble { get; set; } = 0xFFE4E6E8;
    [JsonPropertyName("myText")] public uint MyText { get; set; } = 0xFF111111;
    [JsonPropertyName("aiText")] public uint AiText { get; set; } = 0xFF111111;
    [JsonPropertyName("bg")] public uint Bg { get; set; } = 0xFFFFFFFF;
    [JsonPropertyName("topBar")] public uint TopBar { get; set; } = 0xFFFFFFFF;
    [JsonPropertyName("radius")] public int Radius { get; set; } = 24;
    [JsonPropertyName("showAvatar")] public bool ShowAvatar { get; set; } = false;
    [JsonPropertyName("showTime")] public bool ShowTime { get; set; } = false;
    [JsonPropertyName("fontSize")] public int FontSize { get; set; } = 16;
    [JsonPropertyName("maxWidth")] public int MaxWidthPct { get; set; } = 84;
    [JsonPropertyName("dark")] public bool IsDark { get; set; }

    [JsonIgnore]
    public Color MyBubbleColor => UIntToColor(MyBubble);
    [JsonIgnore] public Color AiBubbleColor => UIntToColor(AiBubble);
    [JsonIgnore] public Color MyTextColor => UIntToColor(MyText);
    [JsonIgnore] public Color AiTextColor => UIntToColor(AiText);
    [JsonIgnore] public Color BgColor => UIntToColor(Bg);
    [JsonIgnore] public Color TopBarColor => UIntToColor(TopBar);

    public static Color UIntToColor(uint argBgr)
    {
        // Android ARGB: 0xAARRGGBB
        byte a = (byte)(argBgr >> 24);
        byte r = (byte)(argBgr >> 16);
        byte g = (byte)(argBgr >> 8);
        byte b = (byte)(argBgr);
        return Color.FromArgb(a, r, g, b);
    }
}

public class Conversation
{
    [JsonPropertyName("id")] public string Id { get; set; } = Guid.NewGuid().ToString();
    [JsonPropertyName("title")] public string Title { get; set; } = "新对话";
    [JsonPropertyName("style")] public ChatStyle Style { get; set; } = new();
    [JsonPropertyName("presetId")] public string StylePresetId { get; set; } = "minimal";
    [JsonPropertyName("createdAt")] public long CreatedAt { get; set; }
    [JsonPropertyName("updatedAt")] public long UpdatedAt { get; set; }
    [JsonPropertyName("pinned")] public bool Pinned { get; set; }
    [JsonPropertyName("messages")] public List<ChatMessage> Messages { get; set; } = new();
}

public class ModelPreset
{
    public string Id { get; init; } = "";
    public string Name { get; init; } = "";
    public string BaseUrl { get; init; } = "";
    public string DefaultModel { get; init; } = "";
    public string AuthHeader { get; init; } = "Authorization";
    public string AuthScheme { get; init; } = "Bearer ";
    public string Doc { get; init; } = "";

    public ModelPreset() { }
    public ModelPreset(string id, string name, string baseUrl, string defModel,
        string authHeader, string authScheme, string doc)
    {
        Id = id; Name = name; BaseUrl = baseUrl; DefaultModel = defModel;
        AuthHeader = authHeader; AuthScheme = authScheme; Doc = doc;
    }
}

public class AppSettings
{
    [JsonPropertyName("providerId")] public string ProviderId { get; set; } = "deepseek";
    [JsonPropertyName("apiKey")] public string ApiKey { get; set; } = "";
    [JsonPropertyName("modelName")] public string ModelName { get; set; } = "";
    [JsonPropertyName("baseUrl")] public string BaseUrl { get; set; } = "";
    [JsonPropertyName("contextCount")] public int ContextCount { get; set; } = 20;
    [JsonPropertyName("maxTokens")] public int MaxTokens { get; set; } = 2048;
    [JsonPropertyName("temperature")] public double Temperature { get; set; } = 0.7;
    [JsonPropertyName("systemPrompt")] public string SystemPrompt { get; set; } =
        "你是 XZD AI Chat 中的 AI 助手，名字叫小蓝。回答简洁、准确、友好，用简体中文。";
    [JsonPropertyName("reasoningEffort")] public string ReasoningEffort { get; set; } = "";
    [JsonPropertyName("deploymentMode")] public string DeploymentMode { get; set; } = "cloud";
    [JsonPropertyName("themeMode")] public string ThemeMode { get; set; } = "system";
}
