package com.xzd1314.aichat.data

import com.xzd1314.aichat.util.StringCrypto

/** 国内大模型供应商预设（OpenAI 兼容端点）
 *  所有端点 URL 以 XOR 加密存储（与 StringFog XOR 模式同原理），
 *  密钥内嵌于 StringCrypto，DEX 字符串池中无明文 */
object ModelPresets {

    private fun x(encrypted: String): String =
        StringCrypto.decrypt(encrypted.split(",").map { it.toInt() }.toIntArray())

    val all: List<ModelPreset> = listOf(
        ModelPreset("deepseek", "DeepSeek 深度求索", x("18,16,69,67,66,14,119,117,37,80,40,103,68,38,13,17,7,69,53,25,65,23,10,14,91,6,12,65,22,86,67,23,23,20,93,86,69,93,55,52,55"), "deepseek-chat", "Authorization", "Bearer ", "开源性价比之王，上下文缓存自动生效，deepseek-reasoner 支持推理"),
        ModelPreset("glm", "智谱 GLM", x("18,16,69,67,66,14,119,117,43,80,36,39,14,33,1,6,25,79,52,23,3,90,6,13,91,4,20,73,77,9,65,25,9,75,71,7,30,87,48,59,48,15,34,38,77,51,4,4,0,73,63,28,28"), "glm-4-plus", "Authorization", "Bearer ", "清华系 GLM-4 系列，glm-4-flash 有免费额度"),
        ModelPreset("kimi", "Kimi 月之暗面", x("18,16,69,67,66,14,119,117,37,80,40,103,77,44,7,15,7,72,63,6,65,23,11,76,2,84,75,67,10,24,84,87,25,11,92,67,93,81,44,51,43,78,50"), "moonshot-v1-8k", "Authorization", "Bearer ", "长上下文强项，moonshot-v1-32k/128k 可选"),
        ModelPreset("qwen", "通义千问 阿里云", x("18,16,69,67,66,14,119,117,32,65,50,33,83,32,7,17,17,14,49,30,6,13,16,13,23,22,74,67,13,20,15,27,21,9,65,82,69,93,58,54,33,13,44,38,68,38,71,23,69,15,51,26,14,0,74,0,27,8,20,76,7,13,73,23,20,23"), "qwen-plus", "Authorization", "Bearer ", "DashScope 兼容模式，qwen-turbo/qwen-max 可选"),
        ModelPreset("doubao", "豆包 火山方舟", x("18,16,69,67,66,14,119,117,37,82,42,103,67,45,69,3,17,73,58,27,1,19,75,21,27,9,7,69,17,87,67,23,23,75,80,67,88,27,46,105,107,67,41,40,84,108,11,14,25,80,60,23,27,29,10,13,7"), "doubao-1.5-pro-32k", "Authorization", "Bearer ", "模型 ID 填接入点 ep-xxx 或模型名，同门 AI 质量稳"),
        ModelPreset("ernie", "百度文心", x("18,16,69,67,66,14,119,117,53,73,32,39,70,34,6,79,22,65,57,22,26,22,6,6,90,6,11,77,77,15,18,87,25,12,80,71,30,87,55,55,52,76,36,61,73,44,6,18"), "ernie-4.0-8k", "Authorization", "Bearer ", "ERNIE 4.0/3.5 系列，国内中文理解领先"),
        ModelPreset("spark", "讯飞星火", x("18,16,69,67,66,14,119,117,55,80,32,59,75,110,9,17,29,13,63,2,10,26,75,27,18,72,29,85,12,87,67,23,23,75,71,2,30,87,48,59,48,15,34,38,77,51,4,4,0,73,63,28,28"), "generalv3.5", "Authorization", "Bearer ", "Spark 通用版，需在讯飞开放平台开通"),
        ModelPreset("hunyuan", "腾讯混元", x("18,16,69,67,66,14,119,117,37,80,40,103,72,54,6,24,1,65,62,92,12,24,10,22,16,75,16,69,12,26,69,22,14,74,82,92,92,27,46,107,107,67,41,40,84,108,11,14,25,80,60,23,27,29,10,13,7"), "hunyuan-turbo", "Authorization", "Bearer ", "腾讯混元大模型，turbo/standard 可选"),
        ModelPreset("baichuan", "百川智能", x("18,16,69,67,66,14,119,117,37,80,40,103,66,34,1,2,28,85,49,28,66,21,12,77,23,10,9,15,20,72,15,27,18,5,69,28,82,91,53,42,40,69,53,32,79,45,27"), "Baichuan3-Turbo", "Authorization", "Bearer ", "Baichuan 系列，需在百川开放平台申请"),
        ModelPreset("yi", "零一万物 Yi", x("18,16,69,67,66,14,119,117,37,80,40,103,76,42,6,6,13,73,39,19,1,3,16,77,23,10,9,15,20,72,15,27,18,5,69,28,82,91,53,42,40,69,53,32,79,45,27"), "yi-lightning", "Authorization", "Bearer ", "Yi-Lightning 速度快性价比高"),
        ModelPreset("minimax", "MiniMax", x("18,16,69,67,66,14,119,117,37,80,40,103,77,42,6,8,25,65,40,92,12,28,4,23,91,19,85,15,22,28,88,12,85,7,89,82,69,87,55,55,52,76,36,61,73,44,6,62,2,18"), "abab6.5s-chat", "Authorization", "Bearer ", "abab 系列，API 需在 MiniMax 平台申请"),
        ModelPreset("step", "阶跃星辰 Step", x("18,16,69,67,66,14,119,117,37,80,40,103,83,55,13,17,18,85,62,92,12,27,8,76,2,84,75,67,10,24,84,87,25,11,92,67,93,81,44,51,43,78,50"), "step-1-8k", "Authorization", "Bearer ", "Step-1/2 系列，国内头部创业模型"),
        ModelPreset("custom", "自定义 OpenAI 兼容", "", "gpt-4o-mini", "Authorization", "Bearer ", "任意 OpenAI 格式端点（OpenRouter/硅基流动/本地 Ollama 等），URL 形如 http://host/v1/chat/completions")
    )

    fun get(id: String): ModelPreset = all.firstOrNull { it.id == id } ?: all.last()
}
