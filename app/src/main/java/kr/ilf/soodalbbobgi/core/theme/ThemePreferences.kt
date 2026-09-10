package kr.ilf.soodalbbobgi.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("theme")

    val themeFlow: Flow<SoodalThemeType> = context.dataStore.data.map { prefs ->
        try {
            SoodalThemeType.valueOf(prefs[key] ?: SoodalThemeType.Light.name)
        } catch (_: Exception) {
            SoodalThemeType.Light
        }
    }

    suspend fun setTheme(theme: SoodalThemeType) {
        context.dataStore.edit { it[key] = theme.name }
    }

    /** 테마 설정을 지운다 (기본 Light로 복귀) — 탈퇴·계정 전환용. */
    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
