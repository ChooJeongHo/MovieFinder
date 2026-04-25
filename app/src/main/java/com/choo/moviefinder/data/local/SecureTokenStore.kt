package com.choo.moviefinder.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "tmdb_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, accountId: String, sessionId: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_ACCOUNT_ID, accountId)
            .putString(KEY_SESSION_ID, sessionId)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getAccountId(): String? = prefs.getString(KEY_ACCOUNT_ID, null)
    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_SESSION_ID)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "tmdb_access_token"
        private const val KEY_ACCOUNT_ID = "tmdb_account_id"
        private const val KEY_SESSION_ID = "tmdb_session_id"
    }
}
