package kr.ilf.soodalbbobgi.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * BASE_URL(`/v1/` 프리픽스 포함)에서 법적 고지 페이지 URL을 만드는 규칙 검증.
 * Android 클래스 없이 순수 JVM에서 돈다.
 */
class LegalPagesTest {

    private val prodBase = "https://bbobgi.soodal.ilf.kr/v1/"

    @Test
    fun `terms url drops the v1 prefix and keeps scheme and host`() {
        assertThat(LegalPages.termsUrl(prodBase)).isEqualTo("https://bbobgi.soodal.ilf.kr/terms")
    }

    @Test
    fun `privacy url matches the page the settings screen already opens`() {
        assertThat(LegalPages.privacyUrl(prodBase)).isEqualTo("https://bbobgi.soodal.ilf.kr/privacy")
    }

    @Test
    fun `works without a trailing slash`() {
        assertThat(LegalPages.termsUrl("https://bbobgi.soodal.ilf.kr/v1")).isEqualTo("https://bbobgi.soodal.ilf.kr/terms")
    }

    @Test
    fun `keeps a custom port for a local dev server`() {
        assertThat(LegalPages.url("http://10.0.2.2:3077/v1/", "/terms")).isEqualTo("http://10.0.2.2:3077/terms")
    }
}
