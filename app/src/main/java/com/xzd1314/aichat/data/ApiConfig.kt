package com.xzd1314.aichat.data

/**
 * API 端点常量集中管理
 * 字符串由 StringFog 在编译期自动 XOR 加密（bytes 模式），
 * DEX 字符串池中不出现明文 URL，运行时按需解密。
 */
object ApiConfig {

    // 各供应商 chat/completions 端点（StringFog 编译期加密）
    val DEEPSEEK = "https://api.deepseek.com/chat/completions"
    val GLM = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    val KIMI = "https://api.moonshot.cn/v1/chat/completions"
    val QWEN = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    val DOUBAO = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
    val WENXIN = "https://qianfan.baidubce.com/v2/chat/completions"
    val XINGHUO = "https://spark-api-open.xf-yun.com/v1/chat/completions"
    val HUNYUAN = "https://api.hunyuan.cloud.tencent.com/v1/chat/completions"
    val BAICHUAN = "https://api.baichuan-ai.com/v1/chat/completions"
    val LINGYI = "https://api.lingyiwanwu.com/v1/chat/completions"
    val MINIMAX = "https://api.minimax.chat/v1/text/chatcompletion_v2"
    val STEPFUN = "https://api.stepfun.com/v1/chat/completions"
    val YI = "https://api.wodelab.com/v1/chat/completions"

    val DEV_SITE = "https://xzd1314.top"
    val DEFAULT_MODEL = "deepseek-chat"
}
