using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Windows.UI.Core;
using XzdAiChatWinUI.data;
using XzdAiChatWinUI.ui;
using XzdAiChatWinUI.viewmodels;

namespace XzdAiChatWinUI;

public sealed partial class MainWindow : Window
{
    private MainViewModel Vm { get; }
    private bool _suppressSettingsEvents;

    public MainWindow()
    {
        this.InitializeComponent();
        try { this.SystemBackdrop = new Microsoft.UI.Xaml.Media.MicaBackdrop(); } catch { }
        Vm = new MainViewModel();

        Vm.PropertyChanged += Vm_PropertyChanged;

        ReloadProviderCombo();
        SyncSettingsToUi();
        ReloadConversations();
        ReloadMessages();
    }

    // ---------- 会话列表 ----------
    private void ReloadConversations()
    {
        ConvPanel.Children.Clear();
        foreach (var c in Vm.Conversations)
        {
            var btn = new Button
            {
                Background = c == Vm.CurrentConversation
                    ? new SolidColorBrush(Color.FromArgb(0x22, 0x88, 0x88, 0x88))
                    : new SolidColorBrush(Colors.Transparent),
                BorderThickness = new Thickness(0),
                CornerRadius = new CornerRadius(6),
                Padding = new Thickness(10, 8, 10, 8),
                HorizontalAlignment = HorizontalAlignment.Stretch,
                HorizontalContentAlignment = HorizontalAlignment.Stretch,
            };
            var stack = new StackPanel { Orientation = Orientation.Horizontal };
            var title = new TextBlock
            {
                Text = (c.Pinned ? "📌 " : "") + c.Title,
                TextTrimming = TextTrimming.CharacterEllipsis,
                VerticalAlignment = VerticalAlignment.Center,
                MaxWidth = 180,
            };
            var del = new Button
            {
                Content = "✕",
                Background = new SolidColorBrush(Colors.Transparent),
                BorderThickness = new Thickness(0),
                FontSize = 12,
                Foreground = new SolidColorBrush(Color.FromArgb(0xAA, 0x88, 0x88, 0x88)),
                Margin = new Thickness(8, 0, 0, 0),
                Tag = c,
            };
            del.Click += (s, e) => { Vm.DeleteConversation(c); ReloadConversations(); ReloadMessages(); };
            stack.Children.Add(title);
            stack.Children.Add(del);
            btn.Content = stack;
            btn.Click += (s, e) => { Vm.SelectConversation(c); ReloadConversations(); ReloadMessages(); };
            ConvPanel.Children.Add(btn);
        }
    }

    // ---------- 消息列表 ----------
    private void ReloadMessages()
    {
        MsgPanel.Children.Clear();
        var conv = Vm.CurrentConversation;
        if (conv == null) return;
        TitleText.Text = conv.Title;
        ApplyChatBackground(conv.Style);
        foreach (var m in conv.Messages) AddMessageBubble(m, conv.Style);
        ScrollToEnd();
    }

    private void ApplyChatBackground(ChatStyle s)
    {
        ChatArea.Background = new SolidColorBrush(s.BgColor);
        TopBar.Background = new SolidColorBrush(s.TopBarColor);
    }

    private void AddMessageBubble(ChatMessage m, ChatStyle style)
    {
        bool isUser = m.Role == "user";
        var row = new Grid
        {
            HorizontalAlignment = isUser ? HorizontalAlignment.Right : HorizontalAlignment.Left,
            MaxWidth = (this.Content as FrameworkElement)?.ActualWidth != null
                ? style.MaxWidthPct / 100.0 * 900 : 700,
        };

        var bubble = new Border
        {
            Background = new SolidColorBrush(isUser ? style.MyBubbleColor : style.AiBubbleColor),
            CornerRadius = new CornerRadius(style.Radius),
            Padding = new Thickness(12, 8, 12, 8),
            Margin = new Thickness(0, 2, 0, 2),
        };

        var stack = new StackPanel { Spacing = 4, MaxWidth = style.MaxWidthPct / 100.0 * 800 };

        if (m.HasReasoning)
        {
            var reasoning = new RichTextBlock
            {
                Foreground = new SolidColorBrush(Color.FromArgb(0xFF, 0x88, 0x88, 0x88)),
                FontSize = 12,
                IsTextSelectionEnabled = false,
            };
            MarkdownRender.Render(reasoning, "**思考过程**\n\n" + m.Reasoning);
            stack.Children.Add(reasoning);
        }

        var rich = new RichTextBlock
        {
            Foreground = new SolidColorBrush(isUser ? style.MyTextColor : style.AiTextColor),
            FontSize = style.FontSize,
            IsTextSelectionEnabled = true,
            TextWrapping = TextWrapping.Wrap,
        };
        MarkdownRender.Render(rich, m.Content);
        stack.Children.Add(rich);

        bubble.Child = stack;
        row.Children.Add(bubble);
        MsgPanel.Children.Add(row);
    }

    private async void ScrollToEnd()
    {
        await Task.Delay(50);
        MsgScroll.ChangeView(null, MsgScroll.ExtentHeight, null);
    }

    // ---------- ViewModel 属性变化 ----------
    private void Vm_PropertyChanged(object? sender, System.ComponentModel.PropertyChangedEventArgs e)
    {
        switch (e.PropertyName)
        {
            case nameof(MainViewModel.IsStreaming):
                StreamingBubble.Visibility = Vm.IsStreaming ? Visibility.Visible : Visibility.Collapsed;
                SendBtn.Visibility = Vm.IsBusy ? Visibility.Collapsed : Visibility.Visible;
                StopBtn.Visibility = Vm.IsBusy ? Visibility.Visible : Visibility.Collapsed;
                break;
            case nameof(MainViewModel.LiveContent):
                MarkdownRender.Render(LiveRich, Vm.LiveContent);
                MsgScroll.ChangeView(null, MsgScroll.ExtentHeight, null);
                break;
            case nameof(MainViewModel.LiveReasoning):
                if (!string.IsNullOrEmpty(Vm.LiveReasoning))
                {
                    ReasoningRich.Visibility = Visibility.Visible;
                    MarkdownRender.Render(ReasoningRich, "**思考**\n" + Vm.LiveReasoning);
                }
                break;
            case nameof(MainViewModel.CurrentConversation):
                ReloadMessages();
                break;
            case nameof(MainViewModel.StatusText):
                break;
        }
    }

    // ---------- 按钮事件 ----------
    private async void SendBtn_Click(object sender, RoutedEventArgs e)
    {
        await Vm.SendAsync();
        ReloadMessages();
        ReloadConversations();
        InputBox.Focus(FocusState.Programmatic);
    }

    private void StopBtn_Click(object sender, RoutedEventArgs e) => Vm.Stop();

    private void NewChatBtn_Click(object sender, RoutedEventArgs e)
    {
        Vm.NewConversation();
        ReloadConversations();
        ReloadMessages();
    }

    private async void InputBox_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == Windows.System.VirtualKey.Enter)
        {
            var shiftState = CoreWindow.GetForCurrentThread()
                .GetKeyState(Windows.System.VirtualKey.Shift);
            bool shiftDown = (shiftState & CoreVirtualKeyStates.Down) != 0;
            if (!shiftDown)
            {
                e.Handled = true;
                await SendBtn_Click(sender, e);
            }
        }
    }

    private void SettingsBtn_Click(object sender, RoutedEventArgs e)
    {
        SyncSettingsToUi();
        SettingsPanel.Visibility = Visibility.Visible;
    }

    private void CloseSettings_Click(object sender, RoutedEventArgs e)
    {
        SettingsPanel.Visibility = Visibility.Collapsed;
        Vm.Store.SaveSettings();
    }

    private void SettingsPanel_Tapped(object sender, Microsoft.UI.Xaml.Input.TappedRoutedEventArgs e)
    {
        SettingsPanel.Visibility = Visibility.Collapsed;
        Vm.Store.SaveSettings();
    }

    // ---------- 设置 ----------
    private void ReloadProviderCombo()
    {
        ProviderCombo.Items.Clear();
        foreach (var p in ModelPresets.All)
            ProviderCombo.Items.Add(new ComboBoxItem { Content = $"{p.Name}（{p.DefaultModel}）", Tag = p.Id });
    }

    private void SyncSettingsToUi()
    {
        _suppressSettingsEvents = true;
        var s = Vm.Store.Settings;
        for (int i = 0; i < ProviderCombo.Items.Count; i++)
            if ((string)((ComboBoxItem)ProviderCombo.Items[i]).Tag == s.ProviderId)
            { ProviderCombo.SelectedIndex = i; break; }
        ApiKeyBox.Password = s.ApiKey;
        ModelBox.Text = s.ModelName;
        BaseUrlBox.Text = s.BaseUrl;
        TempBox.Value = s.Temperature;
        CtxBox.Value = s.ContextCount;
        MaxTokBox.Value = s.MaxTokens;
        SysPromptBox.Text = s.SystemPrompt;
        _suppressSettingsEvents = false;
    }

    private void ProviderCombo_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (_suppressSettingsEvents || ProviderCombo.SelectedItem is not ComboBoxItem item) return;
        Vm.Store.Settings.ProviderId = (string)item.Tag;
        Vm.Store.SaveSettings();
    }
    private void ApiKeyBox_Changed(object sender, RoutedEventArgs e)
    { if (!_suppressSettingsEvents) { Vm.Store.Settings.ApiKey = ApiKeyBox.Password; Vm.Store.SaveSettings(); } }
    private void ModelBox_Changed(object sender, TextChangedEventArgs e)
    { if (!_suppressSettingsEvents) { Vm.Store.Settings.ModelName = ModelBox.Text; Vm.Store.SaveSettings(); } }
    private void BaseUrlBox_Changed(object sender, TextChangedEventArgs e)
    { if (!_suppressSettingsEvents) { Vm.Store.Settings.BaseUrl = BaseUrlBox.Text; Vm.Store.SaveSettings(); } }
    private void TempBox_Changed(object sender, NumberBoxValueChangedEventArgs e)
    { if (!_suppressSettingsEvents) Vm.Store.Settings.Temperature = TempBox.Value; }
    private void CtxBox_Changed(object sender, NumberBoxValueChangedEventArgs e)
    { if (!_suppressSettingsEvents) Vm.Store.Settings.ContextCount = (int)CtxBox.Value; }
    private void MaxTokBox_Changed(object sender, NumberBoxValueChangedEventArgs e)
    { if (!_suppressSettingsEvents) Vm.Store.Settings.MaxTokens = (int)MaxTokBox.Value; }
    private void SysPrompt_Changed(object sender, TextChangedEventArgs e)
    { if (!_suppressSettingsEvents) { Vm.Store.Settings.SystemPrompt = SysPromptBox.Text; Vm.Store.SaveSettings(); } }

    private async void TestBtn_Click(object sender, RoutedEventArgs e)
    {
        TestResult.Text = "测试中…";
        try
        {
            var ms = await net.LLMClient.TestConnectionAsync(
                Vm.Store.ResolvedProvider,
                Vm.Store.ResolvedBaseUrl,
                Vm.Store.Settings.ApiKey,
                Vm.Store.ResolvedModel);
            TestResult.Text = $"✓ 连接成功，延迟 {ms} ms";
        }
        catch (Exception ex)
        {
            TestResult.Text = "✗ " + ex.Message;
        }
    }

    // ---------- 风格切换 ----------
    private void StyleBtn_Click(object sender, RoutedEventArgs e)
    {
        if (Vm.CurrentConversation == null) return;
        var flyout = new MenuFlyout();
        foreach (var s in StylePresets.All)
        {
            var item = new MenuFlyoutItem { Text = s.Name };
            item.Click += (_, _) =>
            {
                Vm.Store.ApplyStyle(Vm.CurrentConversation.Id, s.Id);
                ReloadMessages();
            };
            flyout.Items.Add(item);
        }
        flyout.ShowAt(StyleBtnPlace(sender));
    }

    private FrameworkElement StyleBtnPlace(object sender) => (FrameworkElement)sender;
}
