package kr.ilf.soodalbbobgi.data.auth

import retrofit2.HttpException
import java.io.IOException

/**
 * 서버 호출 실패의 성격 — 인증 처리에서 토큰을 지울지 결정하는 기준.
 */
enum class ServerFailure {
    /** 서버가 요청을 명시적으로 거부 (4xx) — 토큰 무효로 간주해도 안전. */
    REJECTED,

    /** 서버에 닿지 못했거나 서버 내부 장애 (네트워크 오류/5xx) — 토큰을 지우면 안 됨. */
    UNREACHABLE,
}

/**
 * 예외를 [ServerFailure]로 분류한다.
 *
 * 알 수 없는 예외는 보수적으로 [ServerFailure.UNREACHABLE]로 둔다 — 일시 장애에
 * 멀쩡한 세션을 지우는 것보다, 무효 토큰이 한 번 더 살아있다가 다음 호출에서
 * 거부되는 쪽이 안전하다.
 *
 * @param t 서버 호출에서 발생한 예외
 */
fun classifyServerFailure(t: Throwable): ServerFailure = when {
    t is HttpException && t.code() in 400..499 -> ServerFailure.REJECTED
    t is IOException -> ServerFailure.UNREACHABLE
    else -> ServerFailure.UNREACHABLE
}
