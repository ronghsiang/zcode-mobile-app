package ai.zcode.remote.ui.remote

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RemotePageUrl 重写规则：加载远程页时统一把 app_version 指向 latest，
 * 避免连接保存时的旧版本参数把云端页面钉死在旧构建上（旧页面 + 新桌面端
 * 会导致额度/模型管理等新功能失效）。
 */
class RemotePageUrlTest {

    private val minideskUrl = "https://zcode.z.ai/remote/v4?sid=d_UXNddCStPAEVefAAatomWF" +
        "&hash=CQNmwpW7MOBjBL0rjdqYk%2FNVLZPe5LS%2FpSUeAH51q2s%3D&t=1788226490774" +
        "&mid=4e97c63c-94c7-41aa-bb7b-6fe428ac1553&name=MrRONG_Minidesk&app_version=3.10.1"

    @Test
    fun `已有的 app_version 改写为 latest 且其余参数不变`() {
        val result = RemotePageUrl.withLatestPageBuild(minideskUrl)
        assertEquals(
            "https://zcode.z.ai/remote/v4?sid=d_UXNddCStPAEVefAAatomWF" +
                "&hash=CQNmwpW7MOBjBL0rjdqYk%2FNVLZPe5LS%2FpSUeAH51q2s%3D&t=1788226490774" +
                "&mid=4e97c63c-94c7-41aa-bb7b-6fe428ac1553&name=MrRONG_Minidesk&app_version=latest",
            result,
        )
    }

    @Test
    fun `无 app_version 且已有查询参数时追加 latest`() {
        val result = RemotePageUrl.withLatestPageBuild(
            "https://zcode.z.ai/remote/v4?sid=d_UXNddCStPAEVefAAatomWF&mid=4e97c63c",
        )
        assertEquals(
            "https://zcode.z.ai/remote/v4?sid=d_UXNddCStPAEVefAAatomWF&mid=4e97c63c&app_version=latest",
            result,
        )
    }

    @Test
    fun `无查询参数时以问号追加 latest`() {
        val result = RemotePageUrl.withLatestPageBuild("https://zcode.z.ai/remote/v4")
        assertEquals("https://zcode.z.ai/remote/v4?app_version=latest", result)
    }

    @Test
    fun `非远程页 URL 原样返回`() {
        val url = "https://example.com/some/other/page?app_version=3.10.1"
        assertEquals(url, RemotePageUrl.withLatestPageBuild(url))
    }

    @Test
    fun `空 URL 原样返回`() {
        assertEquals("", RemotePageUrl.withLatestPageBuild(""))
    }
}
