package com.soodalbbobgi.app.data.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AssetMapperTest {

    @Test
    fun `ServerAssetFile toDomain copies path hash size`() {
        val dto = ServerAssetFile(
            path = "characters/otter_pink.png",
            hash = "sha256-abc123",
            size = 4096L,
        )

        val domain = dto.toDomain()

        assertThat(domain.path).isEqualTo("characters/otter_pink.png")
        assertThat(domain.hash).isEqualTo("sha256-abc123")
        assertThat(domain.size).isEqualTo(4096L)
    }

    @Test
    fun `AssetManifestData toDomain maps version updatedAt and files`() {
        val dto = AssetManifestData(
            version = "1.2.0",
            updatedAt = 1_717_000_000_000L,
            files = listOf(
                ServerAssetFile("backgrounds/pool.png", "sha256-aaa", 1024L),
                ServerAssetFile("borders/gold.png", "sha256-bbb", 2048L),
            ),
        )

        val domain = dto.toDomain()

        assertThat(domain.version).isEqualTo("1.2.0")
        assertThat(domain.updatedAt).isEqualTo(1_717_000_000_000L)
        assertThat(domain.files).hasSize(2)
        assertThat(domain.files[0].path).isEqualTo("backgrounds/pool.png")
        assertThat(domain.files[0].hash).isEqualTo("sha256-aaa")
        assertThat(domain.files[0].size).isEqualTo(1024L)
        assertThat(domain.files[1].path).isEqualTo("borders/gold.png")
        assertThat(domain.files[1].hash).isEqualTo("sha256-bbb")
        assertThat(domain.files[1].size).isEqualTo(2048L)
    }

    @Test
    fun `AssetManifestData toDomain handles empty files list`() {
        val dto = AssetManifestData(
            version = "0.0.1",
            updatedAt = 0L,
            files = emptyList(),
        )

        val domain = dto.toDomain()

        assertThat(domain.version).isEqualTo("0.0.1")
        assertThat(domain.updatedAt).isEqualTo(0L)
        assertThat(domain.files).isEmpty()
    }
}
