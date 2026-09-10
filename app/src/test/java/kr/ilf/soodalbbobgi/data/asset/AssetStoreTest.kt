package kr.ilf.soodalbbobgi.data.asset

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.domain.model.AssetFile
import kr.ilf.soodalbbobgi.domain.model.AssetManifest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class AssetStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: AssetStore
    private lateinit var rootDir: File

    @Before
    fun setUp() {
        rootDir = File(tempFolder.root, "assets")
        store = AssetStore(rootDir)
    }

    // ─── 기본 read/write ─────────────────────────────────────

    @Test
    fun `writeFile bytes then exists returns true`() {
        store.writeFile("characters/otter.png", "data".toByteArray())

        assertThat(store.exists("characters/otter.png")).isTrue()
    }

    @Test
    fun `writeFile bytes round trip preserves content`() {
        val payload = "hello world".toByteArray()

        store.writeFile("characters/otter.png", payload)

        assertThat(store.fileFor("characters/otter.png").readBytes()).isEqualTo(payload)
    }

    @Test
    fun `writeFile from InputStream round trip preserves content`() {
        val payload = "binary-content-here".toByteArray()

        store.writeFile("backgrounds/pool.png", ByteArrayInputStream(payload))

        assertThat(store.fileFor("backgrounds/pool.png").readBytes()).isEqualTo(payload)
    }

    @Test
    fun `writeFile creates nested parent directories`() {
        store.writeFile("a/b/c/d.png", "x".toByteArray())

        assertThat(File(rootDir, "a/b/c/d.png").isFile).isTrue()
    }

    @Test
    fun `writeFile leaves no tmp file on success`() {
        store.writeFile("characters/otter.png", "data".toByteArray())

        val tmpFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".tmp") }
            .toList()
        assertThat(tmpFiles).isEmpty()
    }

    @Test
    fun `writeFile overwrites existing file`() {
        store.writeFile("a.txt", "first".toByteArray())
        store.writeFile("a.txt", "second".toByteArray())

        assertThat(store.fileFor("a.txt").readText()).isEqualTo("second")
    }

    // ─── exists / delete ─────────────────────────────────────

    @Test
    fun `exists returns false for missing file`() {
        assertThat(store.exists("nope.png")).isFalse()
    }

    @Test
    fun `deleteFile removes existing file`() {
        store.writeFile("a.txt", "x".toByteArray())

        store.deleteFile("a.txt")

        assertThat(store.exists("a.txt")).isFalse()
    }

    @Test
    fun `deleteFile on missing path does not throw`() {
        store.deleteFile("never-existed.png")
        // 도달했다는 것 자체가 성공
    }

    // ─── hashOf ──────────────────────────────────────────────

    @Test
    fun `hashOf returns sha256 prefix for empty file`() {
        store.writeFile("empty.bin", ByteArray(0))

        // SHA-256 of empty string
        val expected = "sha256-e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assertThat(store.hashOf("empty.bin")).isEqualTo(expected)
    }

    @Test
    fun `hashOf returns correct digest for known content`() {
        store.writeFile("greeting.txt", "hello".toByteArray())

        // SHA-256 of "hello"
        val expected = "sha256-2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        assertThat(store.hashOf("greeting.txt")).isEqualTo(expected)
    }

    @Test
    fun `hashOf returns null when file missing`() {
        assertThat(store.hashOf("nope.bin")).isNull()
    }

    @Test
    fun `hashOf handles large content streaming`() {
        // DEFAULT_BUFFER_SIZE(8KB)를 넘는 입력으로 스트리밍 경로 확인.
        val big = ByteArray(64 * 1024) { (it % 251).toByte() }
        store.writeFile("big.bin", big)

        val hash = store.hashOf("big.bin")

        assertThat(hash).isNotNull()
        assertThat(hash!!).startsWith("sha256-")
        // 64 hex chars after prefix
        assertThat(hash.length).isEqualTo("sha256-".length + 64)
    }

    // ─── manifest round trip ─────────────────────────────────

    @Test
    fun `saveLocalManifest and loadLocalManifest round trip`() {
        val manifest = AssetManifest(
            version = "1.2.0",
            updatedAt = 1_717_000_000_000L,
            files = listOf(
                AssetFile("characters/otter.png", "sha256-aaa", 1024L),
                AssetFile("backgrounds/pool.png", "sha256-bbb", 2048L),
            ),
        )

        store.saveLocalManifest(manifest)
        val loaded = store.loadLocalManifest()

        assertThat(loaded).isEqualTo(manifest)
    }

    @Test
    fun `loadLocalManifest returns null when missing`() {
        assertThat(store.loadLocalManifest()).isNull()
    }

    @Test
    fun `loadLocalManifest returns null on corrupt json`() {
        File(rootDir, "manifest.json").writeText("{not valid json")

        assertThat(store.loadLocalManifest()).isNull()
    }

    @Test
    fun `saveLocalManifest leaves no tmp file on success`() {
        store.saveLocalManifest(AssetManifest("v", 0L, emptyList()))

        val tmp = File(rootDir, "manifest.json.tmp")
        assertThat(tmp.exists()).isFalse()
    }

    @Test
    fun `saveLocalManifest preserves prior manifest when tmp orphan exists`() {
        // 기존 manifest가 정상 저장된 상태에서, 어떤 사고로 stale tmp가 남아있다고 가정.
        val first = AssetManifest("v1", 1L, emptyList())
        store.saveLocalManifest(first)
        File(rootDir, "manifest.json.tmp").writeText("orphan-from-prior-crash")

        // 새 저장이 정상 완료되어야 한다.
        val second = AssetManifest("v2", 2L, emptyList())
        store.saveLocalManifest(second)

        assertThat(store.loadLocalManifest()).isEqualTo(second)
        assertThat(File(rootDir, "manifest.json.tmp").exists()).isFalse()
    }

    // ─── path traversal ──────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `writeFile bytes rejects parent traversal`() {
        store.writeFile("../etc/passwd", "x".toByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `writeFile stream rejects parent traversal`() {
        store.writeFile("../etc/passwd", ByteArrayInputStream("x".toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fileFor rejects nested parent traversal`() {
        store.fileFor("subdir/../../etc/passwd")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `exists rejects parent traversal`() {
        store.exists("../outside.png")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deleteFile rejects parent traversal`() {
        store.deleteFile("../outside.png")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hashOf rejects parent traversal`() {
        store.hashOf("../outside.png")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveSafely rejects blank path`() {
        store.fileFor("")
    }

    @Test
    fun `nested subdir relative path resolves inside root`() {
        // 합법적인 하위 경로는 통과.
        val f = store.fileFor("a/b/c.png")

        assertThat(f.canonicalPath).startsWith(rootDir.canonicalPath)
    }

    // ─── listAllFiles ────────────────────────────────────────

    @Test
    fun `listAllFiles returns empty when rootDir is empty`() {
        assertThat(store.listAllFiles()).isEmpty()
    }

    @Test
    fun `listAllFiles excludes manifest json and sorts results`() {
        store.writeFile("z/last.png", "1".toByteArray())
        store.writeFile("a/first.png", "2".toByteArray())
        store.writeFile("m/middle.png", "3".toByteArray())
        store.saveLocalManifest(AssetManifest("v", 0L, emptyList()))

        val files = store.listAllFiles()

        assertThat(files).containsExactly(
            "a/first.png",
            "m/middle.png",
            "z/last.png",
        ).inOrder()
    }

    @Test
    fun `listAllFiles uses forward slashes regardless of OS separator`() {
        store.writeFile("nested/dir/x.png", "x".toByteArray())

        val files = store.listAllFiles()

        assertThat(files).containsExactly("nested/dir/x.png")
    }

    // ─── rootDir ─────────────────────────────────────────────

    @Test
    fun `rootDir is created on initialization`() {
        assertThat(rootDir.isDirectory).isTrue()
    }
}
