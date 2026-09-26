package com.xiaolong.happytalky.core

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class VoiceDirection {
    INCOMING,
    OUTGOING
}

data class VoiceMessage(
    val id: String,
    val createdAt: Long,
    val direction: VoiceDirection,
    val file: File,
    val durationMs: Long = 0L,
    val readAt: Long? = null
) {
    val isRead: Boolean
        get() =
            direction == VoiceDirection.OUTGOING ||
                readAt != null

    fun displayTime(): String =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(Date(createdAt))

    fun displayDuration(): String {
        val totalSeconds =
            (durationMs.coerceAtLeast(0L) + 500L) / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(
            Locale.getDefault(),
            minutes,
            seconds
        )
    }
}

object VoiceMessageStore {
    private const val DIRECTORY = "voice-history"
    private const val READ_PREFS = "happytalky_voice_read_state"

    fun saveOutgoing(
        context: Context,
        source: File
    ): VoiceMessage {
        val createdAt =
            System.currentTimeMillis()
        val id =
            UUID.randomUUID().toString()
        val target =
            messageFile(
                context,
                createdAt,
                VoiceDirection.OUTGOING,
                id
            )

        source.copyTo(
            target,
            overwrite = true
        )
        source.delete()

        return VoiceMessage(
            id = id,
            createdAt = createdAt,
            direction = VoiceDirection.OUTGOING,
            file = target,
            durationMs = durationMs(target),
            readAt = createdAt
        )
    }

    fun saveIncoming(
        context: Context,
        id: String,
        createdAt: Long,
        input: InputStream
    ): VoiceMessage {
        val safeCreatedAt =
            if (createdAt > 0L) {
                createdAt
            } else {
                System.currentTimeMillis()
            }

        val target =
            messageFile(
                context,
                safeCreatedAt,
                VoiceDirection.INCOMING,
                id
            )

        if (
            !target.exists() ||
            target.length() == 0L
        ) {
            target.outputStream().use { output ->
                input.copyTo(output)
            }
            clearReadState(context, id)
        }

        return VoiceMessage(
            id = id,
            createdAt = safeCreatedAt,
            direction = VoiceDirection.INCOMING,
            file = target,
            durationMs = durationMs(target),
            readAt = readAt(context, id)
        )
    }

    fun list(
        context: Context,
        limit: Int = 50
    ): List<VoiceMessage> =
        directory(context)
            .listFiles()
            .orEmpty()
            .mapNotNull {
                parse(
                    context,
                    it
                )
            }
            .sortedByDescending { it.createdAt }
            .take(limit)

    fun unreadCount(context: Context): Int =
        directory(context)
            .listFiles()
            .orEmpty()
            .mapNotNull(::parseName)
            .count {
                it.direction ==
                    VoiceDirection.INCOMING &&
                    readAt(
                        context,
                        it.id
                    ) == null
            }

    fun markRead(
        context: Context,
        id: String,
        at: Long = System.currentTimeMillis()
    ) {
        readPrefs(context)
            .edit()
            .putLong(id, at)
            .apply()
    }

    fun delete(
        context: Context,
        ids: Set<String>
    ): Int {
        if (ids.isEmpty()) return 0

        var deleted = 0

        directory(context)
            .listFiles()
            .orEmpty()
            .mapNotNull {
                parse(
                    context,
                    it
                )
            }
            .filter { it.id in ids }
            .forEach { message ->
                if (message.file.delete()) {
                    deleted += 1
                    clearReadState(
                        context,
                        message.id
                    )
                }
            }

        return deleted
    }

    fun clear(context: Context): Int {
        var deleted = 0

        directory(context)
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile &&
                    it.extension.lowercase() ==
                        "m4a"
            }
            .forEach { file ->
                if (file.delete()) {
                    deleted += 1
                }
            }

        readPrefs(context)
            .edit()
            .clear()
            .apply()

        return deleted
    }

    private fun messageFile(
        context: Context,
        createdAt: Long,
        direction: VoiceDirection,
        id: String
    ): File {
        val token =
            if (
                direction ==
                    VoiceDirection.INCOMING
            ) {
                "in"
            } else {
                "out"
            }

        return File(
            directory(context),
            "$createdAt-$token-$id.m4a"
        )
    }

    private fun directory(
        context: Context
    ): File =
        File(
            context.filesDir,
            DIRECTORY
        ).apply {
            mkdirs()
        }

    private fun readPrefs(
        context: Context
    ) =
        context.getSharedPreferences(
            READ_PREFS,
            Context.MODE_PRIVATE
        )

    private fun readAt(
        context: Context,
        id: String
    ): Long? {
        val value =
            readPrefs(context)
                .getLong(
                    id,
                    0L
                )

        return value
            .takeIf {
                it > 0L
            }
    }

    private fun clearReadState(
        context: Context,
        id: String
    ) {
        readPrefs(context)
            .edit()
            .remove(id)
            .apply()
    }

    private data class ParsedName(
        val createdAt: Long,
        val direction: VoiceDirection,
        val id: String
    )

    private fun parseName(
        file: File
    ): ParsedName? {
        if (
            !file.isFile ||
            file.extension.lowercase() !=
                "m4a"
        ) {
            return null
        }

        val stem =
            file.name.removeSuffix(".m4a")
        val first =
            stem.indexOf('-')

        if (first <= 0) return null

        val second =
            stem.indexOf(
                '-',
                first + 1
            )

        if (
            second <= first + 1
        ) {
            return null
        }

        val createdAt =
            stem.substring(
                0,
                first
            ).toLongOrNull()
                ?: return null

        val direction =
            when (
                stem.substring(
                    first + 1,
                    second
                )
            ) {
                "in" ->
                    VoiceDirection.INCOMING

                "out" ->
                    VoiceDirection.OUTGOING

                else ->
                    return null
            }

        val id =
            stem.substring(
                second + 1
            )

        if (id.isBlank()) {
            return null
        }

        return ParsedName(
            createdAt = createdAt,
            direction = direction,
            id = id
        )
    }

    private fun parse(
        context: Context,
        file: File
    ): VoiceMessage? {
        val parsed =
            parseName(file)
                ?: return null

        return VoiceMessage(
            id = parsed.id,
            createdAt = parsed.createdAt,
            direction = parsed.direction,
            file = file,
            durationMs = durationMs(file),
            readAt =
                if (
                    parsed.direction ==
                        VoiceDirection.OUTGOING
                ) {
                    parsed.createdAt
                } else {
                    readAt(
                        context,
                        parsed.id
                    )
                }
        )
    }

    private fun durationMs(
        file: File
    ): Long {
        val retriever =
            MediaMetadataRetriever()

        return try {
            retriever.setDataSource(
                file.absolutePath
            )
            retriever
                .extractMetadata(
                    MediaMetadataRetriever
                        .METADATA_KEY_DURATION
                )
                ?.toLongOrNull()
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }
}
