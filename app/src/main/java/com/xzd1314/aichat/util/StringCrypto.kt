package com.xzd1314.aichat.util

/**
 * XOR 字符串加密（与 StringFog XOR 模式同原理）
 * 密钥内嵌，运行时解密，DEX 中无明文
 * 比 Base64 强：需要知道密钥才能解密
 */
object StringCrypto {

    // XOR 密钥（随机字节数组，逆向者需要先找到这个密钥）
    private val KEY = byteArrayOf(
        0x7A, 0x64, 0x31, 0x33, 0x31, 0x34, 0x58, 0x5A,
        0x44, 0x20, 0x41, 0x49, 0x20, 0x43, 0x68, 0x61,
        0x74, 0x20, 0x50, 0x72, 0x6F, 0x74, 0x65, 0x63,
        0x74, 0x65, 0x64, 0x20, 0x62, 0x79, 0x20, 0x78
    )

    /** 加密字符串为 IntArray（每个字符与密钥 XOR） */
    fun encrypt(plain: String): IntArray {
        val chars = plain.toCharArray()
        val result = IntArray(chars.size)
        for (i in chars.indices) {
            result[i] = chars[i].code xor (KEY[i % KEY.size].toInt() and 0xFF)
        }
        return result
    }

    /** 解密 IntArray 为字符串 */
    fun decrypt(encrypted: IntArray): String {
        val chars = CharArray(encrypted.size)
        for (i in encrypted.indices) {
            chars[i] = (encrypted[i] xor (KEY[i % KEY.size].toInt() and 0xFF)).toChar()
        }
        return String(chars)
    }

    /** 便捷方法：加密字符串为逗号分隔的 Int 字符串（用于代码中内嵌） */
    fun enc(plain: String): String = encrypt(plain).joinToString(",")
}
