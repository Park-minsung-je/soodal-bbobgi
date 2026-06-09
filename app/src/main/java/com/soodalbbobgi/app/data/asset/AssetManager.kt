package com.soodalbbobgi.app.data.asset

import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.toDomain
import com.soodalbbobgi.app.domain.model.AssetFile
import com.soodalbbobgi.app.domain.model.AssetManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 에셋 매니페스트와 로컬을 비교해 변경분만 다운로드/삭제하고 로컬 매니페스트를 갱신한다.
 *
 * 동기화 흐름 (`sync()` 호출 시):
 * 1. 서버 매니페스트 조회 ([SoodalApi.getAssetManifest])
 * 2. 로컬 매니페스트와 hash 비교
 *    - toDownload: 로컬에 없거나 hash가 달라진 path
 *    - toDelete: 로컬엔 있는데 서버 매니페스트엔 없는 path
 * 3. toDownload를 순차 다운로드 ([SoodalApi.downloadAssetFile]) → [AssetStore.writeFile]로 원자적 저장,
 *    다운로드 직후 [AssetStore.hashOf]로 해시 검증 (CDN 손상/쓰기 오류 방어)
 *    - HTTP 4xx 응답(서버에 파일 없음)은 해당 파일만 건너뛰고 계속 진행한다.
 *    - 네트워크 오류, 5xx, 해시 불일치는 전체 sync를 실패 처리한다.
 * 4. toDelete를 순차 삭제
 * 5. 서버 매니페스트에서 건너뛴(4xx) 파일을 제외한 매니페스트를 로컬에 저장.
 *    건너뛴 파일은 로컬 매니페스트에 없으므로 다음 sync 때 diff에서 다시 포착되지만,
 *    sync 자체는 성공으로 끝나 "에셋 동기화 실패" 메시지가 뜨지 않는다.
 *
 * 진행 상태는 [progress] StateFlow로 노출. Splash UI가 collect해서 사용자에게 표시한다.
 *
 * 주의:
 * - 동시 [sync] 호출은 내부 [Mutex]로 직렬화된다 (AssetStore가 단일 스레드 가정이라 race 방지 필수).
 * - 실패해도 partial progress(이미 받은 파일들)는 디스크에 남는다.
 *   로컬 매니페스트는 갱신되지 않으므로 다음 sync에서 자연스럽게 재시도된다.
 * - 재시도 로직 없음. 4xx 외의 파일 실패는 전체 sync 실패.
 */
@Singleton
class AssetManager @Inject constructor(
    private val api: SoodalApi,
    private val store: AssetStore,
) {

    private val _progress = MutableStateFlow<AssetSyncProgress>(AssetSyncProgress.Idle)

    /** 동기화 진행 상태. UI가 collect해서 진행률을 표시한다. */
    val progress: StateFlow<AssetSyncProgress> = _progress.asStateFlow()

    /**
     * 동시 sync 호출을 직렬화하는 뮤텍스.
     * Splash 자동 동기화와 사용자 수동 새로고침이 겹쳐도 progress 발행과
     * manifest.json.tmp 쓰기가 race하지 않도록 한다.
     */
    private val syncMutex = Mutex()

    /**
     * 서버 매니페스트와 로컬을 비교해 변경분만 다운로드/삭제하고 로컬 매니페스트를 갱신한다.
     *
     * 호출자가 viewModelScope 등 적절한 스코프에서 호출해야 한다.
     * 동시 호출은 내부 [Mutex]로 직렬화되므로 호출자가 락을 따로 잡을 필요는 없다.
     *
     * @return 성공 시 Result.success(Unit). 실패 시 Result.failure with 원본 예외.
     *         실패해도 이미 받은 파일은 디스크에 남고, 로컬 매니페스트만 미갱신 상태로 유지된다.
     */
    suspend fun sync(): Result<Unit> {
        // 초기 호출 직후 progress가 즉시 FetchingManifest로 전환되도록 한다 (Idle race 방지).
        // Splash 네비게이션 게이트가 Idle을 "동기화 끝남"으로 오인해 통과시키는 race를 막는다.
        _progress.value = AssetSyncProgress.FetchingManifest
        return syncMutex.withLock {
            // 1. 서버 매니페스트 조회
            val serverManifest: AssetManifest = try {
                val response = api.getAssetManifest()
                val data = response.data
                if (!response.success || data == null) {
                    return@withLock fail("Failed to fetch asset manifest: server returned error")
                }
                data.toDomain()
            } catch (t: Throwable) {
                return@withLock fail("Failed to fetch asset manifest", t)
            }

            // 2. diff
            val localManifest = store.loadLocalManifest()
            val localByPath: Map<String, String> =
                localManifest?.files?.associate { it.path to it.hash } ?: emptyMap()
            val serverByPath: Map<String, String> =
                serverManifest.files.associate { it.path to it.hash }

            val toDownload: List<AssetFile> =
                serverManifest.files.filter { localByPath[it.path] != it.hash }
            val toDelete: List<String> =
                localByPath.keys.filter { it !in serverByPath }

            // 3. 순차 다운로드 + 해시 검증.
            // toDownload가 비어 있으면 Downloading(0, 0) 발행을 생략한다 — UI가 completed/total로
            // 진행률을 계산할 때 0/0으로 NaN이 나오는 걸 막기 위함.
            // TODO(perf): cold-start 시 많은 파일을 순차 요청한다. 프로파일링으로 지연이 보이면
            //   coroutineScope { ... awaitAll() } + Semaphore(4) 등으로 병렬화 고려.
            val skippedPaths = mutableListOf<String>()
            if (toDownload.isNotEmpty()) {
                _progress.value = AssetSyncProgress.Downloading(completed = 0, total = toDownload.size)
                var completed = 0
                for (file in toDownload) {
                    when (val outcome = downloadOne(file.path, file.hash)) {
                        DownloadOutcome.Success -> {
                            completed += 1
                            _progress.value = AssetSyncProgress.Downloading(completed = completed, total = toDownload.size)
                        }
                        DownloadOutcome.Skipped -> {
                            // 서버에 파일이 없음(4xx). 건너뛰고 계속 진행한다.
                            skippedPaths += file.path
                        }
                        is DownloadOutcome.Failure -> {
                            // 네트워크 오류, 5xx, 해시 불일치 — 전체 sync 실패.
                            return@withLock Result.failure(outcome.cause)
                        }
                    }
                }
            }

            if (skippedPaths.isNotEmpty()) {
                Timber.w("에셋 sync: ${skippedPaths.size}개 파일 건너뜀(서버에 없음) — $skippedPaths")
            }

            // 4. orphan 삭제
            for (path in toDelete) {
                store.deleteFile(path)
            }

            // 5. 로컬 매니페스트 갱신.
            // 건너뛴 파일은 포함하지 않는다 — 다음 sync에서 diff로 재시도되더라도
            // sync 결과는 성공으로 끝나 "에셋 동기화 실패" 메시지가 뜨지 않는다.
            val manifestToSave = if (skippedPaths.isEmpty()) {
                serverManifest
            } else {
                val skippedSet = skippedPaths.toSet()
                serverManifest.copy(files = serverManifest.files.filterNot { it.path in skippedSet })
            }
            try {
                store.saveLocalManifest(manifestToSave)
            } catch (t: Throwable) {
                return@withLock fail("Failed to save local manifest", t)
            }

            val downloadedCount = toDownload.size - skippedPaths.size
            _progress.value = AssetSyncProgress.Done(
                version = serverManifest.version,
                downloaded = downloadedCount,
                removed = toDelete.size,
                skipped = skippedPaths.size,
            )
            Result.success(Unit)
        }
    }

    /**
     * 파일 하나를 받아서 저장하고 해시를 검증한다.
     *
     * @return 성공 시 [DownloadOutcome.Success], HTTP 4xx 시 [DownloadOutcome.Skipped],
     *         네트워크 오류/5xx/해시 불일치 시 [DownloadOutcome.Failure].
     *         해시 불일치 시에는 다운로드된 파일을 삭제한다.
     */
    private suspend fun downloadOne(path: String, expectedHash: String): DownloadOutcome {
        val response = try {
            api.downloadAssetFile(path)
        } catch (t: Throwable) {
            return failOutcome("Failed to download $path", t)
        }

        if (!response.isSuccessful) {
            val code = response.code()
            // OkHttp는 errorBody도 명시적으로 close하지 않으면 GC 시점까지 connection을 잡고 있다.
            response.errorBody()?.close()
            return if (code in 400..499) {
                // 4xx — 서버에 파일이 없거나 접근 불가. 일시적 오류가 아니므로 건너뛴다.
                Timber.w("에셋 다운로드 건너뜀 (HTTP $code): $path")
                DownloadOutcome.Skipped
            } else {
                // 5xx — 서버 오류로 재시도 여지가 있으므로 전체 실패.
                failOutcome("Failed to download $path: HTTP $code")
            }
        }
        val body = response.body()
            ?: return failOutcome("Failed to download $path: empty response body")

        try {
            // ResponseBody.use는 byteStream도 함께 닫아주므로 어디서 예외가 터져도 connection이 해제된다.
            body.use { responseBody ->
                store.writeFile(path, responseBody.byteStream())
            }
        } catch (t: Throwable) {
            return failOutcome("Failed to write $path", t)
        }

        // 해시 검증
        val actualHash = store.hashOf(path)
        if (actualHash != expectedHash) {
            // 잘못된 파일을 그대로 두면 안 됨. 삭제하고 실패 반환.
            store.deleteFile(path)
            return failOutcome("Hash mismatch for $path: expected $expectedHash, got $actualHash")
        }
        return DownloadOutcome.Success
    }

    /** Error progress를 발행하고 DownloadOutcome.Failure를 만든다. */
    private fun failOutcome(message: String, cause: Throwable? = null): DownloadOutcome.Failure {
        val exception = cause ?: IllegalStateException(message)
        _progress.value = AssetSyncProgress.Error(message, cause)
        return DownloadOutcome.Failure(exception)
    }

    /** Error progress를 발행하고 Result.failure를 만든다. */
    private fun fail(message: String, cause: Throwable? = null): Result<Unit> {
        _progress.value = AssetSyncProgress.Error(message, cause)
        return Result.failure(cause ?: IllegalStateException(message))
    }
}

/**
 * 파일 하나의 다운로드 시도 결과.
 *
 * sync 루프에서 4xx(파일 없음)는 건너뜀으로 처리하고,
 * 네트워크 오류/5xx/해시 불일치는 전체 sync 실패로 전파한다.
 */
private sealed interface DownloadOutcome {
    /** 다운로드 및 해시 검증 성공. */
    data object Success : DownloadOutcome

    /** HTTP 4xx — 서버에 파일이 없음. 이 파일만 건너뛰고 계속 진행한다. */
    data object Skipped : DownloadOutcome

    /**
     * 네트워크 오류, HTTP 5xx, 해시 불일치 등 재시도 여지가 있는 실패.
     *
     * @param cause 실패 원인 예외
     */
    data class Failure(val cause: Throwable) : DownloadOutcome
}
