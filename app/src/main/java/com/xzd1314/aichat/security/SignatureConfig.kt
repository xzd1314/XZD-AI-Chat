package com.xzd1314.aichat.security

/**
 * 签名校验配置
 *
 * 官方发布版本使用 xzd-release.jks 签名。
 * 自构建版本请将 EXPECTED_SIGNATURE_SHA256 设为空字符串 ""，即可跳过签名校验。
 * 或替换为你自己的签名证书 SHA-256。
 */
object SignatureConfig {
    // 官方签名 SHA-256（开源版本默认为空，跳过校验；官方发布版会注入真实签名）
    const val EXPECTED_SIGNATURE_SHA256 = ""
}
