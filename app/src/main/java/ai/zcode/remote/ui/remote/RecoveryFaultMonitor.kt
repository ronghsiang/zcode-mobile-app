package ai.zcode.remote.ui.remote

/**
 * 订阅恢复故障监控：页面订阅流恢复失败（fault.subscription.recoveryFailed）是
 * 页内终态，页面自带的"重新连接"走的是同一套失效的恢复路径，只有整页重载能
 * 脱离。云端页面失败时会打 console.warn（WebChromeClient.onConsoleMessage 可见），
 * 本监控器据此决定是否触发整页重载。
 *
 * 防循环：重载后 60s 冷却；连续 3 次重载仍失败则熔断 8 分钟（服务端状态未恢复
 * 时避免无限刷新），退避结束自动重新武装。
 */
class RecoveryFaultMonitor(
    private val cooldownMs: Long = COOLDOWN_MS,
    private val maxConsecutiveReloads: Int = MAX_CONSECUTIVE_RELOADS,
    private val circuitBreakMs: Long = CIRCUIT_BREAK_MS,
) {

    private var lastReloadAt = Long.MIN_VALUE
    private var consecutive = 0
    private var brokenUntil = Long.MIN_VALUE

    /** 页面故障日志到达；返回 true 表示应立即整页重载。 */
    fun onConsoleMessage(message: String, nowMs: Long): Boolean {
        if (!message.contains(FAULT_MARKER)) return false
        if (brokenUntil != Long.MIN_VALUE) {
            if (nowMs < brokenUntil) return false
            brokenUntil = Long.MIN_VALUE
            consecutive = 0
        }
        if (lastReloadAt != Long.MIN_VALUE && nowMs - lastReloadAt < cooldownMs) return false
        consecutive++
        lastReloadAt = nowMs
        if (consecutive >= maxConsecutiveReloads) brokenUntil = nowMs + circuitBreakMs
        return true
    }

    /** 页面（重新）加载完成：清除冷却，但保留连续失败计数。 */
    fun onPageLoad() {
        lastReloadAt = Long.MIN_VALUE
    }

    companion object {
        const val FAULT_MARKER = "fault.subscription.recoveryFailed"
        const val COOLDOWN_MS = 60_000L
        const val MAX_CONSECUTIVE_RELOADS = 3
        const val CIRCUIT_BREAK_MS = 480_000L
    }
}
