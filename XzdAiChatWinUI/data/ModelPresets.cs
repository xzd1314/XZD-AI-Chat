using System.Collections.Generic;
using System.Linq;

namespace XzdAiChatWinUI.data;

public static class ModelPresets
{
    public static readonly List<ModelPreset> All = new()
    {
        new("deepseek",   "DeepSeek 深度求索",   "https://api.deepseek.com/chat/completions",                                    "deepseek-chat",         "Authorization", "Bearer ", "开源性价比之王，deepseek-reasoner 支持推理"),
        new("glm",        "智谱 GLM",            "https://open.bigmodel.cn/api/paas/v4/chat/completions",                         "glm-4-plus",            "Authorization", "Bearer ", "清华系 GLM-4 系列，glm-4-flash 有免费额度"),
        new("kimi",       "Kimi 月之暗面",       "https://api.moonshot.cn/v1/chat/completions",                                   "moonshot-v1-8k",        "Authorization", "Bearer ", "长上下文强项，moonshot-v1-32k/128k 可选"),
        new("qwen",       "通义千问 阿里云",     "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",            "qwen-plus",             "Authorization", "Bearer ", "DashScope 兼容模式，qwen-turbo/qwen-max 可选"),
        new("doubao",     "豆包 火山方舟",       "https://ark.cn-beijing.volces.com/api/v3/chat/completions",                    "doubao-1.5-pro-32k",    "Authorization", "Bearer ", "模型 ID 填接入点 ep-xxx 或模型名"),
        new("ernie",      "百度文心",            "https://qianfan.baidubce.com/v2/chat/completions",                              "ernie-4.0-8k",          "Authorization", "Bearer ", "千帆 v2 OpenAI 兼容接口"),
        new("spark",      "讯飞星火",            "https://spark-api-open.xf-yun.com/v1/chat/completions",                        "generalv3.5",            "Authorization", "Bearer ", "API Key 格式为 appId:apiSecret"),
        new("hunyuan",    "腾讯混元",            "https://api.hunyuan.cloud.tencent.com/v1/chat/completions",                    "hunyuan-turbo",         "Authorization", "Bearer ", "混元 OpenAI 兼容接口"),
        new("baichuan",   "百川智能",            "https://api.baichuan-ai.com/v1/chat/completions",                               "Baichuan3-Turbo",       "Authorization", "Bearer ", "Baichuan 系列"),
        new("lingyi",     "零一万物 Yi",         "https://api.lingyiwanwu.com/v1/chat/completions",                              "yi-lightning",           "Authorization", "Bearer ", "Yi-Lightning 速度快性价比高"),
        new("minimax",    "MiniMax",             "https://api.minimax.chat/v1/text/chatcompletion_v2",                            "MiniMax-M3",             "Authorization", "Bearer ", "MiniMax M3/M3.1 系列"),
        new("step",       "阶跃星辰 Step",       "https://api.stepfun.com/v1/chat/completions",                                   "step-1-8k",             "Authorization", "Bearer ", "Step-1/2 系列"),
        new("ollama",     "Ollama 本地",         "http://localhost:11434/v1/chat/completions",                                   "llama3.1",              "Authorization", "Bearer ", "本地 Ollama，无需 Key"),
        new("custom",     "自定义 OpenAI 兼容",  "",                                                                              "gpt-4o-mini",           "Authorization", "Bearer ", "任意 OpenAI 格式端点（OpenRouter/硅基流动/vLLM 等）"),
    };

    public static ModelPreset Get(string id) => All.FirstOrDefault(p => p.Id == id) ?? All[^1];
}
