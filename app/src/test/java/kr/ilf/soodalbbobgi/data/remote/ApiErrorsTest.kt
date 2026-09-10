package kr.ilf.soodalbbobgi.data.remote

import com.google.common.truth.Truth.assertThat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Retrofit이 non-2xx로 던진 [HttpException] 본문에서 서버 에러를 읽는 규칙. */
class ApiErrorsTest {

    private fun http(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `4xx 본문의 error를 code·message·details까지 읽는다`() {
        val e = http(
            429,
            """{"success":false,"error":{"code":"NICKNAME_COOLDOWN","message":"m","details":{"nextAllowedAt":123}}}""",
        )
        val err = e.toApiError()
        assertThat(err?.code).isEqualTo("NICKNAME_COOLDOWN")
        assertThat(err?.message).isEqualTo("m")
        assertThat(err?.details?.nextAllowedAt).isEqualTo(123L)
    }

    @Test
    fun `details 없는 기존 형식도 읽는다`() {
        val err = http(409, """{"success":false,"error":{"code":"NICKNAME_TAKEN","message":"m"}}""").toApiError()
        assertThat(err?.code).isEqualTo("NICKNAME_TAKEN")
        assertThat(err?.details).isNull()
    }

    @Test
    fun `본문이 비었거나 JSON이 아니면 null`() {
        assertThat(http(400, "").toApiError()).isNull()
        assertThat(http(502, "<html>bad gateway</html>").toApiError()).isNull()
    }

    @Test
    fun `HttpException이 아니면 null`() {
        assertThat(RuntimeException("x").toApiError()).isNull()
    }
}
