package kr.ilf.soodalbbobgi.core.di

import javax.inject.Qualifier

/** Retrofit base URL 문자열 주입용 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

/**
 * 화면 수명과 무관하게 끝까지 실행되어야 하는 백그라운드 작업
 * (로그인 직후 동기화 등)용 앱 수준 스코프 한정자.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
