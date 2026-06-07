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

    companion object {
        private const val KEY_CHANGES_TOKEN = "hc_changes_token"
    }
}
