package kr.ilf.soodalbbobgi.data.remote.api

import kr.ilf.soodalbbobgi.data.remote.dto.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * 수달 뽑기 서버 REST API 인터페이스.
 * Base URL은 `BuildConfig.BASE_URL` — local.properties의 `SOODAL_BASE_URL`에서 주입된다.
 */
interface SoodalApi {

    // ── Auth ──

    /** 카카오 액세스 토큰으로 로그인/회원가입 */
    @POST("auth/kakao")
    suspend fun authKakao(@Body request: KakaoAuthRequest): ApiResponse<AuthData>

    /** Google ID 토큰으로 로그인/회원가입 */
    @POST("auth/google")
    suspend fun authGoogle(@Body request: GoogleAuthRequest): ApiResponse<AuthData>

    /** 만료된 액세스 토큰을 리프레시 토큰으로 갱신 */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): ApiResponse<TokenData>

    /** 리프레시 토큰 무효화 (로그아웃) */
    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): ApiResponse<Unit>

    // ── User ──

    /** 현재 사용자 정보 조회 */
    @GET("users/me")
    suspend fun getMe(): ApiResponse<UserData>

    /** 사용자 정보 수정 (닉네임, 성별, 연령대) */
    @PATCH("users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): ApiResponse<UserData>

    /** 계정 삭제 — 서버의 모든 데이터가 영구 삭제된다 */
    @DELETE("users/me")
    suspend fun deleteMe(): ApiResponse<Unit>

    // ── Gacha ──

    /** 활성 뽑기 상자 + 아이템 목록 */
    @GET("gacha/boxes")
    suspend fun getGachaBoxes(): ApiResponse<GachaBoxesData>

    /** 뽑기 실행 */
    @POST("gacha/pull")
    suspend fun gachaPull(@Body request: GachaPullRequest): ApiResponse<GachaPullData>

    // ── Inventory ──

    /** 보유 아이템 목록 조회 */
    @GET("inventory")
    suspend fun getInventory(@Query("category") category: String? = null): ApiResponse<InventoryData>

    // ── Items (마스터 카탈로그) ──

    /** 활성 아이템 마스터 전체 목록 (Splash에서 한 번 받아 캐시) */
    @GET("items")
    suspend fun getItems(): ApiResponse<ItemsData>

    // ── Profile Card ──

    /** 프로필 카드 설정 조회 */
    @GET("profile-card")
    suspend fun getProfileCard(): ApiResponse<ServerProfileCard>

    /** 프로필 카드 설정 저장 */
    @PUT("profile-card")
    suspend fun updateProfileCard(@Body request: ServerProfileCard): ApiResponse<ProfileCardUpdateData>

    // ── Shop ──

    /** 진열 중인 상품 목록 + 사용자별 구매 카운트 */
    @GET("shop")
    suspend fun getShop(): ApiResponse<ShopListingsData>

    /** 상점 상품 구매 (item이면 인벤토리 추가, box면 즉시 가챠 실행) */
    @POST("shop/purchase")
    suspend fun shopPurchase(@Body request: ShopPurchaseRequest): ApiResponse<ShopPurchaseData>

    // ── Swim Logs ──

    /** 수영 기록 추가 */
    @POST("swim-logs")
    suspend fun addSwimLog(@Body request: SwimLogRequest): ApiResponse<SwimLogResponseData>

    /** 수영 기록 목록 조회 — 기간을 비우면 전체 이력을 받는다 (재설치·데이터 손실 복원용). */
    @GET("swim-logs")
    suspend fun getSwimLogs(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
    ): ApiResponse<SwimLogsData>

    /** 영법별 거리 보정 (총 거리는 유지, 분배만 갱신) */
    @PATCH("swim-logs/by-date/{date}/strokes")
    suspend fun updateSwimLogStrokes(
        @Path("date") date: String,
        @Body request: UpdateStrokesRequest,
    ): ApiResponse<UpdateStrokesData>

    /** 이미 서버에 있는 기록의 빈 심박 채우기 — 심박 컬럼 도입 전 기록 복구용 */
    @PATCH("swim-logs/by-date/{date}/vitals")
    suspend fun updateSwimLogVitals(
        @Path("date") date: String,
        @Body request: UpdateVitalsRequest,
    ): ApiResponse<SwimLogResponseData>

    /** 수영 기록 삭제 (soft-delete, 조개 미회수) */
    @DELETE("swim-logs/by-date/{date}")
    suspend fun deleteSwimLog(@Path("date") date: String): ApiResponse<DeleteSwimLogData>

    /**
     * (개발자 전용) 최근 수영 기록과 그 지급 이력을 서버에서 되돌린다 — 보상 흐름 재테스트용.
     * 서버가 `ALLOW_DEV_RESET`으로 열어둔 경우에만 응답하며, 꺼져 있으면 404다.
     */
    @POST("dev/reset-swim-logs")
    suspend fun devResetSwimLogs(@Body body: DevResetRequest): ApiResponse<DevResetData>

    /** 수영 통계 조회 */
    @GET("swim-logs/stats")
    suspend fun getSwimStats(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): ApiResponse<SwimStatsData>

    // ── Assets ──

    /** 에셋 매니페스트 조회 (인증 불필요) */
    @GET("assets/version")
    suspend fun getAssetManifest(): ApiResponse<AssetManifestData>

    /**
     * 개별 에셋 파일 다운로드 (streaming, 인증 불필요).
     *
     * @param path forward-slash 상대경로. 슬래시를 살리려 encoded=true.
     * @return raw [ResponseBody]를 [Response]로 감싸 HTTP 에러 코드를 분리 처리할 수 있게 한다.
     */
    @Streaming
    @GET("assets/files/{path}")
    suspend fun downloadAssetFile(@Path("path", encoded = true) path: String): Response<ResponseBody>
}
