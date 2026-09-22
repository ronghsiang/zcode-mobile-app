package ai.zcode.remote.ui.remote

/**
 * 远程页加载 URL 重写：连接里保存的 app_version 是扫码当时桌面端的版本，
 * 桌面端自动升级后该参数会把云端页面钉死在旧构建上——旧页面叠加新桌面端
 * 会导致额度、模型管理等较新功能失效。加载时统一改写为 latest，让云端
 * 始终下发当前稳定构建。仅用于加载；连接身份匹配仍用原始 URL。
 */
object RemotePageUrl {

    private const val PAGE_MARKER = "zcode.z.ai/remote"

    fun withLatestPageBuild(url: String): String {
        if (!url.contains(PAGE_MARKER)) return url
        return when {
            url.contains("app_version=") ->
                url.replace(Regex("app_version=[^&]*"), "app_version=latest")
            url.contains("?") -> "$url&app_version=latest"
            else -> "$url?app_version=latest"
        }
    }
}
