using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.UI.Text;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Documents;
using Microsoft.UI.Xaml.Media;
using Windows.UI;
using Windows.UI.Text;

namespace XzdAiChatWinUI.ui;

/// <summary>
/// 极简 Markdown → RichTextBlock 渲染器。
/// 支持：代码块 ```、行内代码 `、**粗体**、标题 #/##/###、无序列表 -。
/// </summary>
public static class MarkdownRender
{
    public static void Render(RichTextBlock rich, string markdown)
    {
        rich.Blocks.Clear();
        if (string.IsNullOrEmpty(markdown)) return;

        var lines = markdown.Replace("\r\n", "\n").Split('\n');
        var inCode = false;
        var codeBuf = new StringBuilder();
        var paraBuf = new StringBuilder();

        void FlushPara()
        {
            if (paraBuf.Length == 0) return;
            var p = new Paragraph();
            foreach (var il in ParseInline(paraBuf.ToString())) p.Inlines.Add(il);
            rich.Blocks.Add(p);
            paraBuf.Clear();
        }

        void FlushCode()
        {
            if (codeBuf.Length == 0) return;
            var border = new Border
            {
                Background = new SolidColorBrush(Color.FromArgb(0xFF, 0x2E, 0x2E, 0x2E)),
                CornerRadius = new CornerRadius(6),
                Padding = new Thickness(10, 8, 10, 8),
                Child = new TextBlock
                {
                    Text = codeBuf.ToString().TrimEnd(),
                    FontFamily = new FontFamily("Consolas, Cascadia Code, monospace"),
                    FontSize = 13,
                    Foreground = new SolidColorBrush(Color.FromArgb(0xFF, 0xE6, 0xE6, 0xE6)),
                    TextWrapping = TextWrapping.Wrap,
                }
            };
            var p = new Paragraph();
            p.Inlines.Add(new InlineUIContainer { Child = border });
            rich.Blocks.Add(p);
            codeBuf.Clear();
        }

        foreach (var raw in lines)
        {
            var line = raw;
            if (line.TrimStart().StartsWith("```"))
            {
                if (inCode) { FlushCode(); inCode = false; }
                else { FlushPara(); inCode = true; }
                continue;
            }
            if (inCode) { codeBuf.AppendLine(line); continue; }

            if (string.IsNullOrWhiteSpace(line)) { FlushPara(); continue; }

            if (line.StartsWith("### ")) { FlushPara(); AddHeading(rich, line.Substring(4), 15, FontWeights.SemiBold); continue; }
            if (line.StartsWith("## "))  { FlushPara(); AddHeading(rich, line.Substring(3), 17, FontWeights.SemiBold); continue; }
            if (line.StartsWith("# "))   { FlushPara(); AddHeading(rich, line.Substring(2), 19, FontWeights.Bold); continue; }

            if (line.TrimStart().StartsWith("- ") || line.TrimStart().StartsWith("* "))
            {
                FlushPara();
                var p = new Paragraph();
                p.Inlines.Add(new Run { Text = "  •  ", Foreground = new SolidColorBrush(Color.FromArgb(0xFF, 0x88, 0x88, 0x88)) });
                var rest = line.TrimStart().Substring(2);
                foreach (var il in ParseInline(rest)) p.Inlines.Add(il);
                rich.Blocks.Add(p);
                continue;
            }

            paraBuf.AppendLine(line);
        }
        FlushPara();
        if (inCode) FlushCode();
    }

    private static void AddHeading(RichTextBlock rich, string text, double size, FontWeight weight)
    {
        var p = new Paragraph();
        p.Inlines.Add(new Run { Text = text, FontSize = size, FontWeight = weight });
        rich.Blocks.Add(p);
    }

    private static List<Inline> ParseInline(string text)
    {
        var list = new List<Inline>();
        int i = 0;
        var buf = new StringBuilder();

        void PushBuf()
        {
            if (buf.Length > 0) { list.Add(new Run { Text = buf.ToString() }); buf.Clear(); }
        }

        while (i < text.Length)
        {
            if (i + 1 < text.Length && text[i] == '`')
            {
                int end = text.IndexOf('`', i + 1);
                if (end > i)
                {
                    PushBuf();
                    list.Add(new Run
                    {
                        Text = text.Substring(i + 1, end - i - 1),
                        FontFamily = new FontFamily("Consolas, monospace"),
                        FontSize = 13,
                    });
                    i = end + 1;
                    continue;
                }
            }
            if (i + 1 < text.Length && text[i] == '*' && text[i + 1] == '*')
            {
                int end = text.IndexOf("**", i + 2, StringComparison.Ordinal);
                if (end > i)
                {
                    PushBuf();
                    list.Add(new Run
                    {
                        Text = text.Substring(i + 2, end - i - 2),
                        FontWeight = FontWeights.Bold,
                    });
                    i = end + 2;
                    continue;
                }
            }
            buf.Append(text[i]);
            i++;
        }
        PushBuf();
        return list;
    }
}
