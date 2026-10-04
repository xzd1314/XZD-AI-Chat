package com.xzd1314.aichat.agent

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xzd1314.aichat.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Agent 定时任务 Worker
 * 由 WorkManager 周期性触发，执行 Store 中启用的定时任务命令
 */
class AgentCommandWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val store = Store.get(applicationContext)
            val tasks = store.getScheduledTasks().filter { it.enabled }
            val now = System.currentTimeMillis()
            tasks.forEach { task ->
                val intervalMs = task.intervalMinutes * 60 * 1000L
                if (intervalMs <= 0 || now - task.lastRun >= intervalMs) {
                    // 执行命令
                    val result = ShellExecutor.execute(task.command, applicationContext)
                    // 记录命令历史
                    store.addCommandHistory(
                        com.xzd1314.aichat.data.CommandHistoryItem(
                            command = task.command,
                            success = result.exitCode == 0,
                            exitCode = result.exitCode,
                            outputPreview = result.stdout.take(200)
                        )
                    )
                    // 更新 lastRun
                    val updated = store.getScheduledTasks().map {
                        if (it.id == task.id) it.copy(lastRun = now) else it
                    }
                    store.saveScheduledTasks(updated)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "xzd_agent_scheduled"
    }
}
