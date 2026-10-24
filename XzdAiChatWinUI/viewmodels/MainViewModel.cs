using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.UI.Dispatching;
using XzdAiChatWinUI.data;
using XzdAiChatWinUI.net;

namespace XzdAiChatWinUI.viewmodels;

public partial class MainViewModel : ObservableObject
{
    public Store Store { get; }
    public ObservableCollection<Conversation> Conversations { get; } = new();
    public ObservableCollection<ChatMessage> Messages { get; } = new();

    [ObservableProperty] private Conversation? _currentConversation;
    [ObservableProperty] private string _inputText = "";
    [ObservableProperty] private bool _isBusy;
    [ObservableProperty] private string _statusText = "就绪";
    [ObservableProperty] private bool _showSettings;

    // 流式输出中的"虚拟"消息（不进 Messages 集合，绑到底部气泡）
    [ObservableProperty] private bool _isStreaming;
    [ObservableProperty] private string _liveContent = "";
    [ObservableProperty] private string _liveReasoning = "";
    [ObservableProperty] private bool _showReasoning;

    private CancellationTokenSource? _cts;
    private DispatcherQueue? _uiQueue;

    public MainViewModel()
    {
        Store = App.GlobalStore;
        _uiQueue = ((MainWindow)App.MainWindow!).DispatcherQueue;
        foreach (var c in Store.Conversations) Conversations.Add(c);
        if (Conversations.Count == 0) NewConversation();
        else SelectConversation(Conversations[0]);
    }

    public void NewConversation()
    {
        var c = Store.CreateConversation();
        Conversations.Insert(0, c);
        SelectConversation(c);
    }

    public void SelectConversation(Conversation c)
    {
        CurrentConversation = c;
        Messages.Clear();
        foreach (var m in c.Messages) Messages.Add(m);
    }

    public void DeleteConversation(Conversation c)
    {
        Store.DeleteConversation(c.Id);
        Conversations.Remove(c);
        if (CurrentConversation == c)
        {
            if (Conversations.Count > 0) SelectConversation(Conversations[0]);
            else NewConversation();
        }
    }

    public void TogglePin(Conversation c)
    {
        c.Pinned = !c.Pinned;
        Store.SaveConversations();
        var list = Conversations.OrderByDescending(x => x.Pinned).ThenByDescending(x => x.UpdatedAt).ToList();
        Conversations.Clear();
        foreach (var x in list) Conversations.Add(x);
    }

    public async Task SendAsync()
    {
        if (IsBusy) return;
        var text = InputText.Trim();
        if (string.IsNullOrEmpty(text) || CurrentConversation == null) return;

        var userMsg = new ChatMessage("user", text, Store.NowMs());
        CurrentConversation.Messages.Add(userMsg);
        Messages.Add(userMsg);
        InputText = "";

        if (CurrentConversation.Messages.Count <= 2)
        {
            CurrentConversation.Title = text.Length > 18 ? text.Substring(0, 18) + "…" : text;
            Store.SaveConversations();
        }

        await DoStreamAsync();
    }

    public void Stop() => _cts?.Cancel();

    private async Task DoStreamAsync()
    {
        if (CurrentConversation == null) return;
        IsBusy = true;
        StatusText = "思考中…";
        IsStreaming = true;
        LiveContent = "";
        LiveReasoning = "";
        ShowReasoning = false;
        _cts = new CancellationTokenSource();

        var sbContent = new System.Text.StringBuilder();
        var sbReason = new System.Text.StringBuilder();

        try
        {
            var s = Store.Settings;
            var preset = Store.ResolvedProvider;
            var model = Store.ResolvedModel;
            var baseUrl = Store.ResolvedBaseUrl;
            var reqMsgs = LLMClient.BuildRequestMessages(s.SystemPrompt, CurrentConversation.Messages, s.ContextCount);

            var (content, reasoning) = await LLMClient.ChatStreamAsync(
                preset, baseUrl, s.ApiKey, model, reqMsgs,
                s.Temperature, s.MaxTokens,
                string.IsNullOrWhiteSpace(s.ReasoningEffort) ? null : s.ReasoningEffort,
                (chunk, rchunk) =>
                {
                    sbContent.Append(chunk);
                    sbReason.Append(rchunk);
                    _uiQueue?.TryEnqueue(() =>
                    {
                        LiveContent = sbContent.ToString();
                        if (sbReason.Length > 0)
                        {
                            ShowReasoning = true;
                            LiveReasoning = sbReason.ToString();
                        }
                    });
                },
                _cts.Token);

            var final = new ChatMessage("assistant", content, Store.NowMs(),
                string.IsNullOrEmpty(reasoning) ? null : reasoning);
            CurrentConversation.Messages.Add(final);
            CurrentConversation.UpdatedAt = Store.NowMs();
            Messages.Add(final);
            Store.SaveConversations();
            StatusText = "就绪";
        }
        catch (OperationCanceledException)
        {
            StatusText = "已停止";
            if (sbContent.Length > 0)
            {
                var partial = new ChatMessage("assistant", sbContent.ToString(), Store.NowMs(),
                    sbReason.Length > 0 ? sbReason.ToString() : null);
                CurrentConversation.Messages.Add(partial);
                Messages.Add(partial);
                Store.SaveConversations();
            }
        }
        catch (Exception ex)
        {
            StatusText = "出错";
            var err = new ChatMessage("assistant", "⚠️ " + ex.Message, Store.NowMs());
            Messages.Add(err);
            CurrentConversation.Messages.Add(err);
            Store.SaveConversations();
        }
        finally
        {
            IsBusy = false;
            IsStreaming = false;
            LiveContent = "";
            LiveReasoning = "";
            _cts?.Dispose();
            _cts = null;
        }
    }
}
