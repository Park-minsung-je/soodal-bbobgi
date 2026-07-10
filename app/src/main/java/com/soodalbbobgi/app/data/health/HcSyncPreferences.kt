package com.soodalbbobgi.app.data.health

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect 동기화 상태를 SharedPreferences에 저장/조회한다.
 * 변경 토큰 외에, 앱 내 삭제의 정합성을 위한 두 집합을 함께 영속한다:
 * - 삭제된 hcRecordId 블랙리스트 — 토큰 만료 폴백/수정 이벤트로 되살아나는 것 차단
 * - 서버 삭제 대기 날짜 큐 — 오프라인 삭제를 다음 동기화에서 재시도, 그동안 pull 복원 차단
 */
@Singleton
class HcSyncPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hc_sync", Context.MODE_PRIVATE)

    /** 저장된 HC 변경 토큰. 없으면 null. */
    fun getChangesToken(): String? = prefs.getString(KEY_CHANGES_TOKEN, null)

    /** HC 변경 토큰을 저장한다. */
    fun saveChangesToken(token: String) {
        prefs.edit().putString(KEY_CHANGES_TOKEN, token).apply()
    }

    /** 저장된 토큰을 삭제한다 (전체 읽기로 리셋). */
    fun clearChangesToken() {
        prefs.edit().remove(KEY_CHANGES_TOKEN).apply()
    }

    /** 앱에서 삭제한 HC 레코드 ID 집합 — 재수입 차단용. */
    fun getDeletedHcRecordIds(): Set<String> =
        prefs.getStringSet(KEY_DELETED_HC_IDS, emptySet()) ?: emptySet()

    /** 삭제한 HC 레코드 ID를 블랙리스트에 추가한다. */
    fun addDeletedHcRecordId(hcRecordId: String) {
        prefs.edit()
            .putStringSet(KEY_DELETED_HC_IDS, getDeletedHcRecordIds() + hcRecordId)
            .apply()
    }

    /** 서버 삭제가 아직 성공하지 못한 날짜 집합 — 다음 동기화에서 재시도. */
    fun getPendingServerDeletes(): Set<String> =
        prefs.getStringSet(KEY_PENDING_DELETES, emptySet()) ?: emptySet()

    /** 서버 삭제 대기 날짜를 추가한다. */
    fun addPendingServerDelete(date: String) {
        prefs.edit()
            .putStringSet(KEY_PENDING_DELETES, getPendingServerDeletes() + date)
            .apply()
    }

    /** 서버 삭제가 성공한 날짜를 대기 큐에서 제거한다. */
    fun removePendingServerDelete(date: String) {
        prefs.edit()
            .putStringSet(KEY_PENDING_DELETES, getPendingServerDeletes() - date)
            .apply()
    }

    companion object {
        private const val KEY_CHANGES_TOKEN = "hc_changes_token"
        private const val KEY_DELETED_HC_IDS = "deleted_hc_record_ids"
        private const val KEY_PENDING_DELETES = "pending_server_deletes"
    }
}
