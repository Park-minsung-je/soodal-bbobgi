package kr.ilf.soodalbbobgi.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 베이스라인 프로파일 생성기 — 콜드 스타트 여정을 기록해 자주 타는 클래스·메서드를
 * AOT 컴파일 대상으로 남긴다.
 *
 * 로그인(OAuth)은 헤드리스로 통과할 수 없어 여정은 스플래시 → 첫 화면 정착까지다.
 * 그래도 앱 초기화·Compose 런타임·내비게이션 경로가 모두 프로파일에 잡힌다.
 *
 * 생성: `./gradlew :app:generateReleaseBaselineProfile`
 * 결과: `app/src/release/generated/baselineProfiles/` → 릴리즈 빌드에 자동 포함.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "kr.ilf.soodalbbobgi",
        // 이 여정이 곧 콜드 스타트다 — 스타트업 프로파일로도 표시해 dex 배치 최적화에 쓴다
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        // 스플래시 애니메이션 → 첫 화면(인증/홈) 정착까지 기다린다
        device.waitForIdle()
        Thread.sleep(4000)
    }
}
