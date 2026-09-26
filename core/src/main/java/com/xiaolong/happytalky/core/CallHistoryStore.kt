package com.xiaolong.happytalky.core

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CallDirection {
    INCOMING,
    OUTGOING
}

enum class CallOutcome {
    COMPLETED,
    DECLINED_BY_ME,
    DECLINED_BY_PEER,
    NO_ANSWER,
    MISSED,
    CANCELLED_BY_ME,
    CANCELLED_BY_PEER,
    BUSY,
    FAILED,
    DISCONNECTED
}

data class CallHistoryEntry(
    val id: String,
    val callId: String,
    val occurredAt: Long,
    val direction: CallDirection,
    val outcome: CallOutcome,
    val durationMs: Long = 0L
) {
    fun displayTime(): String =
        SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(Date(occurredAt))

    fun displayDuration(): String {
        val totalSeconds =
            (durationMs.coerceAtLeast(0L) + 500L) /
                1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(
            Locale.getDefault(),
            minutes,
            seconds
        )
    }

    fun shortLabel(): String =
        when (outcome) {
            CallOutcome.COMPLETED ->
                if (durationMs > 0L) {
                    "Call · ${displayDuration()}"
                } else {
                    "Call ended"
                }

            CallOutcome.DECLINED_BY_ME ->
                "Declined call"

            CallOutcome.DECLINED_BY_PEER ->
                "Call declined"

            CallOutcome.NO_ANSWER ->
                "No answer"

            CallOutcome.MISSED ->
                "Missed call"

            CallOutcome.CANCELLED_BY_ME ->
                "Cancelled call"

            CallOutcome.CANCELLED_BY_PEER ->
                "Caller cancelled"

            CallOutcome.BUSY ->
                "Busy"

            CallOutcome.FAILED ->
                "Call failed"

            CallOutcome.DISCONNECTED ->
                "Call disconnected"
        }
}

object CallHistoryStore {
    private const val FILE_NAME =
        "call-history.jsonl"

    @Synchronized
    fun append(
        context: Context,
        callId: String,
        direction: CallDirection,
        outcome: CallOutcome,
        startedAt: Long = 0L,
        endedAt: Long =
            System.currentTimeMillis()
    ): CallHistoryEntry {
        list(
            context,
            limit = 100
        ).firstOrNull {
            it.callId == callId
        }?.let {
            return it
        }

        val duration =
            if (
                startedAt > 0L &&
                endedAt >= startedAt
            ) {
                endedAt - startedAt
            } else {
                0L
            }

        val entry =
            CallHistoryEntry(
                id =
                    UUID.randomUUID()
                        .toString(),
                callId = callId,
                occurredAt = endedAt,
                direction = direction,
                outcome = outcome,
                durationMs = duration
            )

        val file = historyFile(context)
        file.parentFile?.mkdirs()
        file.appendText(
            encode(entry) + "\n"
        )

        trim(context)
        return entry
    }

    @Synchronized
    fun list(
        context: Context,
        limit: Int = 50
    ): List<CallHistoryEntry> {
        val file = historyFile(context)
        if (!file.exists()) {
            return emptyList()
        }

        return file
            .readLines()
            .asReversed()
            .mapNotNull(::decode)
            .take(limit)
    }

    @Synchronized
    fun clear(context: Context) {
        historyFile(context).delete()
    }

    private fun trim(context: Context) {
        val file = historyFile(context)
        if (!file.exists()) return

        val lines =
            file.readLines()
        if (lines.size <= 100) {
            return
        }

        file.writeText(
            lines
                .takeLast(100)
                .joinToString(
                    separator = "\n",
                    postfix = "\n"
                )
        )
    }

    private fun historyFile(
        context: Context
    ): File =
        File(
            context.filesDir,
            FILE_NAME
        )

    private fun encode(
        entry: CallHistoryEntry
    ): String =
        JSONObject()
            .put("id", entry.id)
            .put("callId", entry.callId)
            .put(
                "occurredAt",
                entry.occurredAt
            )
            .put(
                "direction",
                entry.direction.name
            )
            .put(
                "outcome",
                entry.outcome.name
            )
            .put(
                "durationMs",
                entry.durationMs
            )
            .toString()

    private fun decode(
        raw: String
    ): CallHistoryEntry? =
        runCatching {
            val obj =
                JSONObject(raw)

            CallHistoryEntry(
                id =
                    obj.getString("id"),
                callId =
                    obj.getString(
                        "callId"
                    ),
                occurredAt =
                    obj.getLong(
                        "occurredAt"
                    ),
                direction =
                    CallDirection.valueOf(
                        obj.getString(
                            "direction"
                        )
                    ),
                outcome =
                    CallOutcome.valueOf(
                        obj.getString(
                            "outcome"
                        )
                    ),
                durationMs =
                    obj.optLong(
                        "durationMs",
                        0L
                    )
            )
        }.getOrNull()
}
