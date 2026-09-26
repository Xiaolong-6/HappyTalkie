package com.xiaolong.happytalkie.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object LiveCallAudio {
    private const val TAG = "HappyTalkieLiveAudio"
    private val executor = Executors.newCachedThreadPool()
    private val running = AtomicBoolean(false)
    private val starting = AtomicBoolean(false)

    @Volatile private var channel: ChannelClient.Channel? = null
    @Volatile private var input: InputStream? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var player: AudioTrack? = null
    @Volatile private var echoCanceler: AcousticEchoCanceler? = null
    @Volatile private var noiseSuppressor: NoiseSuppressor? = null
    @Volatile private var previousAudioMode: Int? = null
    @Volatile private var communicationDeviceRequested = false

    fun isRunning(): Boolean = running.get()
    fun isStarting(): Boolean = starting.get()

    fun startOutgoing(
        context: Context,
        callId: String,
        callback: (Boolean) -> Unit = {}
    ) {
        if (running.get() || starting.get()) {
            callback(running.get())
            return
        }
        starting.set(true)

        val appContext = context.applicationContext
        val nodeClient = Wearable.getNodeClient(appContext)
        val channelClient = Wearable.getChannelClient(appContext)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull()
                if (node == null) {
                    starting.set(false)
                    callback(false)
                    return@addOnSuccessListener
                }

                channelClient.openChannel(
                    node.id,
                    Protocol.CALL_AUDIO_PREFIX + callId
                ).addOnSuccessListener { opened ->
                    executor.execute {
                        val attached = attach(appContext, opened)
                        starting.set(false)
                        callback(attached)
                    }
                }.addOnFailureListener {
                    starting.set(false)
                    callback(false)
                }
            }
            .addOnFailureListener {
                starting.set(false)
                callback(false)
            }
    }

    fun attachIncoming(context: Context, incoming: ChannelClient.Channel) {
        if (!incoming.path.startsWith(Protocol.CALL_AUDIO_PREFIX)) return
        if (running.get() || starting.get()) {
            Wearable.getChannelClient(context).close(incoming)
            return
        }

        starting.set(true)
        executor.execute {
            attach(context.applicationContext, incoming)
            starting.set(false)
        }
    }

    private fun attach(context: Context, opened: ChannelClient.Channel): Boolean {
        return try {
            val channelClient = Wearable.getChannelClient(context)
            val remoteInput = Tasks.await(channelClient.getInputStream(opened))
            val remoteOutput = Tasks.await(channelClient.getOutputStream(opened))

            val manager = context.getSystemService(AudioManager::class.java)
            configureCommunicationAudio(manager)

            val sampleRate = Protocol.AUDIO_SAMPLE_RATE
            val recordMin = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val playMin = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(2048, recordMin, playMin)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (
                audioRecord.state != AudioRecord.STATE_INITIALIZED ||
                audioTrack.state != AudioTrack.STATE_INITIALIZED
            ) {
                runCatching { audioRecord.release() }
                runCatching { audioTrack.release() }
                runCatching { remoteInput.close() }
                runCatching { remoteOutput.close() }
                runCatching { channelClient.close(opened) }
                restoreCommunicationAudio(manager)
                return false
            }

            val aecAvailable = AcousticEchoCanceler.isAvailable()
            val echo =
                if (aecAvailable)
                    runCatching {
                        AcousticEchoCanceler.create(audioRecord.audioSessionId)
                    }.getOrNull()
                else null
            val nsAvailable = NoiseSuppressor.isAvailable()
            val noise =
                if (nsAvailable)
                    runCatching {
                        NoiseSuppressor.create(audioRecord.audioSessionId)
                    }.getOrNull()
                else null

            if (echo != null && !echo.enabled && echo.hasControl()) {
                runCatching { echo.enabled = true }
            }
            if (noise != null && !noise.enabled && noise.hasControl()) {
                runCatching { noise.enabled = true }
            }

            Log.i(
                TAG,
                "live audio configured " +
                    "mode=${manager?.mode} " +
                    "route=${communicationRoute(manager)} " +
                    "aecAvailable=$aecAvailable " +
                    "aecCreated=${echo != null} " +
                    "aecEnabled=${echo?.enabled == true} " +
                    "aecControl=${echo?.hasControl() == true} " +
                    "nsAvailable=$nsAvailable " +
                    "nsEnabled=${noise?.enabled == true}"
            )

            synchronized(this) {
                channel = opened
                input = remoteInput
                output = remoteOutput
                recorder = audioRecord
                player = audioTrack
                echoCanceler = echo
                noiseSuppressor = noise
                running.set(true)
            }

            audioTrack.play()
            audioRecord.startRecording()

            StateStore.setStatus(context, "Live call")
            EventBus.notifyStateChanged(context)

            executor.execute { captureLoop(context, audioRecord, remoteOutput, bufferSize) }
            executor.execute { playbackLoop(context, remoteInput, audioTrack, bufferSize) }
            true
        } catch (_: Exception) {
            stop(context)
            false
        }
    }

    private fun captureLoop(
        context: Context,
        audioRecord: AudioRecord,
        stream: OutputStream,
        bufferSize: Int
    ) {
        val buffer = ByteArray(bufferSize)
        try {
            while (running.get()) {
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read > 0) {
                    stream.write(buffer, 0, read)
                    stream.flush()
                } else if (read < 0) {
                    break
                }
            }
        } catch (_: Exception) {
            // The peer may have disconnected.
        } finally {
            if (running.get()) {
                disconnected(context)
            }
        }
    }

    private fun playbackLoop(
        context: Context,
        stream: InputStream,
        audioTrack: AudioTrack,
        bufferSize: Int
    ) {
        val buffer = ByteArray(bufferSize)
        try {
            while (running.get()) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read > 0) audioTrack.write(buffer, 0, read)
            }
        } catch (_: Exception) {
            // The peer may have disconnected.
        } finally {
            if (running.get()) {
                disconnected(context)
            }
        }
    }

    private fun disconnected(context: Context) {
        stop(context, closeChannel = false)
        StateStore.clearCallState(context)
        StateStore.setStatus(context, "Call disconnected")
        EventBus.notifyStateChanged(context)
        LiveCallService.stop(context)
    }

    private fun configureCommunicationAudio(manager: AudioManager?) {
        if (manager == null) return

        previousAudioMode = manager.mode
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
        communicationDeviceRequested = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker =
                manager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
            if (speaker != null) {
                communicationDeviceRequested =
                    runCatching { manager.setCommunicationDevice(speaker) }
                        .getOrDefault(false)
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { manager.isSpeakerphoneOn = true }
        }
    }

    private fun clearCommunicationAudio(manager: AudioManager?) {
        if (manager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (communicationDeviceRequested) {
                runCatching { manager.clearCommunicationDevice() }
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { manager.isSpeakerphoneOn = false }
        }
        communicationDeviceRequested = false
    }

    private fun restoreCommunicationAudio(manager: AudioManager?) {
        clearCommunicationAudio(manager)
        previousAudioMode?.let { oldMode ->
            runCatching { manager?.mode = oldMode }
        }
        previousAudioMode = null
    }

    private fun communicationRoute(manager: AudioManager?): String {
        if (manager == null) return "none"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.communicationDevice?.let { device ->
                "${device.type}:${device.productName}"
            } ?: "default"
        } else {
            @Suppress("DEPRECATION")
            if (manager.isSpeakerphoneOn) "legacy-speaker" else "legacy-default"
        }
    }

    fun stop(context: Context, closeChannel: Boolean = true) {
        val wasRunning = running.getAndSet(false)
        starting.set(false)

        val localRecorder: AudioRecord?
        val localPlayer: AudioTrack?
        val localInput: InputStream?
        val localOutput: OutputStream?
        val localChannel: ChannelClient.Channel?

        synchronized(this) {
            localRecorder = recorder
            localPlayer = player
            localInput = input
            localOutput = output
            localChannel = channel

            recorder = null
            player = null
            input = null
            output = null
            channel = null
        }

        runCatching { localRecorder?.stop() }
        runCatching { localPlayer?.stop() }
        runCatching { localInput?.close() }
        runCatching { localOutput?.close() }
        runCatching { echoCanceler?.release() }
        runCatching { noiseSuppressor?.release() }
        echoCanceler = null
        noiseSuppressor = null
        runCatching { localRecorder?.release() }
        runCatching { localPlayer?.release() }

        if (closeChannel && localChannel != null) {
            runCatching {
                Wearable.getChannelClient(context.applicationContext)
                    .close(localChannel)
            }
        }

        val manager = context.getSystemService(AudioManager::class.java)
        restoreCommunicationAudio(manager)

        if (wasRunning) {
            EventBus.notifyStateChanged(context)
        }
    }
}
