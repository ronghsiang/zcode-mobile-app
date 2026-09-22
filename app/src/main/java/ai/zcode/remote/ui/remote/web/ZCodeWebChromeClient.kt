package ai.zcode.remote.ui.remote.web

import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import ai.zcode.remote.ui.remote.RecoveryFaultMonitor

class ZCodeWebChromeClient(
    private val allowedHost: String?,
    private val onProgressUpdate: (progress: Int) -> Unit,
    private val onTitleReceived: (title: String) -> Unit,
    private val onOpenFileChooser: (filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams) -> Boolean,
    private val onRecoveryFaultDetected: (message: String) -> Unit = {},
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressUpdate(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            onTitleReceived(title)
        }
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        val originHost = request?.origin?.host
        val allowed = !allowedHost.isNullOrBlank() && originHost.equals(allowedHost, ignoreCase = true)
        val resources = request?.resources.orEmpty().filter {
            it == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }.toTypedArray()
        if (allowed && resources.isNotEmpty()) {
            request?.grant(resources)
        } else {
            request?.deny()
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback != null && fileChooserParams != null) {
            return onOpenFileChooser(filePathCallback, fileChooserParams)
        }
        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val text = consoleMessage.message()
            android.util.Log.d("ZCodeWeb", "[${consoleMessage.messageLevel()}] $text (line: ${consoleMessage.lineNumber()})")
            // 页面订阅恢复失败打的是页内终态日志，页内"重新连接"无法修复，
            // 只能靠 App 侧整页重载——检测到即上抛（由 Activity 决策冷却/熔断）。
            if (text.contains(RecoveryFaultMonitor.FAULT_MARKER)) {
                onRecoveryFaultDetected(text)
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }
}
