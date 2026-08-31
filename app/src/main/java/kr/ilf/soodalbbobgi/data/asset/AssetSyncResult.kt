package kr.ilf.soodalbbobgi.data.asset

/**
 * 에셋 동기화 진행/결과 상태.
 *
 * Splash UI가 [AssetManager.progress]를 collect해서 사용자에게 진행률을 표시한다.
 * 라이프사이클: Idle -> FetchingManifest -> Downloading(0,N) ... Downloading(N,N) -> Done.
 * 실패 시 임의 단계에서 Error로 전이.
 */
sealed interface AssetSyncProgress {
    /** 동기화 시작 전 또는 끝난 직후 초기 상태. */
    data object Idle : AssetSyncProgress

    /** 서버 매니페스트 조회 중. */
    data object FetchingManifest : AssetSyncProgress

    /**
     * 다운로드 진행 중.
     * @param completed 지금까지 받은 파일 개수
     * @param total 받아야 하는 전체 파일 개수
     */
    data class Downloading(val completed: Int, val total: Int) : AssetSyncProgress

    /**
     * 동기화 성공.
     * @param version 새로 적용된 매니페스트 버전
     * @param downloaded 이번 동기화로 새로 다운받은 파일 수
     * @param removed 이번 동기화로 삭제된 로컬 orphan 파일 수
     * @param skipped HTTP 4xx 응답으로 건너뛴 파일 수 (서버에 없는 파일)
     */
    data class Done(
        val version: String,
        val downloaded: Int,
        val removed: Int,
        val skipped: Int = 0,
    ) : AssetSyncProgress

    /**
     * 실패. 사용자에게 보여줄 메시지(가능하면)와 원본 예외를 포함한다.
     * @param message UI에 표시할 짧은 메시지
     * @param cause 원본 예외 (있다면)
     */
    data class Error(val message: String, val cause: Throwable? = null) : AssetSyncProgress
}
