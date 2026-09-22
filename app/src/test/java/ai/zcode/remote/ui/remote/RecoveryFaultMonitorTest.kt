package ai.zcode.remote.ui.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RecoveryFaultMonitor 决策规则：页面订阅恢复失败（fault.subscription.recoveryFailed）
 * 是页内终态、页内"重新连接"无法修复，唯一可靠手段是整页重载。监控器要求：
 * 首次故障立即重载；冷却期内不重复重载；连续多次失败进入熔断，退避后重新武装。
 */
class RecoveryFaultMonitorTest {

    private val faultLog = "[v4-store] conversation recovery fail-closed: fault.subscription.recoveryFailed"
    private val otherLog = "Tooltip is changing from controlled to uncontrolled."

    @Test
    fun `首次检测到恢复故障立即重载`() {
        val monitor = RecoveryFaultMonitor()
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 0))
    }

    @Test
    fun `非故障日志不触发重载`() {
        val monitor = RecoveryFaultMonitor()
        assertFalse(monitor.onConsoleMessage(otherLog, nowMs = 0))
        assertFalse(monitor.onConsoleMessage("", nowMs = 0))
        // 冷却期内再来一条非故障日志，状态不受影响
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 1))
    }

    @Test
    fun `冷却期内重复故障不重载`() {
        val monitor = RecoveryFaultMonitor()
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 0))
        assertFalse(monitor.onConsoleMessage(faultLog, nowMs = 30_000))
    }

    @Test
    fun `冷却期过后故障重新触发重载`() {
        val monitor = RecoveryFaultMonitor()
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 0))
        assertFalse(monitor.onConsoleMessage(faultLog, nowMs = 30_000))
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 61_000))
    }

    @Test
    fun `页面加载后重置冷却状态`() {
        val monitor = RecoveryFaultMonitor()
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 0))
        monitor.onPageLoad()
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 1_000))
    }

    @Test
    fun `连续重载达到上限后熔断退避`() {
        val monitor = RecoveryFaultMonitor()
        // 第 1 次：立即允许
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 0))
        monitor.onPageLoad()
        // 第 2 次：冷却后允许
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 61_000))
        monitor.onPageLoad()
        // 第 3 次：冷却后允许，随后熔断
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 122_000))
        monitor.onPageLoad()
        assertFalse(monitor.onConsoleMessage(faultLog, nowMs = 200_000))
        // 熔断退避（10 分钟）后重新武装
        assertTrue(monitor.onConsoleMessage(faultLog, nowMs = 700_000))
    }

    @Test
    fun `熔断退避后重新进入正常冷却节奏`() {
        val monitor = RecoveryFaultMonitor()
        var now = 0L
        assertTrue(monitor.onConsoleMessage(faultLog, now)); monitor.onPageLoad()
        now += 61_000; assertTrue(monitor.onConsoleMessage(faultLog, now)); monitor.onPageLoad()
        now += 61_000; assertTrue(monitor.onConsoleMessage(faultLog, now)); monitor.onPageLoad()
        // 熔断期内的尝试全部拒绝
        now += 61_000; assertFalse(monitor.onConsoleMessage(faultLog, now))
        // 退避结束，重新武装
        now += 600_000; assertTrue(monitor.onConsoleMessage(faultLog, now))
    }
}
