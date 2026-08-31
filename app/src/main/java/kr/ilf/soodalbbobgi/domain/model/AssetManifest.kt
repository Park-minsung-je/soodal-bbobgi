package kr.ilf.soodalbbobgi.domain.model

/** 서버 에셋 매니페스트. 로컬과 비교해 변경분만 다운로드하는 데 사용한다. */
data class AssetManifest(
    val version: String,
    val updatedAt: Long,
    val files: List<AssetFile>,
)

/** 매니페스트의 파일 한 항목. path는 assets 루트 기준 상대경로. hash는 "sha256-{hex}" 형태. */
data class AssetFile(
    val path: String,
    val hash: String,
    val size: Long,
)
