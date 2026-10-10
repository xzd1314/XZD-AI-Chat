package com.xzd1314.aichat.agent

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xzd1314.aichat.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 快捷指令磁贴
 * 在下拉菜单中显示"XZD 快捷指令"，点击启动应用并跳转到 Agent 页
 * 长按可以配置快捷命令
 */
class QuickCommandTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.let { tile ->
            tile.state = Tile.STATE_ACTIVE
            tile.label = "XZD 快捷指令"
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // 启动应用并跳转到 Agent 页
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("quick_command", true)
        }
        startActivityAndCollapse(intent)

        // 如果有保存的快捷命令，直接执行
        val store = com.xzd1314.aichat.data.Store.get(this)
        val quickCmd = store.getScheduledTasks().firstOrNull { it.name == "快捷命令" }
        if (quickCmd != null) {
            CoroutineScope(Dispatchers.IO).launch {
                ShellExecutor.execute(quickCmd.command, this@QuickCommandTileService)
            }
        }
    }
}
