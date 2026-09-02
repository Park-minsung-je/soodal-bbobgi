package kr.ilf.soodalbbobgi.data.auth

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kr.ilf.soodalbbobgi.data.local.LocalDataResetter
import org.junit.Test

/** 로그인한 계정이 로컬 데이터의 마지막 소유자와 다를 때만 로컬을 초기화하는지 검증. */
class AccountSwitchGuardTest {
    private val prefs: AccountPrefs = mockk(relaxed = true)
    private val resetter: LocalDataResetter = mockk(relaxed = true)
    private val guard = AccountSwitchGuard(prefs, resetter)

    @Test
    fun `첫 로그인이면 초기화 없이 소유자만 기록한다`() = runTest {
        every { prefs.lastLocalUserId } returns null

        assertThat(guard.ensureLocalOwnedBy("u1")).isFalse()

        coVerify(exactly = 0) { resetter.clearAll(any()) }
        verify { prefs.lastLocalUserId = "u1" }
    }

    @Test
    fun `같은 계정 재로그인은 로컬을 지우지 않는다`() = runTest {
        every { prefs.lastLocalUserId } returns "u1"

        assertThat(guard.ensureLocalOwnedBy("u1")).isFalse()

        coVerify(exactly = 0) { resetter.clearAll(any()) }
    }

    @Test
    fun `다른 계정이면 에셋만 남기고 초기화한 뒤 소유자를 바꾼다`() = runTest {
        every { prefs.lastLocalUserId } returns "u1"

        assertThat(guard.ensureLocalOwnedBy("u2")).isTrue()

        coVerifyOrder {
            resetter.clearAll(keepAssets = true)
            prefs.lastLocalUserId = "u2"
        }
    }
}
