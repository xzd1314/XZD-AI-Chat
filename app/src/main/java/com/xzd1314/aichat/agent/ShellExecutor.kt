package com.xzd1314.aichat.agent

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** Shell 执行结果 */
data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val mode: String  // "root" | "shizuku" | "normal"
)

/**
 * Shell 执行器：优先 Root（su/Magisk/KernelSU），其次 Shizuku，最后普通 shell。
 * 用于 Agent 模式控制手机执行命令。
 */
object ShellExecutor {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private val SU_PATHS = arrayOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/vendor/bin/su", "/data/local/tmp/su", "/su/bin/su",
        "/magisk/.core/bin/su"
    )

    /** 查找 su 可执行文件路径 */
    fun findSu(): String? {
        for (path in SU_PATHS) {
            if (java.io.File(path).exists()) return path
        }
        // 用 which 检测
        return try {
            val p = ProcessBuilder("sh", "-c", "which su 2>/dev/null").start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText().trim()
            p.waitFor(3, TimeUnit.SECONDS)
            if (out.isNotBlank() && java.io.File(out).exists()) out else null
        } catch (e: Exception) {
            null
        }
    }

    /** 检测是否有 Root 权限（兼容 Magisk / KernelSU / 传统 su） */
    fun hasRoot(): Boolean {
        val su = findSu() ?: return false
        return try {
            val p = ProcessBuilder(su, "-c", "id").redirectErrorStream(true).start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val finished = p.waitFor(8, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                return false
            }
            out.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    /** 检测 Magisk 是否安装 */
    fun hasMagisk(): Boolean {
        return try {
            val p = ProcessBuilder("sh", "-c", "magisk -v 2>/dev/null || ls /data/adb/magisk/ 2>/dev/null").start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            p.waitFor(3, TimeUnit.SECONDS)
            out.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /** 检测 KernelSU 是否安装 */
    fun hasKernelSU(): Boolean {
        return try {
            val p = ProcessBuilder("sh", "-c", "ksud -V 2>/dev/null || ls /data/adb/ksud 2>/dev/null || cat /proc/version 2>/dev/null | grep -i kernelsu").start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
            p.waitFor(3, TimeUnit.SECONDS)
            out.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    /** 检测 Shizuku 应用是否安装 */
    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** 检测 Shizuku 是否已授权（通过 Shizuku SDK） */
    fun isShizukuAuthorized(): Boolean {
        return try {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            // 先检查是否已连接
            val pingMethod = cls.getMethod("pingBinder")
            val connected = pingMethod.invoke(null) as? Boolean
            if (connected != true) return false
            // 再检查权限
            val checkMethod = cls.getMethod("checkSelfPermission")
            val perm = checkMethod.invoke(null) as? Int
            perm == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 注册 Shizuku Binder 连接监听器
     * 当 Shizuku 服务连接成功时回调（用户在 Shizuku 应用授权后会触发）
     * @return 是否注册成功
     */
    fun registerShizukuListener(onConnected: () -> Unit): Boolean {
        return try {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            val listenerCls = Class.forName("rikka.shizuku.Shizuku\$OnBinderReceivedListener")
            val listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerCls.classLoader,
                arrayOf(listenerCls)
            ) { _, _, _ ->
                onConnected()
                null
            }
            val addMethod = cls.getMethod("addBinderReceivedListener", listenerCls)
            addMethod.invoke(null, listener)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 请求 Shizuku 授权，并注册授权结果回调
     * @param onResult 授权结果回调（granted=true 表示已授权）
     */
    fun requestShizukuPermission(onResult: (Boolean) -> Unit): Boolean {
        return try {
            val cls = Class.forName("rikka.shizuku.Shizuku")
            // 注册授权结果监听器
            val resultListenerCls = Class.forName("rikka.shizuku.Shizuku\$OnPermissionGrantResultListener")
            val resultListener = java.lang.reflect.Proxy.newProxyInstance(
                resultListenerCls.classLoader,
                arrayOf(resultListenerCls)
            ) { _, method, args ->
                if (method.name == "onPermissionGranted") {
                    val granted = args?.getOrNull(0) as? Int == 0
                    onResult(granted)
                }
                null
            }
            val addResultMethod = cls.getMethod("addPermissionResultListener", resultListenerCls)
            addResultMethod.invoke(null, resultListener)

            // 检查是否已授权
            val checkMethod = cls.getMethod("checkSelfPermission")
            val perm = checkMethod.invoke(null) as? Int
            if (perm == 0) {
                onResult(true)
                return true
            }

            // 请求授权
            val reqMethod = cls.getMethod("requestPermission", Int::class.javaPrimitiveType)
            reqMethod.invoke(null, 1001)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 获取权限模式详情 */
    data class PermissionInfo(
        val mode: String,       // "root" | "shizuku" | "normal"
        val rootManager: String?, // "magisk" | "kernelsu" | "su" | null
        val shizukuInstalled: Boolean,
        val shizukuAuthorized: Boolean
    )

    fun getPermissionInfo(context: Context): PermissionInfo {
        val root = hasRoot()
        val magisk = hasMagisk()
        val ksu = hasKernelSU()
        val shizukuInstalled = isShizukuInstalled(context)
        val shizukuAuth = if (shizukuInstalled) isShizukuAuthorized() else false

        val mode = when {
            root -> "root"
            shizukuAuth -> "shizuku"
            else -> "normal"
        }
        val rootManager = when {
            magisk -> "magisk"
            ksu -> "kernelsu"
            root -> "su"
            else -> null
        }
        return PermissionInfo(mode, rootManager, shizukuInstalled, shizukuAuth)
    }

    /** 获取当前可用的最高权限模式 */
    fun currentMode(context: Context? = null): String {
        if (hasRoot()) return "root"
        if (isShizukuAuthorized()) return "shizuku"
        return "normal"
    }

    /** 执行 shell 命令，自动选择最高权限模式 */
    suspend fun execute(command: String, context: Context? = null): ShellResult = withContext(Dispatchers.IO) {
        val mode = currentMode(context)
        when (mode) {
            "root" -> execSu(command)
            "shizuku" -> execShizuku(command)
            else -> execNormal(command)
        }
    }

    /**
     * 脚本模式：按顺序执行多条命令
     * 命令用换行或 ; 分隔，支持 # 注释行
     * @return 每条命令的执行结果列表
     */
    suspend fun executeScript(script: String, context: Context? = null): List<ShellResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ShellResult>()
        val lines = script.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
        // 拆分 ; 分隔的命令
        val commands = mutableListOf<String>()
        lines.forEach { line ->
            line.split(";").map { it.trim() }.filter { it.isNotBlank() }.forEach { commands.add(it) }
        }
        for (cmd in commands) {
            val result = execute(cmd, context)
            results.add(result)
            // 如果命令失败且不是最后一条，继续执行（脚本模式不中断）
        }
        results
    }

    /** 从命令输出中提取文件路径（用于文件抓取） */
    fun extractFilePaths(output: String): List<String> {
        val paths = mutableSetOf<String>()
        // 匹配绝对路径 /xxx/yyy.zzz
        val regex = Regex("""/(?:[\w.\-]+/)+[\w.\-]+""")
        regex.findAll(output).forEach { match ->
            val path = match.value
            // 过滤常见非文件路径
            if (!path.startsWith("/proc") && !path.startsWith("/sys") &&
                !path.startsWith("/dev") && path.length > 5) {
                paths.add(path)
            }
        }
        return paths.toList()
    }

    /** 检查文件是否存在且可读 */
    fun fileExists(path: String): Boolean {
        return try {
            java.io.File(path).exists() && java.io.File(path).canRead()
        } catch (e: Exception) {
            false
        }
    }

    private fun execSu(command: String): ShellResult {
        val su = findSu() ?: return ShellResult("", "su 未找到", -1, "root")
        return try {
            val p = ProcessBuilder(su, "-c", command).redirectErrorStream(false).start()
            val stdout = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(p.errorStream)).readText()
            val finished = p.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                ShellResult(stdout, "命令执行超时（30秒）", -1, "root")
            } else {
                ShellResult(stdout, stderr, p.exitValue(), "root")
            }
        } catch (e: Exception) {
            ShellResult("", e.message ?: "执行失败", -1, "root")
        }
    }

    private fun execShizuku(command: String): ShellResult {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getMethod("newProcess", Array<String>::class.java)
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command)) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                ShellResult(stdout, "命令执行超时", -1, "shizuku")
            } else {
                ShellResult(stdout, stderr, process.exitValue(), "shizuku")
            }
        } catch (e: Exception) {
            execNormal(command).copy(mode = "shizuku(fallback)")
        }
    }

    private fun execNormal(command: String): ShellResult {
        return try {
            val p = ProcessBuilder("sh", "-c", command).redirectErrorStream(false).start()
            val stdout = BufferedReader(InputStreamReader(p.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(p.errorStream)).readText()
            val finished = p.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                ShellResult(stdout, "命令执行超时", -1, "normal")
            } else {
                ShellResult(stdout, stderr, p.exitValue(), "normal")
            }
        } catch (e: Exception) {
            ShellResult("", e.message ?: "执行失败", -1, "normal")
        }
    }
}
