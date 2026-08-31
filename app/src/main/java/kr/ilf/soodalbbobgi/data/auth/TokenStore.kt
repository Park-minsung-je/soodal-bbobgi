package kr.ilf.soodalbbobgi.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        "soodal_auth",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun isAccessTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return System.currentTimeMillis() >= expiresAt
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
