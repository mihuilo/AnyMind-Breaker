package com.anymindbreaker.core.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.anymindbreaker.core.common.game.GameResult
import com.anymindbreaker.core.common.game.GameState
import com.anymindbreaker.core.datastore.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class FeedbackEvent {
    CORRECT,
    ERROR,
    COMPLETE,
    FAILED,
}

interface GameFeedback {
    fun play(event: FeedbackEvent)
}

/** Sounds are short synthesized tones, so the app ships no audio files. */
class AndroidGameFeedback(
    context: Context,
    settings: SettingsRepository,
    scope: CoroutineScope,
) : GameFeedback {

    private class Note(val frequency: Double, val millis: Int)

    @Volatile
    private var soundEnabled = false

    @Volatile
    private var vibrationEnabled = false

    private val audioExecutor = Executors.newSingleThreadExecutor()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    init {
        scope.launch {
            settings.settings.collect {
                soundEnabled = it.soundEnabled
                vibrationEnabled = it.vibrationEnabled
            }
        }
    }

    override fun play(event: FeedbackEvent) {
        if (soundEnabled) {
            val notes = when (event) {
                FeedbackEvent.CORRECT -> listOf(Note(880.0, 60))
                FeedbackEvent.ERROR -> listOf(Note(196.0, 160))
                FeedbackEvent.COMPLETE -> listOf(Note(523.25, 110), Note(659.25, 110), Note(783.99, 110), Note(1046.5, 220))
                FeedbackEvent.FAILED -> listOf(Note(261.63, 180), Note(196.0, 320))
            }
            audioExecutor.execute { playTones(notes) }
        }
        if (vibrationEnabled) {
            // A correct answer is not vibrated: vibration is kept for errors and endings.
            val millis = when (event) {
                FeedbackEvent.CORRECT -> 0L
                FeedbackEvent.ERROR -> 60L
                FeedbackEvent.COMPLETE -> 120L
                FeedbackEvent.FAILED -> 250L
            }
            if (millis > 0) vibrate(millis)
        }
    }

    private fun vibrate(millis: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: RuntimeException) {
            // Feedback is optional; a device without a working vibrator must not break the game.
        }
    }

    private fun playTones(notes: List<Note>) {
        val samples = ShortArray(notes.sumOf { it.millis * SAMPLE_RATE / 1000 })
        var offset = 0
        for (note in notes) {
            val length = note.millis * SAMPLE_RATE / 1000
            val fade = min(length / 2, SAMPLE_RATE * FADE_MILLIS / 1000)
            for (i in 0 until length) {
                // Fading in and out avoids clicks at the note edges.
                val envelope = min(1.0, min(i, length - i).toDouble() / fade)
                val value = sin(2 * PI * note.frequency * i / SAMPLE_RATE) * envelope * VOLUME
                samples[offset + i] = (value * Short.MAX_VALUE).toInt().toShort()
            }
            offset += length
        }
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            try {
                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep(notes.sumOf { it.millis } + 60L)
            } finally {
                track.release()
            }
        } catch (_: RuntimeException) {
            // No audio output available; the game goes on silently.
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22050
        const val FADE_MILLIS = 8
        const val VOLUME = 0.25
    }
}

/** Turns changes of the game state into sound and vibration. */
@Composable
fun GameFeedbackEffect(state: GameState?, feedback: GameFeedback) {
    val previous = remember { mutableStateOf<Triple<Int, Int, GameResult>?>(null) }
    val current = state?.let { Triple(it.mistakes, it.entries, it.result) }

    LaunchedEffect(current) {
        val before = previous.value
        previous.value = current
        if (before == null || current == null) return@LaunchedEffect
        val (mistakes, entries, result) = current
        when {
            result != before.third && result == GameResult.COMPLETED -> feedback.play(FeedbackEvent.COMPLETE)
            result != before.third && result == GameResult.FAILED -> feedback.play(FeedbackEvent.FAILED)
            mistakes > before.first -> feedback.play(FeedbackEvent.ERROR)
            entries > before.second -> feedback.play(FeedbackEvent.CORRECT)
        }
    }
}
