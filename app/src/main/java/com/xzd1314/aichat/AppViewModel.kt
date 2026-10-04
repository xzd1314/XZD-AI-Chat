package com.xzd1314.aichat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.xzd1314.aichat.data.AgentConversation
import com.xzd1314.aichat.data.AgentMessage
import com.xzd1314.aichat.data.ChatMessage
import com.xzd1314.aichat.data.ChatStyle
import com.xzd1314.aichat.data.Conversation
import com.xzd1314.aichat.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val store: Store = Store.get(app)

    private val _convs = MutableStateFlow<List<Conversation>>(store.loadConversations())
    val convs: StateFlow<List<Conversation>> = _convs

    // Agent 会话
    private val _agentConvs = MutableStateFlow<List<AgentConversation>>(store.getAgentConvs())
    val agentConvs: StateFlow<List<AgentConversation>> = _agentConvs

    fun getAgentById(id: String): AgentConversation? = _agentConvs.value.firstOrNull { it.id == id }

    fun addAgentConversation() {
        val c = store.createAgentConversation()
        _agentConvs.value = listOf(c) + _agentConvs.value
        persistAgent()
    }

    fun deleteAgentConversation(id: String) {
        _agentConvs.value = _agentConvs.value.filter { it.id != id }
        persistAgent()
    }

    fun addAgentMessage(id: String, msg: AgentMessage) {
        _agentConvs.value = _agentConvs.value.map {
            if (it.id == id) it.copy(
                messages = (it.messages + msg).toMutableList(),
                updatedAt = System.currentTimeMillis()
            ) else it
        }
        persistAgent()
    }

    fun updateAgentMessage(id: String, index: Int, msg: AgentMessage) {
        _agentConvs.value = _agentConvs.value.map {
            if (it.id == id) {
                val msgs = it.messages.toMutableList()
                if (index in msgs.indices) msgs[index] = msg
                it.copy(messages = msgs, updatedAt = System.currentTimeMillis())
            } else it
        }
        persistAgent()
    }

    fun renameAgentConversation(id: String, title: String) {
        _agentConvs.value = _agentConvs.value.map {
            if (it.id == id) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it
        }
        persistAgent()
    }

    private fun persistAgent() = store.saveAgentConvs(_agentConvs.value)

    /** 当前打开的会话 id */
    var currentId by mutableStateOf<String?>(null)
        private set

    fun open(id: String) { currentId = id }

    fun current(): Conversation? = _convs.value.firstOrNull { it.id == currentId }

    fun addConversation() {
        val c = store.createConversation()
        _convs.value = listOf(c) + _convs.value
        persist()
        currentId = c.id
    }

    fun deleteConversation(id: String) {
        _convs.value = _convs.value.filterNot { it.id == id }
        if (currentId == id) currentId = null
        persist()
    }

    fun rename(id: String, title: String) {
        _convs.value = _convs.value.map {
            if (it.id == id) it.copy(title = title.ifBlank { "未命名对话" }, updatedAt = System.currentTimeMillis()) else it
        }
        persist()
    }

    fun setAvatar(id: String, uri: String?) {
        _convs.value = _convs.value.map {
            if (it.id == id) it.copy(avatarUri = uri, updatedAt = System.currentTimeMillis()) else it
        }
        persist()
    }

    fun applyStyle(id: String, style: ChatStyle, presetId: String) {
        _convs.value = _convs.value.map {
            if (it.id == id) it.copy(style = style, stylePresetId = presetId, updatedAt = System.currentTimeMillis()) else it
        }
        persist()
    }

    fun addMessage(id: String, role: String, content: String, reasoning: String? = null, imageUri: String? = null) {
        _convs.value = _convs.value.map {
            if (it.id == id) {
                it.copy(
                    messages = (it.messages + ChatMessage(role, content, System.currentTimeMillis(), reasoning, imageUri)).toMutableList(),
                    updatedAt = System.currentTimeMillis()
                )
            } else it
        }
        persist()
    }

    fun clearMessages(id: String) {
        _convs.value = _convs.value.map {
            if (it.id == id) it.copy(messages = mutableListOf(), updatedAt = System.currentTimeMillis()) else it
        }
        persist()
    }

    fun getById(id: String): Conversation? = _convs.value.firstOrNull { it.id == id }

    private fun persist() {
        store.saveConversations(_convs.value)
    }
}
