package com.soodalbbobgi.app.data.remote.api

import com.soodalbbobgi.app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 수달 뽑기 서버 REST API 인터페이스.
 * Base URL: https://soodal.bbobgi.ilf.kr/v1/
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

    // ── User ──

    /** 현재 사용자 정보 조회 */
    @GET("users/me")
    suspend fun getMe(): ApiResponse<UserData>

    /** 사용자 정보 수정 (닉네임, 성별, 연령대) */
    @PATCH("users/me")
    suspend fun updateMe(@Body request: UpdateUserRequest): ApiResponse<UserData>

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

    /** 수영 기록 목록 조회 */
    @GET("swim-logs")
    suspend fun getSwimLogs(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): ApiResponse<SwimLogsData>

    /** 수영 기록 삭제 (soft-delete, 조개 미회수) */
    @DELETE("swim-logs/by-date/{date}")
    suspend fun deleteSwimLog(@Path("date") date: String): ApiResponse<DeleteSwimLogData>

    /** 수영 통계 조회 */
    @GET("swim-logs/stats")
    suspend fun getSwimStats(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): ApiResponse<SwimStatsData>
}
