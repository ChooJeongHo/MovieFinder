package com.choo.moviefinder.data.local

import androidx.datastore.core.Serializer
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object UserSettingsSerializer : Serializer<UserSettings> {

    override val defaultValue: UserSettings = UserSettings()

    // InputStream에서 JSON을 읽어 UserSettings로 역직렬화
    override suspend fun readFrom(input: InputStream): UserSettings {
        return try {
            Json.decodeFromString(
                UserSettings.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "UserSettings 읽기 실패, 기본값 반환")
            defaultValue
        }
    }

    // UserSettings를 JSON으로 직렬화하여 OutputStream에 기록
    override suspend fun writeTo(t: UserSettings, output: OutputStream) {
        output.write(
            Json.encodeToString(UserSettings.serializer(), t).encodeToByteArray()
        )
    }
}
