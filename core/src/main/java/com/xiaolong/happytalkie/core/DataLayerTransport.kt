package com.xiaolong.happytalkie.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class DataLayerTransport(context: Context) {
    private val appContext = context.applicationContext
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val dataClient = Wearable.getDataClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()

    fun sendSignal(path: String, callId: String, callback: (Boolean) -> Unit) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    mainHandler.post { callback(false) }
                    return@addOnSuccessListener
                }

                val remaining = AtomicInteger(nodes.size)
                val anySuccess = AtomicBoolean(false)

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        path,
                        callId.toByteArray(Charsets.UTF_8)
                    ).addOnSuccessListener {
                        anySuccess.set(true)
                        if (remaining.decrementAndGet() == 0) {
                            mainHandler.post { callback(anySuccess.get()) }
                        }
                    }.addOnFailureListener {
                        if (remaining.decrementAndGet() == 0) {
                            mainHandler.post { callback(anySuccess.get()) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                mainHandler.post { callback(false) }
            }
    }

    fun queueVoice(
        file: File,
        callId: String?,
        role: EndpointRole,
        callback: (Boolean) -> Unit
    ) {
        io.execute {
            try {
                val bytes = file.readBytes()
                val id = UUID.randomUUID().toString()
                val mapRequest = PutDataMapRequest.create(Protocol.VOICE_PREFIX + id)
                mapRequest.dataMap.putString(Protocol.KEY_ID, id)
                mapRequest.dataMap.putString(Protocol.KEY_ORIGIN, role.wireValue)
                mapRequest.dataMap.putString(Protocol.KEY_CALL_ID, callId ?: "")
                mapRequest.dataMap.putLong(Protocol.KEY_CREATED_AT, System.currentTimeMillis())
                mapRequest.dataMap.putAsset(Protocol.KEY_AUDIO, Asset.createFromBytes(bytes))

                dataClient.putDataItem(mapRequest.asPutDataRequest().setUrgent())
                    .addOnSuccessListener {
                        file.delete()
                        mainHandler.post { callback(true) }
                    }
                    .addOnFailureListener {
                        mainHandler.post { callback(false) }
                    }
            } catch (_: Exception) {
                mainHandler.post { callback(false) }
            }
        }
    }
}
