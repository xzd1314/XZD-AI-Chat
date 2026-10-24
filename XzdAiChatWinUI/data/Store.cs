using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace XzdAiChatWinUI.data;

/// <summary>
/// 本地存储：所有数据存放在 %AppData%\XzdAiChat\ 下。
/// conversations.json / settings.json
/// </summary>
public class Store
{
    private static readonly JsonSerializerOptions Opt = new()
    {
        WriteIndented = true,
        PropertyNameCaseInsensitive = true,
    };

    public string DataDir { get; }
    private string ConvsPath => Path.Combine(DataDir, "conversations.json");
    private string SettingsPath => Path.Combine(DataDir, "settings.json");

    public List<Conversation> Conversations { get; private set; } = new();
    public AppSettings Settings { get; private set; } = new();

    public Store()
    {
        DataDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "XzdAiChat");
        Directory.CreateDirectory(DataDir);
        Load();
    }

    private void Load()
    {
        try
        {
            if (File.Exists(ConvsPath))
            {
                var json = File.ReadAllText(ConvsPath);
                Conversations = JsonSerializer.Deserialize<List<Conversation>>(json, Opt) ?? new();
            }
            if (File.Exists(SettingsPath))
            {
                var json = File.ReadAllText(SettingsPath);
                Settings = JsonSerializer.Deserialize<AppSettings>(json, Opt) ?? new AppSettings();
            }
        }
        catch
        {
            Conversations ??= new();
            Settings ??= new AppSettings();
        }
        // 默认按 updatedAt 倒序，置顶在前
        SortConversations();
    }

    private readonly object _ioLock = new();

    public void SaveConversations()
    {
        lock (_ioLock)
        {
            SortConversations();
            File.WriteAllText(ConvsPath, JsonSerializer.Serialize(Conversations, Opt));
        }
    }

    public void SaveSettings()
    {
        lock (_ioLock)
        {
            File.WriteAllText(SettingsPath, JsonSerializer.Serialize(Settings, Opt));
        }
    }

    private void SortConversations()
    {
        Conversations = Conversations
            .OrderByDescending(c => c.Pinned)
            .ThenByDescending(c => c.UpdatedAt)
            .ToList();
    }

    public Conversation CreateConversation()
    {
        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var c = new Conversation
        {
            Id = Guid.NewGuid().ToString(),
            Title = "新对话",
            Style = StylePresets.Default,
            StylePresetId = "minimal",
            CreatedAt = now,
            UpdatedAt = now,
        };
        Conversations.Insert(0, c);
        SaveConversations();
        return c;
    }

    public void DeleteConversation(string id)
    {
        Conversations.RemoveAll(c => c.Id == id);
        SaveConversations();
    }

    public void ApplyStyle(string convId, string presetId)
    {
        var c = Conversations.FirstOrDefault(x => x.Id == convId);
        if (c == null) return;
        var s = StylePresets.Get(presetId);
        c.StylePresetId = presetId;
        // 拷贝一份，避免共享引用
        c.Style = new ChatStyle
        {
            Id = s.Id, Name = s.Name,
            MyBubble = s.MyBubble, AiBubble = s.AiBubble,
            MyText = s.MyText, AiText = s.AiText,
            Bg = s.Bg, TopBar = s.TopBar, Radius = s.Radius,
            ShowAvatar = s.ShowAvatar, ShowTime = s.ShowTime,
            FontSize = s.FontSize, MaxWidthPct = s.MaxWidthPct, IsDark = s.IsDark,
        };
        SaveConversations();
    }

    public ModelPreset ResolvedProvider => ModelPresets.Get(Settings.ProviderId);
    public string ResolvedModel =>
        string.IsNullOrWhiteSpace(Settings.ModelName) ? ResolvedProvider.DefaultModel : Settings.ModelName.Trim();
    public string ResolvedBaseUrl
    {
        get
        {
            if (Settings.DeploymentMode == "local" && !string.IsNullOrWhiteSpace(Settings.BaseUrl))
                return Settings.BaseUrl.Trim();
            return string.IsNullOrWhiteSpace(Settings.BaseUrl) ? ResolvedProvider.BaseUrl : Settings.BaseUrl.Trim();
        }
    }

    public static long NowMs() => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
}
