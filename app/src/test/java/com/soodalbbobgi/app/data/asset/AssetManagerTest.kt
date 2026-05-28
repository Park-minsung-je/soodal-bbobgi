package com.soodalbbobgi.app.data.asset

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.AssetManifestData
import com.soodalbbobgi.app.data.remote.dto.ServerAssetFile
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.security.MessageDigest

/**
 * AssetManager 단위 테스트.
 *
 * SoodalApi는 mockk으로, AssetStore는 임시 디렉토리 위에 실제 인스턴스로 구동한다.
 * (AssetStore는 외부 의존성이 파일 시스템뿐이라 실제로 돌리는 게 더 신뢰 가능.)
 */
class AssetManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var api: SoodalApi
    private lateinit var store: AssetStore
    private lateinit var rootDir: File
    private lateinit var manager: AssetManager

    @Before
    fun setUp() {
        api = mockk()
        rootDir = File(tempFolder.root, "assets")
        store = AssetStore(rootDir)
        manager = AssetManager(api, store)
    }

    // ─── 콜드 스타트 ────────────────────────────────────────────

    @Test
    fun `cold start downloads all files and saves manifest`() = runTest {
        val helloBytes = "hello".toByteArray()
        val worldBytes = "world".toByteArray()
        val helloHash = sha256("hello")
        val worldHash = sha256("world")

        val serverManifest = AssetManifestData(
            version = "1.0.0",
            updatedAt = 100L,
            files = listOf(
                ServerAssetFile("char/c1.png", helloHash, helloBytes.size.toLong()),
                ServerAssetFile("bg/b1.png", worldHash, worldBytes.size.toLong()),
            ),
        )
        coEvery { api.getAssetManifest() } returns ApiResponse(true, serverManifest, null)
        coEvery { api.downloadAssetFile("char/c1.png") } returns successBody(helloBytes)
        coEvery { api.downloadAssetFile("bg/b1.png") } returns successBody(worldBytes)

        val result = manager.sync()

        assertThat(result.isSuccess).isTrue()
        assertThat(store.exists("char/c1.png")).isTrue()
        assertThat(store.exists("bg/b1.png")).isTrue()
        assertThat(store.fileFor("char/c1.png").readBytes()).isEqualTo(helloBytes)
        assertThat(store.loadLocalManifest()?.version).isEqualTo("1.0.0")
        assertThat(store.loadLocalManifest()?.files).hasSize(2)

        val progress = manager.progress.value
        assertThat(progress).isInstanceOf(AssetSyncProgress.Done::class.java)
        progress as AssetSyncProgress.Done
        assertThat(progress.version).isEqualTo("1.0.0")
        assertThat(progress.downloaded).isEqualTo(2)
        assertThat(progress.removed).isEqualTo(0)
    }

    @Test
    fun `cold start with empty manifest succeeds with no downloads`() = runTest {
        coEvery { api.getAssetManifest() } returns ApiResponse(
            true,
            AssetManifestData("0.1", 0L, emptyList()),
            null,
        )

        val result = manager.sync()

        assertThat(result.isSuccess).isTrue()
        assertThat(store.loadLocalManifest()?.files).isEmpty()
        coVerify(exactly = 0) { api.downloadAssetFile(any()) }

        val progress = manager.progress.value as AssetSyncProgress.Done
        assertThat(progress.downloaded).isEqualTo(0)
        assertThat(progress.removed).isEqualTo(0)
    }

    // ─── 증분 동기화 ───────────────────────────────────────────

    @Test
    fun `incremental only downloads files with changed hashes`() = runTest {
        // 로컬: c1=hello, b1=world. 서버: c1 그대로, b1 새 내용("new").
        val helloBytes = "hello".toByteArray()
        val worldBytes = "world".toByteArray()
        val newBytes = "new".toByteArray()
        val helloHash = sha256("hello")
        val newHash = sha256("new")
        val worldHash = sha256("world")

        store.writeFile("char/c1.png", helloBytes)
        store.writeFile("bg/b1.png", worldBytes)
        store.saveLocalManifest(
            com.soodalbbobgi.app.domain.model.AssetManifest(
                version = "1.0.0",
                updatedAt = 100L,
                files = listOf(
                    com.soodalbbobgi.app.domain.model.AssetFile("char/c1.png", helloHash, helloBytes.size.toLong()),
                    com.soodalbbobgi.app.domain.model.AssetFile("bg/b1.png", worldHash, worldBytes.size.toLong()),
                ),
            ),
        )

        val serverManifest = AssetManifestData(
            version = "1.1.0",
            updatedAt = 200L,
            files = listOf(
                ServerAssetFile("char/c1.png", helloHash, helloBytes.size.toLong()),
                ServerAssetFile("bg/b1.png", newHash, newBytes.size.toLong()),
            ),
        )
        coEvery { api.getAssetManifest() } returns ApiResponse(true, serverManifest, null)
        coEvery { api.downloadAssetFile("bg/b1.png") } returns successBody(newBytes)

        val result = manager.sync()

        assertThat(result.isSuccess).isTrue()
        // 변경 안 된 c1은 그대로
        assertThat(store.fileFor("char/c1.png").readBytes()).isEqualTo(helloBytes)
        // 변경된 b1은 새 내용
        assertThat(store.fileFor("bg/b1.png").readBytes()).isEqualTo(newBytes)
        // c1은 다운받지 않아야 함
        coVerify(exactly = 0) { api.downloadAssetFile("char/c1.png") }
        coVerify(exactly = 1) { api.downloadAssetFile("bg/b1.png") }

        val progress = manager.progress.value as AssetSyncProgress.Done
        assertThat(progress.downloaded).isEqualTo(1)
        assertThat(progress.removed).isEqualTo(0)
    }

    // ─── 삭제 ──────────────────────────────────────────────────

    @Test
    fun `files removed from server manifest are deleted from disk`() = runTest {
        val aBytes = "a".toByteArray()
        val orphanBytes = "orphan".toByteArray()
        val aHash = sha256("a")
        val orphanHash = sha256("orphan")

        store.writeFile("keep/a.png", aBytes)
        store.writeFile("gone/orphan.png", orphanBytes)
        store.saveLocalManifest(
            com.soodalbbobgi.app.domain.model.AssetManifest(
                version = "1.0.0",
                updatedAt = 100L,
                files = listOf(
                    com.soodalbbobgi.app.domain.model.AssetFile("keep/a.png", aHash, aBytes.size.toLong()),
                    com.soodalbbobgi.app.domain.model.AssetFile("gone/orphan.png", orphanHash, orphanBytes.size.toLong()),
                ),
            ),
        )

        val serverManifest = AssetManifestData(
            version = "2.0.0",
            updatedAt = 200L,
            files = listOf(
                ServerAssetFile("keep/a.png", aHash, aBytes.size.toLong()),
            ),
        )
        coEvery { api.getAssetManifest() } returns ApiResponse(true, serverManifest, null)

        val result = manager.sync()

        assertThat(result.isSuccess).isTrue()
        assertThat(store.exists("keep/a.png")).isTrue()
        assertThat(store.exists("gone/orphan.png")).isFalse()
        coVerify(exactly = 0) { api.downloadAssetFile(any()) }

        val progress = manager.progress.value as AssetSyncProgress.Done
        assertThat(progress.downloaded).isEqualTo(0)
        assertThat(progress.removed).isEqualTo(1)
        assertThat(store.loadLocalManifest()?.files?.map { it.path }).containsExactly("keep/a.png")
    }

    // ─── 매니페스트 fetch 실패 ────────────────────────────────────

    @Test
    fun `manifest fetch network failure returns failure and leaves local manifest untouched`() = runTest {
        // 기존 매니페스트가 있다는 상황을 만든다.
        val before = com.soodalbbobgi.app.domain.model.AssetManifest("0.9.0", 99L, emptyList())
        store.saveLocalManifest(before)

        coEvery { api.getAssetManifest() } throws java.io.IOException("network down")

        val result = manager.sync()

        assertThat(result.isFailure).isTrue()
        assertThat(store.loadLocalManifest()).isEqualTo(before)

        val progress = manager.progress.value
        assertThat(progress).isInstanceOf(AssetSyncProgress.Error::class.java)
    }

    @Test
    fun `manifest fetch returns success=false returns failure`() = runTest {
        coEvery { api.getAssetManifest() } returns ApiResponse(false, null, null)

        val result = manager.sync()

        assertThat(result.isFailure).isTrue()
        assertThat(manager.progress.value).isInstanceOf(AssetSyncProgress.Error::class.java)
    }

    // ─── 다운로드 중 HTTP 에러 ────────────────────────────────────

    @Test
    fun `download HTTP error returns failure and does not update local manifest`() = runTest {
        val helloBytes = "hello".toByteArray()
        val helloHash = sha256("hello")
        val serverManifest = AssetManifestData(
            "1.0.0", 100L,
            listOf(ServerAssetFile("char/c1.png", helloHash, helloBytes.size.toLong())),
        )
        coEvery { api.getAssetManifest() } returns ApiResponse(true, serverManifest, null)
        val errorBody = "Not Found".toResponseBody("text/plain".toMediaTypeOrNull())
        coEvery { api.downloadAssetFile("char/c1.png") } returns Response.error(404, errorBody)

        val result = manager.sync()

        assertThat(result.isFailure).isTrue()
        // 로컬 매니페스트는 갱신되지 않아야 한다.
        assertThat(store.loadLocalManifest()).isNull()
        assertThat(manager.progress.value).isInstanceOf(AssetSyncProgress.Error::class.java)
        // 부분 파일이 디스크에 남을 수도/안 남을 수도 있다 (네트워크 호출 자체가 실패했으므로 여기서는 안 남음).
    }

    // ─── 해시 불일치 검증 ─────────────────────────────────────────

    @Test
    fun `hash mismatch after download deletes file and returns failure`() = runTest {
        // 서버 매니페스트는 hash X를 기대하지만, 실제 본문은 Y의 해시.
        val claimedHash = sha256("expected")
        val actualBytes = "actual".toByteArray()
        val serverManifest = AssetManifestData(
            "1.0.0", 100L,
            listOf(ServerAssetFile("char/c1.png", claimedHash, actualBytes.size.toLong())),
        )
        coEvery { api.getAssetManifest() } returns ApiResponse(true, serverManifest, null)
        coEvery { api.downloadAssetFile("char/c1.png") } returns successBody(actualBytes)

        val result = manager.sync()

        assertThat(result.isFailure).isTrue()
        assertThat(store.exists("char/c1.png")).isFalse()
        assertThat(store.loadLocalManifest()).isNull()
        assertThat(manager.progress.value).isInstanceOf(AssetSyncProgress.Error::class.java)
    }

    // ─── progress 시퀀스 ─────────────────────────────────────────

    @Test
    fun `progress starts at Idle`() {
        assertThat(manager.progress.value).isEqualTo(AssetSyncProgress.Idle)
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────

    /** ResponseBody를 성공 응답으로 감싸 반환한다. */
    private fun successBody(bytes: ByteArray): Response<ResponseBody> {
        val body = bytes.toResponseBody("application/octet-stream".toMediaTypeOrNull())
        return Response.success(body)
    }

    /** 테스트용 SHA-256 해시 계산. AssetStore의 hashOf와 동일한 "sha256-{hex}" 포맷. */
    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return "sha256-" + digest.joinToString("") { "%02x".format(it) }
    }
}
