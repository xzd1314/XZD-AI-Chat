package com.xzd1314.aichat.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 运行时安全检测：签名校验、调试器检测、Frida/Xposed 注入检测
 * 用于防止重打包、动态调试和注入攻击
 */
object SecurityGuard {

    // 预期签名 SHA-256（正式签名证书）
    private const val EXPECTED_SIGNATURE_SHA256 =
        "84b4110681f3f0301d08edb20305ed12fa0daf6704ad98a0d4ecc0d5e25c9020"

    /** 检测应用签名是否被篡改（重打包检测） */
    fun isSignatureValid(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val sig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()
            }
            if (sig == null) return false
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex.equals(EXPECTED_SIGNATURE_SHA256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /** 检测是否连接了调试器 */
    fun isDebuggerConnected(): Boolean {
        return try {
            Debug.isDebuggerConnected()
        } catch (e: Exception) {
            false
        }
    }

    /** 检测是否处于可调试状态（android:debuggable=true） */
    fun isDebuggable(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    /** 检测 Frida 注入（扫描 /proc/self/maps） */
    fun hasFrida(): Boolean {
        return try {
            val maps = File("/proc/self/maps")
            if (!maps.exists()) return false
            val reader = BufferedReader(InputStreamReader(maps.inputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line!!.lowercase()
                if (l.contains("frida") || l.contains("gum-js-loop") ||
                    l.contains("gmain") || l.contains("linjector") ||
                    l.contains("frida-agent") || l.contains("frida-gadget")) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /** 检测 Xposed 框架 */
    fun hasXposed(): Boolean {
        return try {
            Class.forName("de.robv.android.xposed.XposedHelpers")
            true
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName("de.robv.android.xposed.XposedBridge")
                true
            } catch (e2: ClassNotFoundException) {
                false
            }
        }
    }

    /** 检测模拟器 */
    fun isEmulator(): Boolean {
        return try {
            val fingerprint = android.os.Build.FINGERPRINT.lowercase()
            val model = android.os.Build.MODEL.lowercase()
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()
            fingerprint.contains("generic") || fingerprint.contains("sdk_gphone") ||
                model.contains("emulator") || model.contains("android sdk") ||
                manufacturer.contains("genymotion") ||
                android.os.Build.PRODUCT.lowercase().contains("sdk") ||
                android.os.Build.HARDWARE.lowercase().contains("ranchu") ||
                android.os.Build.HARDWARE.lowercase().contains("goldfish")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 综合安全检测
     * @return 检测到的威胁列表（空列表=安全）
     */
    fun checkAll(context: Context): List<String> {
        val threats = mutableListOf<String>()
        if (!isSignatureValid(context)) threats.add("SIGNATURE_TAMPERED")
        if (isDebuggerConnected()) threats.add("DEBUGGER_CONNECTED")
        if (isDebuggable(context)) threats.add("DEBUGGABLE_FLAG")
        if (hasFrida()) threats.add("FRIDA_INJECTED")
        if (hasXposed()) threats.add("XPOSED_DETECTED")
        return threats
    }

    /**
     * 启动时安全检查，如果检测到严重威胁则抛出异常终止应用
     */
    fun enforceAtStartup(context: Context) {
        val threats = checkAll(context)
        // 签名被篡改 = 直接终止
        if (threats.contains("SIGNATURE_TAMPERED")) {
            throw SecurityException("应用签名校验失败，可能已被重打包")
        }
        // 调试器连接 + Frida = 终止
        if (threats.contains("DEBUGGER_CONNECTED") && threats.contains("FRIDA_INJECTED")) {
            throw SecurityException("检测到调试器和注入工具，应用终止")
        }
    }
}
