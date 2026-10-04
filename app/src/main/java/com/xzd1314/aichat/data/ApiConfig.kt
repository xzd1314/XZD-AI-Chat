package com.xzd1314.aichat.data

import android.util.Base64

/**
 * API 端点常量集中管理
 * 所有 URL 以 Base64 编码存储，DEX 字符串池中不出现明文，
 * 配合 R8 混淆后逆向者难以定位端点。
 */
object ApiConfig {

    private fun dec(encoded: String): String =
        String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)

    // 各供应商 chat/completions 端点（Base64 编码）
    val DEEPSEEK: String get() = dec("aHR0cHM6Ly9hcGkuZGVlcHNlZWsuY29tL2NoYXQvY29tcGxldGlvbnM=")
    val GLM: String get() = dec("aHR0cHM6Ly9vcGVuLmJpZ21vZGVsLmNuL2FwaS9wYWFzL3Y0L2NoYXQvY29tcGxldGlvbnM=")
    val KIMI: String get() = dec("aHR0cHM6Ly9hcGkubW9vbnNob3QuY24vdjEvY2hhdC9jb21wbGV0aW9ucw==")
    val QWEN: String get() = dec("aHR0cHM6Ly9kYXNoc2NvcGUuYWxpeXVuY3MuY29tL2NvbXBhdGlibGUtbW9kZS92MS9jaGF0L2NvbXBsZXRpb25z")
    val DOUBAO: String get() = dec("aHR0cHM6Ly9hcmsuY24tYmVpamluZy52b2xjZXMuY29tL2FwaS92My9jaGF0L2NvbXBsZXRpb25z")
    val WENXIN: String get() = dec("aHR0cHM6Ly9xaWFuZmFuLmJhaWR1YmNlLmNvbS92Mi9jaGF0L2NvbXBsZXRpb25z")
    val XINGHUO: String get() = dec("aHR0cHM6Ly9zcGFyay1hcGktb3Blbi54Zi15dW4uY29tL3YxL2NoYXQvY29tcGxldGlvbnM=")
    val HUNYUAN: String get() = dec("aHR0cHM6Ly9hcGkuaHVueXVhbi5jbG91ZC50ZW5jZW50LmNvbS92MS9jaGF0L2NvbXBsZXRpb25z")
    val BAICHUAN: String get() = dec("aHR0cHM6Ly9hcGkuYmFpY2h1YW4tYWkuY29tL3YxL2NoYXQvY29tcGxldGlvbnM=")
    val LINGYI: String get() = dec("aHR0cHM6Ly9hcGkubGluZ3lpd2Fud3UuY29tL3YxL2NoYXQvY29tcGxldGlvbnM=")
    val MINIMAX: String get() = dec("aHR0cHM6Ly9hcGkubWluaW1heC5jaGF0L3YxL3RleHQvY2hhdGNvbXBsZXRpb25fdjI=")
    val STEPFUN: String get() = dec("aHR0cHM6Ly9hcGkuc3RlcGZ1bi5jb20vdjEvY2hhdC9jb21wbGV0aW9ucw==")
    val YI: String get() = dec("aHR0cHM6Ly9hcGkud29uZGFsYWIuY29tL3YxL2NoYXQvY29tcGxldGlvbnM=")

    val DEV_SITE: String get() = dec("aHR0cHM6Ly94emQxMzE0LnRvcA==")
    val DEFAULT_MODEL: String get() = "deepseek-chat"
}
