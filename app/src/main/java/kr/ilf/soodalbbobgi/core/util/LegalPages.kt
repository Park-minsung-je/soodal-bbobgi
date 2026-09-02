package kr.ilf.soodalbbobgi.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber
import java.net.URI

/**
 * 공개 법적 고지 페이지(이용약관·개인정보처리방침)의 URL을 API Base URL로부터 만든다.
 *
 * `BuildConfig.BASE_URL`은 `https://host/v1/`처럼 API 프리픽스를 포함하므로 그대로 붙이면
 * `/v1/terms`가 된다. scheme + authority만 남기고 경로를 교체한다.
 * Android 의존이 없는 [java.net.URI]를 써서 유닛 테스트에서 그대로 검증한다.
 */
object LegalPages {
    const val TERMS_PATH = "/terms"
    const val PRIVACY_PATH = "/privacy"

    /**
     * @param baseUrl API Base URL (예: `https://bbobgi.soodal.ilf.kr/v1/`)
     * @param path 도메인 루트 기준 경로 (`/terms` 또는 `/privacy`)
     * @return `scheme://authority + path` 형태의 절대 URL
     */
    fun url(baseUrl: String, path: String): String {
        val uri = URI(baseUrl)
        return "${uri.scheme}://${uri.authority}$path"
    }

    /** 이용약관 페이지 URL. */
    fun termsUrl(baseUrl: String): String = url(baseUrl, TERMS_PATH)

    /** 개인정보처리방침 페이지 URL. */
    fun privacyUrl(baseUrl: String): String = url(baseUrl, PRIVACY_PATH)
}

/**
 * URL을 기본 브라우저로 연다. 처리할 앱이 없거나 실행이 막힌 경우 앱이 죽지 않도록 삼키고 로그만 남긴다.
 *
 * @param context 화면 Context
 * @param url 열 절대 URL
 */
fun openInBrowser(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Timber.w(it, "브라우저 열기 실패: %s", url) }
}
