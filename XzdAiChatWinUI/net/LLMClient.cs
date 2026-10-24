using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using XzdAiChatWinUI.data;

namespace XzdAiChatWinUI.net;

public static class LLMClient
{
    private static readonly HttpClient Http = CreateHttpClient();

    private static HttpClient CreateHttpClient()
    {
        var h = new HttpClient(new SocketsHttpHandler
        {
            PooledConnectionLifetime = TimeSpan.FromMinutes(5),
        })
        {
            Timeout = TimeSpan.FromMinutes(3),
        };
        return h;
    }

    public static List<object> BuildRequestMessages(string systemPrompt, List<ChatMessage> history, int maxCount)
    {
        var trimmed = history.Count > maxCount ? history.GetRange(history.Count - maxCount, maxCount) : history;
        var list = new List<object> { new { role = "system", content = systemPrompt } };
        foreach (var m in trimmed) list.Add(new { role = m.Role, content = m.Content });
        return list;
    }

    /// <summary>
    /// 流式聊天。onContent(chunk, reasoningChunk) 被增量回调。
    /// 返回 (content全量, reasoning全量)。
    /// </summary>
    public static async Task<(string content, string reasoning)> ChatStreamAsync(
        ModelPreset preset, string baseUrl, string apiKey, string model,
        List<object> messages, double temperature, int maxTokens, string? reasoningEffort,
        Action<string, string> onChunk, CancellationToken ct)
    {
        var url = string.IsNullOrWhiteSpace(baseUrl) ? preset.BaseUrl : baseUrl;

        var payload = new
        {
            model,
            messages,
            temperature,
            max_tokens = maxTokens,
            stream = true,
            reasoning_effort = reasoningEffort,
        };

        using var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
        req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("text/event-stream"));
        if (!string.IsNullOrWhiteSpace(apiKey))
            req.Headers.TryAddWithoutValidation(preset.AuthHeader, preset.AuthScheme + apiKey);

        using var resp = await Http.SendAsync(req, HttpCompletionOption.ResponseHeadersRead, ct);
        var body = await resp.Content.ReadAsStringAsync(ct);
        if (!resp.IsSuccessStatusCode)
        {
            throw new Exception(FriendlyError((int)resp.StatusCode, body));
        }

        var full = new StringBuilder();
        var reasoning = new StringBuilder();

        using var stream = await resp.Content.ReadAsStreamAsync(ct);
        using var reader = new System.IO.StreamReader(stream, Encoding.UTF8);
        string? line;
        while ((line = await reader.ReadLineAsync(ct)) != null)
        {
            if (!line.StartsWith("data:")) continue;
            var data = line.Substring(5).Trim();
            if (data == "[DONE]") break;
            if (string.IsNullOrEmpty(data)) continue;

            try
            {
                using var doc = JsonDocument.Parse(data);
                var root = doc.RootElement;
                if (!root.TryGetProperty("choices", out var choices) || choices.GetArrayLength() == 0) continue;
                var delta = choices[0].GetProperty("delta");

                string? contentDelta = null;
                string? reasoningDelta = null;
                if (delta.TryGetProperty("content", out var cEl) && cEl.ValueKind == JsonValueKind.String)
                    contentDelta = cEl.GetString();
                if (delta.TryGetProperty("reasoning_content", out var rEl) && rEl.ValueKind == JsonValueKind.String)
                    reasoningDelta = rEl.GetString();

                if (!string.IsNullOrEmpty(contentDelta))
                {
                    full.Append(contentDelta);
                    onChunk(contentDelta, "");
                }
                if (!string.IsNullOrEmpty(reasoningDelta))
                {
                    reasoning.Append(reasoningDelta);
                    onChunk("", reasoningDelta);
                }
            }
            catch (JsonException) { /* 跳过坏行 */ }
        }

        if (full.Length == 0)
            throw new Exception("模型返回了空内容，请检查模型名是否正确。");

        return (full.ToString(), reasoning.ToString());
    }

    public static async Task<long> TestConnectionAsync(ModelPreset preset, string baseUrl, string apiKey, string model)
    {
        var url = string.IsNullOrWhiteSpace(baseUrl) ? preset.BaseUrl : baseUrl;
        var sw = StopwatchStart();
        var payload = new
        {
            model,
            messages = new[] { new { role = "user", content = "你好，请回复：ok" } },
            max_tokens = 8,
            stream = false,
        };
        using var req = new HttpRequestMessage(HttpMethod.Post, url);
        req.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");
        if (!string.IsNullOrWhiteSpace(apiKey))
            req.Headers.TryAddWithoutValidation(preset.AuthHeader, preset.AuthScheme + apiKey);

        using var resp = await Http.SendAsync(req, HttpCompletionOption.ResponseContentRead);
        var body = await resp.Content.ReadAsStringAsync();
        if (!resp.IsSuccessStatusCode)
            throw new Exception(FriendlyError((int)resp.StatusCode, body));
        return sw.ElapsedMilliseconds;
    }

    private static System.Diagnostics.Stopwatch StopwatchStart()
    { var s = System.Diagnostics.Stopwatch.StartNew(); return s; }

    private static string FriendlyError(int code, string body)
    {
        string hint = code switch
        {
            401 => "API Key 无效或未填写（401）。",
            403 => "无权限访问该模型（403）。",
            404 => "接口或模型不存在，请检查 Base URL / 模型名（404）。",
            429 => "请求过于频繁或额度不足（429）。",
            400 => "请求参数有误，请检查模型名（400）。",
            500 or 502 or 503 => $"服务端繁忙或异常（{code}）。",
            _ => $"请求失败（{code}）。",
        };
        var detail = body.Length > 300 ? body.Substring(0, 300) : body;
        detail = detail.Replace("\n", " ");
        return string.IsNullOrEmpty(detail) ? hint : $"{hint} 详情：{detail}";
    }
}
