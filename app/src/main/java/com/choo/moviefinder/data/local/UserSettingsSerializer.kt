package com.choo.moviefinder.data.local

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object UserSettingsSerializer : Serializer<UserSettings> {

    override val defaultValue: UserSettings = UserSettings()

    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            Json.decodeFromString(
                UserSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to read UserSettings, returning default")
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(UserSettings.serializer(), t).encodeToByteArray()
        )
    }
}
