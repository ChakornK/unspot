package com.chakornk.unspot.gecko

import android.util.Log
import androidx.annotation.NonNull
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WebExtensionManager {
    private var port: WebExtension.Port? = null
    
    private val _messages = MutableSharedFlow<WebExtensionMessage>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    data class WebExtensionMessage(
        val type: String,
        val data: JSONObject? = null,
        val rawMessage: Any? = null
    )

    val messageDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(@NonNull port: WebExtension.Port) {
            Log.d("WebExtensionManager", "Connected to WebExtension port")
            this@WebExtensionManager.port = port
            port.setDelegate(portDelegate)
        }
    }

    private val portDelegate = object : WebExtension.PortDelegate {
        override fun onPortMessage(@NonNull message: Any, @NonNull port: WebExtension.Port) {
            Log.d("WebExtensionManager", "Received message: $message")
            if (message is JSONObject) {
                val type = message.optString("type")
                if (type.isNotEmpty()) {
                    _messages.tryEmit(WebExtensionMessage(type, message.optJSONObject("data"), message))
                }
            } else if (message is String) {
                // Handle simple string messages if needed
                _messages.tryEmit(WebExtensionMessage("raw", null, message))
            }
        }

        override fun onDisconnect(@NonNull port: WebExtension.Port) {
            Log.d("WebExtensionManager", "Disconnected from WebExtension port")
            if (this@WebExtensionManager.port == port) {
                this@WebExtensionManager.port = null
            }
        }
    }

    fun sendMessage(type: String, data: Any? = null) {
        val port = this.port ?: run {
            Log.w("WebExtensionManager", "Cannot send message: Port not connected")
            return
        }

        val msg = JSONObject().apply {
            put("type", type)
            put("data", data)
        }
        
        Log.d("WebExtensionManager", "Sending message: $msg")
        port.postMessage(msg)
    }
    
    fun isConnected(): Boolean = port != null
}
