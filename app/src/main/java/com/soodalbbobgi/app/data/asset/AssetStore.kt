package com.soodalbbobgi.app.data.asset

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.soodalbbobgi.app.domain.model.AssetManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 에셋 파일을 로컬 디스크에 저장/조회하는 저수준 I/O 레이어.
 *
 * 책임:
 * - 에셋 파일을 `context.filesDir/assets` 하위에 보관 (manifest.json 포함)
 * - 경로 탈출(`../`) 방어로 sandbox 외부 쓰기 차단
 * - 임시 파일 → 원자적 rename으로 부분 다운로드/매니페스트 손상 방지
 * - 큰 파일도 메모리에 안 올리도록 스트리밍 해시 계산
 *
 * 네트워크 다운로드/매니페스트 diff/정리(orphan cleanup)는 상위 `AssetManager`가 담당.
 */
@Singleton
class AssetStore @VisibleForTesting internal constructor(
    /** 에셋이 저장될 루트 디렉토리. context.filesDir/assets */
    val rootDir: File,
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, ASSETS_DIR_NAME))

    private val gson: Gson = Gson()

    init {
        // 첫 호출 시 루트 디렉토리를 보장한다. mkdirs 실패는 무시(상위에서 write 시 다시 시도).
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    /** path(assets 루트 기준 상대경로)에 해당하는 로컬 File. */
    fun fileFor(path: String): File = resolveSafely(path)

    /** 로컬에 해당 파일이 존재하는지. */
    fun exists(path: String): Boolean = resolveSafely(path).isFile

    /**
     * 파일 바이트를 원자적으로 쓴다. 부모 디렉토리를 자동 생성하며,
     * 임시 파일에 쓴 뒤 rename으로 교체해 중간 실패 시 깨진 파일이 노출되지 않도록 한다.
     *
     * @throws IllegalArgumentException path가 rootDir 바깥을 가리킬 때
     * @throws IOException 디렉토리 생성/쓰기/rename 실패 시
     */
    fun writeFile(path: String, bytes: ByteArray) {
        val target = resolveSafely(path)
        ensureParent(target)
        val tmp = File(target.parentFile, target.name + TMP_SUFFIX)
        try {
            tmp.outputStream().use { it.write(bytes) }
            atomicReplace(tmp, target)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /**
     * InputStream을 원자적으로 쓴다. 스트림은 호출자가 close 책임을 진다.
     *
     * @throws IllegalArgumentException path가 rootDir 바깥을 가리킬 때
     * @throws IOException 디렉토리 생성/쓰기/rename 실패 시
     */
    fun writeFile(path: String, input: InputStream) {
        val target = resolveSafely(path)
        ensureParent(target)
        val tmp = File(target.parentFile, target.name + TMP_SUFFIX)
        try {
            tmp.outputStream().use { out -> input.copyTo(out) }
            atomicReplace(tmp, target)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** 파일 삭제. 존재하지 않아도 예외 없이 noop. */
    fun deleteFile(path: String) {
        val target = resolveSafely(path)
        if (target.exists()) target.delete()
    }

    /**
     * 파일의 SHA-256 hex 다이제스트를 "sha256-{hex}" 형태로 반환.
     * 존재하지 않으면 null. 전체를 메모리에 올리지 않고 스트리밍으로 처리한다.
     */
    fun hashOf(path: String): String? {
        val target = resolveSafely(path)
        if (!target.isFile) return null
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(target.inputStream().buffered(), digest).use { dis ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (dis.read(buf) != -1) { /* 다이제스트가 자동 갱신 */ }
        }
        return "sha256-" + digest.digest().toHex()
    }

    /**
     * 로컬 manifest를 원자적으로 저장한다 (rootDir/manifest.json, Gson JSON).
     * tmp에 쓴 뒤 rename으로 교체해 중간 크래시에도 기존 매니페스트가 보존된다.
     *
     * @throws IOException 디렉토리 생성/쓰기/rename 실패 시
     */
    fun saveLocalManifest(manifest: AssetManifest) {
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            throw IOException("Failed to create rootDir: ${rootDir.absolutePath}")
        }
        val target = File(rootDir, MANIFEST_FILE_NAME)
        val tmp = File(rootDir, MANIFEST_FILE_NAME + TMP_SUFFIX)
        try {
            tmp.writeText(gson.toJson(manifest))
            atomicReplace(tmp, target)
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** 로컬 manifest 조회. 없거나 파싱 실패면 null. */
    fun loadLocalManifest(): AssetManifest? {
        val file = File(rootDir, MANIFEST_FILE_NAME)
        if (!file.isFile) return null
        return try {
            gson.fromJson(file.readText(), AssetManifest::class.java)
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    /** rootDir 전체 파일 목록 (manifest.json 제외, 상대 경로, 정렬됨). 디버깅·검증용. */
    fun listAllFiles(): List<String> {
        if (!rootDir.isDirectory) return emptyList()
        val rootPath = rootDir.canonicalPath
        return rootDir.walkTopDown()
            .filter { it.isFile }
            .map { it.canonicalPath.removePrefix(rootPath).trimStart(File.separatorChar) }
            .map { it.replace(File.separatorChar, '/') }
            .filter { it != MANIFEST_FILE_NAME }
            .toList()
            .sorted()
    }

    // ─── 내부 유틸 ────────────────────────────────────────────

    /**
     * path를 rootDir 기준으로 해석하면서 sandbox 탈출을 차단한다.
     * canonicalPath가 rootDir.canonicalPath로 시작하지 않으면 IllegalArgumentException.
     */
    private fun resolveSafely(path: String): File {
        require(path.isNotBlank()) { "path must not be blank" }
        val candidate = File(rootDir, path)
        val rootCanonical = rootDir.canonicalPath
        val candidateCanonical = candidate.canonicalPath
        // 정확히 rootDir 자체이거나 그 하위여야 한다.
        val isInside = candidateCanonical == rootCanonical ||
            candidateCanonical.startsWith(rootCanonical + File.separator)
        require(isInside) { "path escapes rootDir: $path" }
        require(candidateCanonical != rootCanonical) { "path must not be rootDir itself: $path" }
        return candidate
    }

    /** 대상 파일의 부모 디렉토리를 보장한다. 실패 시 IOException. */
    private fun ensureParent(target: File) {
        val parent = target.parentFile ?: return
        if (parent.exists()) return
        if (!parent.mkdirs() && !parent.exists()) {
            throw IOException("Failed to create parent dir: ${parent.absolutePath}")
        }
    }

    /**
     * tmp → target으로 원자적 교체. File.renameTo가 실패하면 기존 target을 지우고 한 번 더 시도한다.
     */
    private fun atomicReplace(tmp: File, target: File) {
        if (tmp.renameTo(target)) return
        // 일부 파일시스템(JVM on Windows 포함)은 대상이 존재할 때 rename 실패. 한 번 지우고 재시도.
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            throw IOException("Failed to rename ${tmp.absolutePath} -> ${target.absolutePath}")
        }
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(HEX_DIGITS[v ushr 4])
            sb.append(HEX_DIGITS[v and 0x0F])
        }
        return sb.toString()
    }

    companion object {
        private const val ASSETS_DIR_NAME = "assets"
        private const val MANIFEST_FILE_NAME = "manifest.json"
        private const val TMP_SUFFIX = ".tmp"
        private val HEX_DIGITS = "0123456789abcdef".toCharArray()
    }
}
