package com.soodalbbobgi.app.data.auth

import com.google.common.truth.Truth.assertThat
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ServerFailureTest {

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `4xx 응답은 REJECTED`() {
        assertThat(classifyServerFailure(httpException(400))).isEqualTo(ServerFailure.REJECTED)
        assertThat(classifyServerFailure(httpException(401))).isEqualTo(ServerFailure.REJECTED)
        assertThat(classifyServerFailure(httpException(403))).isEqualTo(ServerFailure.REJECTED)
    }

    @Test
    fun `5xx 응답은 서버 장애로 UNREACHABLE`() {
        assertThat(classifyServerFailure(httpException(500))).isEqualTo(ServerFailure.UNREACHABLE)
        assertThat(classifyServerFailure(httpException(502))).isEqualTo(ServerFailure.UNREACHABLE)
        assertThat(classifyServerFailure(httpException(503))).isEqualTo(ServerFailure.UNREACHABLE)
    }

    @Test
    fun `네트워크 예외는 UNREACHABLE`() {
        assertThat(classifyServerFailure(ConnectException("refused"))).isEqualTo(ServerFailure.UNREACHABLE)
        assertThat(classifyServerFailure(SocketTimeoutException("timeout"))).isEqualTo(ServerFailure.UNREACHABLE)
        assertThat(classifyServerFailure(UnknownHostException("dns"))).isEqualTo(ServerFailure.UNREACHABLE)
    }

    @Test
    fun `알 수 없는 예외는 보수적으로 UNREACHABLE`() {
        assertThat(classifyServerFailure(RuntimeException("boom"))).isEqualTo(ServerFailure.UNREACHABLE)
        assertThat(classifyServerFailure(IllegalStateException("state"))).isEqualTo(ServerFailure.UNREACHABLE)
    }
}
