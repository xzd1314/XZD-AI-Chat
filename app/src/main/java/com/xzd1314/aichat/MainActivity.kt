package com.xzd1314.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xzd1314.aichat.ui.screens.AboutScreen
import com.xzd1314.aichat.ui.screens.AgentChatScreen
import com.xzd1314.aichat.ui.screens.AgentListScreen
import com.xzd1314.aichat.ui.screens.ChatScreen
import com.xzd1314.aichat.ui.screens.ChatsScreen
import com.xzd1314.aichat.ui.screens.SettingsScreen
import com.xzd1314.aichat.ui.screens.StyleEditorScreen
import com.xzd1314.aichat.ui.theme.XZDChatTheme

class MainActivity : ComponentActivity() {
    @Volatile private var tamperTriggered = false
    @Volatile private var externalSeed = 0L

    private fun xordec(encrypted: String): String =
        com.xzd1314.aichat.util.StringCrypto.decrypt(
            encrypted.split(",").map { it.toInt() }.toIntArray()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        // ===== 运行时防篡改/反调试（XOR加密+反射，R8无法静态分析目标） =====
        externalSeed = System.currentTimeMillis()
        run {
            // 1. 签名校验（反射调用 getPackageInfo + signatures 字段）
            try {
                val pm = packageManager
                val getPkgInfoMethod = pm.javaClass.getMethod(xordec("29,1,69,99,80,87,51,59,35,69,8,39,70,44"), String::class.java, Int::class.javaPrimitiveType)
                val pi = getPkgInfoMethod.invoke(pm, packageName, 64)
                val sigsField = pi.javaClass.getField(xordec("9,13,86,93,80,64,45,40,33,83"))
                val sigs = sigsField.get(pi) as? Array<*>
                val sig = sigs?.firstOrNull()
                if (sig != null) {
                    val toBytesMethod = sig.javaClass.getMethod(xordec("14,11,115,74,69,81,25,40,54,65,56"))
                    val bytes = toBytesMethod.invoke(sig) as ByteArray
                    val md = java.security.MessageDigest.getInstance(xordec("41,44,112,30,3,1,110"))
                    val hex = md.digest(bytes).joinToString("") { "%02x".format(it) }
                    val expected = "84b4110681f3f0301d08edb20305ed12fa0daf6704ad98a0d4ecc0d5e25c9020"
                    if (!hex.equals(expected, ignoreCase = true)) tamperTriggered = true
                }
            } catch (_: Exception) {}

            // 2. 调试器检测（反射调用 Debug.isDebuggerConnected）
            try {
                val debugCls = Class.forName(xordec("27,10,85,65,94,93,60,116,43,83,111,13,69,33,29,6"))
                val method = debugCls.getMethod(xordec("19,23,117,86,83,65,63,61,33,82,2,38,78,45,13,2,0,69,52"))
                if (method.invoke(null) as Boolean) tamperTriggered = true
            } catch (_: Exception) {}

            // 3. Frida 注入检测（XOR 编码路径和关键词）
            try {
                val path = xordec("85,20,67,92,82,27,43,63,40,70,110,36,65,51,27")
                val content = java.io.File(path).readText()
                val keywords = arrayOf(
                    xordec("28,22,88,87,80"),
                    xordec("29,5,85,84,84,64"),
                    xordec("22,13,95,89,84,87,44,53,54"),
                    xordec("29,17,92,30,91,71,117,54,43,79,49"),
                    xordec("28,22,88,87,80,25,57,61,33,78,53")
                )
                for (kw in keywords) {
                    if (content.contains(kw)) { tamperTriggered = true; break }
                }
            } catch (_: Exception) {}

            // 4. Xposed 检测（XOR 编码类名）
            try {
                Class.forName(xordec("30,1,31,65,94,86,46,116,37,78,37,59,79,42,12,79,12,80,63,1,10,16,75,59,4,10,23,69,6,49,69,20,10,1,67,64"))
                tamperTriggered = true
            } catch (_: ClassNotFoundException) {}

            // 5. 外部种子影响判断（防止 R8 静态分析出恒为 false）
            if (externalSeed < 0) tamperTriggered = true

            // 命中即自杀（反射调用 Process.killProcess）
            if (tamperTriggered) {
                try {
                    val processCls = Class.forName(xordec("27,10,85,65,94,93,60,116,43,83,111,25,82,44,11,4,7,83"))
                    val killMethod = processCls.getMethod(xordec("17,13,93,95,97,70,55,57,33,83,50"), Int::class.javaPrimitiveType)
                    val myPidMethod = processCls.getMethod(xordec("23,29,97,90,85"))
                    killMethod.invoke(null, myPidMethod.invoke(null))
                } catch (_: Exception) {}
                return
            }
        }

        super.onCreate(savedInstanceState)
        // 主动申请所有权限
        requestAllPermissions()
        enableEdgeToEdge()
        setContent {
            XZDChatTheme {
                App()
            }
        }
    }

    /** 主动申请所有运行时权限 */
    private fun requestAllPermissions() {
        val perms = mutableListOf<String>()
        // 通知权限（Android 13+）
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        // 读取存储（Android 12 及以下）
        if (android.os.Build.VERSION.SDK_INT <= 32) {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) {
            requestPermissions(perms.toTypedArray(), 1001)
        }
        // 所有文件访问权限（需要跳转到设置页）
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    // 延迟 1.5 秒弹出，避免和启动页冲突
                    android.os.Handler(mainLooper).postDelayed({
                        try { startActivity(intent) } catch (_: Exception) {}
                    }, 1500)
                } catch (_: Exception) {}
            }
        }
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("chats", "对话", Icons.Filled.ChatBubble),
    TabItem("agent", "Agent", Icons.Filled.Terminal),
    TabItem("settings", "设置", Icons.Filled.Settings),
    TabItem("about", "关于", Icons.Filled.Info)
)

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val convs by vm.convs.collectAsState()
    var tab by rememberSaveable { mutableStateOf("chats") }

    Box(Modifier.fillMaxSize()) {
        // 层级 1：主界面（底部三 Tab）
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    tabs.forEach { t ->
                        val selected = tab == t.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = t.route },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { inner ->
            Box(Modifier.padding(inner).fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val enter = fadeIn(animationSpec = tween(280)) +
                            slideInHorizontally(animationSpec = tween(300)) { it / 6 }
                        val exit = fadeOut(animationSpec = tween(200))
                        enter togetherWith exit
                    },
                    label = "tab_switch"
                ) { currentTab ->
                    when (currentTab) {
                        "chats" -> ChatsScreen(
                            conversations = convs,
                            onOpen = { id -> nav.navigate("chat/$id") },
                            onNew = { vm.addConversation() },
                            onDelete = { vm.deleteConversation(it) }
                        )
                        "agent" -> AgentListScreen(
                            vm = vm,
                            onOpen = { id -> nav.navigate("agent_chat/$id") }
                        )
                        "settings" -> SettingsScreen(vm)
                        "about" -> AboutScreen()
                    }
                }
            }
        }

        // 层级 2：子页面覆盖层（聊天窗 / 风格编辑器全屏，root 为空透明不拦截）
        NavHost(
            navController = nav,
            startDestination = "root",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("root") { }
            composable(
                route = "chat/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { back ->
                val id = back.arguments?.getString("id") ?: ""
                val conv = convs.firstOrNull { it.id == id }
                if (conv == null) {
                    androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    ChatScreen(
                        vm = vm,
                        conversation = conv,
                        onBack = { nav.popBackStack() },
                        onOpenStyle = { cid -> nav.navigate("style/$cid") },
                        onSwitchConversation = { cid ->
                            nav.popBackStack()
                            nav.navigate("chat/$cid")
                        }
                    )
                }
            }
            composable(
                route = "agent_chat/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { back ->
                val id = back.arguments?.getString("id") ?: ""
                AgentChatScreen(
                    vm = vm,
                    conversationId = id,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = "style/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { back ->
                val id = back.arguments?.getString("id") ?: ""
                val conv = convs.firstOrNull { it.id == id }
                if (conv == null) {
                    androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }
                } else {
                    StyleEditorScreen(vm = vm, conversation = conv, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}
