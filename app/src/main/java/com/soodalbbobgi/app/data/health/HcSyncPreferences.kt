package com.soodalbbobgi.app.data.health

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect 변경 토큰을 SharedPreferences에 저장/조회한다.
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

    /** 마지막으로 재계산을 마친 페이스 알고리즘 버전. 없으면 0. */
    fun getAlgoVersion(): Int = prefs.getInt(KEY_ALGO_VERSION, 0)

    /** 페이스 알고리즘 버전을 저장한다 (재계산 완료 표시). */
    fun saveAlgoVersion(version: Int) {
        prefs.edit().putInt(KEY_ALGO_VERSION, version).apply()
    }

    companion object {
        private const val KEY_CHANGES_TOKEN = "hc_changes_token"
        private const val KEY_ALGO_VERSION = "hc_algo_version"
    }
}
