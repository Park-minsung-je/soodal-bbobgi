package com.soodalbbobgi.app.data.remote.dto

import com.soodalbbobgi.app.domain.model.AssetFile
import com.soodalbbobgi.app.domain.model.AssetManifest

/** 서버 DTO를 에셋 파일 도메인 모델로 변환한다. */
internal fun ServerAssetFile.toDomain() = AssetFile(
    path = path,
    hash = hash,
    size = size,
)

/** 서버 매니페스트 DTO를 도메인 모델로 변환한다. files 항목도 함께 매핑된다. */
internal fun AssetManifestData.toDomain() = AssetManifest(
    version = version,
    updatedAt = updatedAt,
    files = files.map { it.toDomain() },
)
