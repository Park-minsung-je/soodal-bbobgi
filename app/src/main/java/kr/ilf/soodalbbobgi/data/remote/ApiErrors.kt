package kr.ilf.soodalbbobgi.data.remote

import com.google.gson.Gson
import kr.ilf.soodalbbobgi.data.remote.dto.ApiError
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import retrofit2.HttpException

private val errorBodyGson = Gson()

/**
 * 서버가 non-2xx로 돌려준 에러 본문을 [ApiError]로 읽는다.
 *
 * Retrofit은 non-2xx를 [HttpException]으로 던지므로 `ApiResponse.error` 분기에 도달하지 않는다.
 * 그래서 catch 블록에서 이 함수로 서버 메시지·코드·부가 정보를 꺼낸다.
 *
 * @return 본문이 `{ success:false, error:{...} }` 형식이면 그 error. 본문이 없거나 JSON이 아니거나
 *   [HttpException]이 아니면 null.
 */
fun Throwable.toApiError(): ApiError? {
    val http = this as? HttpException ?: return null
    val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        ?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { errorBodyGson.fromJson(body, ApiResponse::class.java).error }.getOrNull()
}
