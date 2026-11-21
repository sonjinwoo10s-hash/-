package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerScheduleStore
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.repository.MuezzinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Robust Foreground Service for Adhan playback with native Android MediaSession,
 * MediaStyle lock-screen/notification controls, full System Audio Focus handling,
 * Becoming-Noisy headphone handling, and wake-lock safety.
 *
 * Supports both:
 *  1) Scheduled Prayer Time Adhan ([ACTION_START_ADHAN]) — USAGE_ALARM + AUDIOFOCUS_GAIN_TRANSIENT
 *     with offline-first cache, custom file, remote stream, and system alarm ringtone fallback.
 *  2) Interactive / Preview Adhan ([ACTION_START_PREVIEW]) — USAGE_MEDIA + AUDIOFOCUS_GAIN
 *     with multi-source fallback, Play/Pause/Stop controls, and live progress synced to UI.
 */
class AdhanPlaybackService : Service() {

    companion object {
        const val ACTION_START_ADHAN = "com.example.action.START_ADHAN"
        const val ACTION_START_PREVIEW = "com.example.action.START_ADHAN_PREVIEW"
        const val ACTION_PLAY_PAUSE = "com.example.action.ADHAN_PLAY_PAUSE"
        const val ACTION_SWITCH_FALLBACK = "com.example.action.ADHAN_SWITCH_FALLBACK"
        const val ACTION_RETRY = "com.example.action.ADHAN_RETRY"
        const val ACTION_STOP_ADHAN = "com.example.action.STOP_ADHAN"

        const val EXTRA_MUEZZIN_ID = "extra_adhan_muezzin_id"
        const val EXTRA_MUEZZIN_NAME = "extra_adhan_muezzin_name"
        const val EXTRA_SOURCES = "extra_adhan_sources"

        const val CHANNEL_ID = "rafiq_adhan_alert_channel"
        const val MEDIA_CHANNEL_ID = "rafiq_adhan_media_channel"
        const val NOTIFICATION_ID = 4101

        private const val SAFETY_STOP_MS = 5 * 60 * 1000L // 5 minutes ceiling
        private const val STREAM_LOAD_TIMEOUT_MS = 8500L
        private const val PROGRESS_TICK_MS = 350L
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val FLIP_FACE_DOWN_Z_THRESHOLD = -8.5f
        private const val FLIP_ARM_Z_THRESHOLD = -5.5f
        private const val FLIP_HOLD_DURATION_MS = 400L
        private const val VOLUME_MONITOR_ARM_DELAY_MS = 350L

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        private val _playbackState = MutableStateFlow(AdhanPlayerState())
        val playbackState: StateFlow<AdhanPlayerState> = _playbackState.asStateFlow()

        fun isActivelyPlayingOrLoading(): Boolean {
            if (!isServiceRunning) return false
            val s = _playbackState.value.status
            return s == AdhanPlaybackStatus.PLAYING ||
                s == AdhanPlaybackStatus.FALLBACK_PLAYING ||
                s == AdhanPlaybackStatus.LOADING
        }

        fun startPreview(
            context: Context,
            muezzinId: String,
            muezzinName: String,
            sources: ArrayList<String>
        ) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, AdhanPlaybackService::class.java).apply {
                action = ACTION_START_PREVIEW
                putExtra(EXTRA_MUEZZIN_ID, muezzinId)
                putExtra(EXTRA_MUEZZIN_NAME, muezzinName)
                putStringArrayListExtra(EXTRA_SOURCES, sources)
            }
            runCatching {
                if (isServiceRunning) appCtx.startService(intent)
                else ContextCompat.startForegroundService(appCtx, intent)
            }
        }

        fun sendControlAction(context: Context, action: String) {
            val appCtx = context.applicationContext
            if (!isServiceRunning) {
                if (action == ACTION_STOP_ADHAN) _playbackState.value = AdhanPlayerState()
                return
            }
            val intent = Intent(appCtx, AdhanPlaybackService::class.java).apply {
                this.action = action
            }
            runCatching { appCtx.startService(intent) }
        }
    }

    private enum class PlaybackMode { ALARM, MEDIA }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var sensorManager: SensorManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var playbackMode: PlaybackMode = PlaybackMode.ALARM
    private var hasAudioFocus: Boolean = false
    private var delayedFocusPending: Boolean = false
    private var pausedByTransientFocusLoss: Boolean = false
    private var noisyReceiverRegistered: Boolean = false
    private var volumeReceiverRegistered: Boolean = false
    private var flipSensorRegistered: Boolean = false
    private var volumeMonitorArmedAtMs: Long = 0L
    private var flipArmed: Boolean = false
    private var faceDownSinceMs: Long = 0L
    private var isPrepared: Boolean = false
    private var isUserPaused: Boolean = false
    private var sessionToken: Long = 0L

    private var activeTitle: String = "الأذان"
    private var activeSubtitle: String = "رفيق المسلم"
    private var activeMuezzinId: String = ""
    private var activeMuezzinName: String = ""
    private var activeSources: List<String> = emptyList()
    private var activeSourceIndex: Int = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopPlaybackAndSelf() }
    private var loadTimeoutRunnable: Runnable? = null

    private val progressTickerRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && isPrepared && !isUserPaused) {
                val fraction = runCatching {
                    val dur = mp.duration
                    val pos = mp.currentPosition
                    if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                }.getOrDefault(0f)
                _playbackState.update { it.copy(progressFraction = fraction) }
                mainHandler.postDelayed(this, PROGRESS_TICK_MS)
            }
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY &&
                playbackMode == PlaybackMode.MEDIA
            ) {
                pausedByTransientFocusLoss = false
                pausePlayback()
            }
        }
    }

    /**
     * Dynamic Volume Button detector (works even on lock screen while Foreground Service plays).
     * Registered strictly while Adhan is playing and unregistered the instant it pauses/stops.
     */
    private val volumeButtonSilenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_VOLUME_CHANGED) return
            if (!PrayerScheduleStore.isAdhanSilenceOnVolumeEnabled(this@AdhanPlaybackService)) return
            if (!isActivelyPlayingOrLoading()) return
            if (SystemClock.elapsedRealtime() - volumeMonitorArmedAtMs < VOLUME_MONITOR_ARM_DELAY_MS) return

            val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
            val isRelevantStream = streamType == AudioManager.STREAM_ALARM ||
                streamType == AudioManager.STREAM_MUSIC ||
                streamType == AudioManager.STREAM_RING ||
                streamType == AudioManager.STREAM_NOTIFICATION
            if (isRelevantStream) {
                stopPlaybackAndSelf()
            }
        }
    }

    /**
     * Ultra-lightweight Flip-to-Silence accelerometer listener (SENSOR_DELAY_NORMAL).
     * Registered ONLY while Adhan is actively playing and unregistered immediately when stopped.
     * Triggers when device is flipped face-down (z < -8.5f) and held for >= 400ms.
     */
    private val flipToSilenceListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            if (!PrayerScheduleStore.isAdhanSilenceOnFlipEnabled(this@AdhanPlaybackService)) return
            if (!isCurrentlyPlaying()) return

            val z = event.values[2]
            if (!flipArmed) {
                // Arm once phone is detected not already lying face-down, preventing accidental auto-stop
                if (z > FLIP_ARM_Z_THRESHOLD) {
                    flipArmed = true
                }
                return
            }

            if (z < FLIP_FACE_DOWN_Z_THRESHOLD) {
                val now = SystemClock.elapsedRealtime()
                if (faceDownSinceMs == 0L) {
                    faceDownSinceMs = now
                } else if (now - faceDownSinceMs >= FLIP_HOLD_DURATION_MS) {
                    faceDownSinceMs = 0L
                    stopPlaybackAndSelf()
                }
            } else {
                faceDownSinceMs = 0L
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                delayedFocusPending = false
                pausedByTransientFocusLoss = false
                if (playbackMode == PlaybackMode.ALARM) {
                    stopPlaybackAndSelf()
                } else {
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isCurrentlyPlaying()) {
                    pausedByTransientFocusLoss = true
                    pausePlayback(keepFocus = true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                runCatching { mediaPlayer?.setVolume(0.2f, 0.2f) }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                runCatching { mediaPlayer?.setVolume(1.0f, 1.0f) }
                if (delayedFocusPending || pausedByTransientFocusLoss) {
                    delayedFocusPending = false
                    pausedByTransientFocusLoss = false
                    resumePlayback()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        createNotificationChannels()
        initMediaSession()
        registerNoisyReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ADHAN -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                if (isCurrentlyPlaying()) pausePlayback() else resumePlayback()
                return START_NOT_STICKY
            }
            ACTION_SWITCH_FALLBACK -> {
                if (activeSources.isNotEmpty()) {
                    val nextIdx = if (activeSources.size > 1) (activeSourceIndex + 1) % activeSources.size else 0
                    val token = ++sessionToken
                    trySourceAtIndex(nextIdx, token)
                }
                return START_NOT_STICKY
            }
            ACTION_RETRY -> {
                if (activeSources.isNotEmpty()) {
                    val token = ++sessionToken
                    trySourceAtIndex(0, token)
                }
                return START_NOT_STICKY
            }
            ACTION_START_PREVIEW -> {
                handleStartPreview(intent)
                return START_NOT_STICKY
            }
            else -> {
                handleStartScheduledAlarm(intent)
                return START_NOT_STICKY
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Persist smoothly in the background if Adhan is actively playing or buffering; clean up if paused/idle
        if (!isCurrentlyPlaying() && _playbackState.value.status != AdhanPlaybackStatus.LOADING) {
            stopPlaybackAndSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun handleStartScheduledAlarm(intent: Intent?) {
        playbackMode = PlaybackMode.ALARM
        isUserPaused = false
        pausedByTransientFocusLoss = false

        val prayerKey = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_KEY) ?: "Fajr"
        val prayerName = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val prayerTime = intent?.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME).orEmpty()

        val audioConfig = PrayerScheduleStore.getAdhanAudioForPrayer(this, prayerKey)
        val timePart = if (prayerTime.isNotBlank()) " ($prayerTime)" else ""
        activeMuezzinId = audioConfig.muezzinId
        activeMuezzinName = audioConfig.muezzinName
        activeTitle = "حان الآن وقت صلاة $prayerName$timePart"
        activeSubtitle = "الأذان بصوت ${audioConfig.muezzinName}"

        val cachedMp3 = MuezzinRepository.getCachedAdhanFile(this, audioConfig.muezzinId)
        activeSources = buildList {
            if (cachedMp3.exists() && cachedMp3.length() > 0L) add(cachedMp3.absolutePath)
            val localPath = audioConfig.localFilePath
            if (!localPath.isNullOrBlank() && File(localPath).exists()) add(localPath)
            val remoteUrl = audioConfig.remoteUrl
            if (!remoteUrl.isNullOrBlank()) add(remoteUrl)
        }.distinct()
        activeSourceIndex = 0

        if (!promoteToForeground()) return

        // Pause any active Quran audio before Adhan starts
        QuranMediaEngine.pauseFromSystem()

        acquireWakeLock()
        scheduleSafetyTimeout()

        val token = ++sessionToken
        if (activeSources.isEmpty()) {
            playSystemAlarmFallback(token)
        } else {
            trySourceAtIndex(0, token)
        }
    }

    private fun handleStartPreview(intent: Intent) {
        playbackMode = PlaybackMode.MEDIA
        isUserPaused = false
        pausedByTransientFocusLoss = false

        activeMuezzinId = intent.getStringExtra(EXTRA_MUEZZIN_ID).orEmpty()
        activeMuezzinName = intent.getStringExtra(EXTRA_MUEZZIN_NAME).orEmpty().ifBlank { "الأذان" }
        activeTitle = "الأذان — $activeMuezzinName"
        activeSubtitle = "رفيق المسلم • معاينة الأذان"
        activeSources = intent.getStringArrayListExtra(EXTRA_SOURCES).orEmpty().filter { it.isNotBlank() }
        activeSourceIndex = 0

        if (!promoteToForeground()) return

        QuranMediaEngine.pauseFromSystem()
        acquireWakeLock()
        scheduleSafetyTimeout()

        if (activeSources.isEmpty()) {
            _playbackState.value = AdhanPlayerState(
                activeMuezzinId = activeMuezzinId,
                activeMuezzinName = activeMuezzinName,
                status = AdhanPlaybackStatus.ERROR,
                errorDetail = "لا يوجد ملف صوتي صالح — استخدم «تعديل الرابط / رفع صوت»"
            )
            updateMediaSessionAndNotification()
            return
        }

        val token = ++sessionToken
        trySourceAtIndex(0, token)
    }

    private fun trySourceAtIndex(index: Int, token: Long) {
        if (token != sessionToken) return
        cancelLoadTimeout()
        stopProgressTicker()
        releasePlayer()

        if (index >= activeSources.size) {
            if (playbackMode == PlaybackMode.ALARM) {
                playSystemAlarmFallback(token)
            } else {
                releaseWakeLock()
                abandonAudioFocus()
                unregisterQuickSilenceMonitors()
                _playbackState.value = AdhanPlayerState(
                    activeMuezzinId = activeMuezzinId,
                    activeMuezzinName = activeMuezzinName,
                    status = AdhanPlaybackStatus.ERROR,
                    errorDetail = "تعذر تشغيل الصوت — يمكنك المحاولة مجدداً أو استبداله عبر «تعديل الرابط / رفع صوت»",
                    activeStreamIndex = 0,
                    totalStreams = activeSources.size
                )
                updateMediaSessionAndNotification()
            }
            return
        }

        activeSourceIndex = index
        val source = activeSources[index]
        val isFallback = index > 0

        _playbackState.value = AdhanPlayerState(
            activeMuezzinId = activeMuezzinId,
            activeMuezzinName = activeMuezzinName,
            status = AdhanPlaybackStatus.LOADING,
            errorDetail = null,
            activeStreamIndex = index,
            totalStreams = activeSources.size,
            progressFraction = 0f
        )
        updateMediaSessionAndNotification()

        val timeoutTask = Runnable {
            if (token == sessionToken && _playbackState.value.status == AdhanPlaybackStatus.LOADING) {
                trySourceAtIndex(index + 1, token)
            }
        }
        loadTimeoutRunnable = timeoutTask
        mainHandler.postDelayed(timeoutTask, STREAM_LOAD_TIMEOUT_MS)

        try {
            val mp = createConfiguredMediaPlayer().apply {
                setDataSource(source)
                setOnPreparedListener { player ->
                    if (token != sessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    cancelLoadTimeout()
                    isPrepared = true
                    startPreparedPlayer(player, isFallback)
                }
                setOnCompletionListener {
                    if (token != sessionToken) return@setOnCompletionListener
                    stopPlaybackAndSelf()
                }
                setOnErrorListener { _, _, _ ->
                    if (token != sessionToken) return@setOnErrorListener true
                    cancelLoadTimeout()
                    trySourceAtIndex(index + 1, token)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            cancelLoadTimeout()
            trySourceAtIndex(index + 1, token)
        }
    }

    private fun startPreparedPlayer(player: MediaPlayer, isFallback: Boolean) {
        val focusResult = requestAudioFocus()
        when (focusResult) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                hasAudioFocus = true
                delayedFocusPending = false
                isUserPaused = false
                runCatching {
                    player.setVolume(1.0f, 1.0f)
                    player.start()
                }.onFailure {
                    if (playbackMode == PlaybackMode.ALARM) {
                        playSystemAlarmFallback(sessionToken)
                    } else {
                        trySourceAtIndex(activeSourceIndex + 1, sessionToken)
                    }
                    return
                }
                _playbackState.value = AdhanPlayerState(
                    activeMuezzinId = activeMuezzinId,
                    activeMuezzinName = activeMuezzinName,
                    status = if (isFallback) AdhanPlaybackStatus.FALLBACK_PLAYING else AdhanPlaybackStatus.PLAYING,
                    errorDetail = null,
                    activeStreamIndex = activeSourceIndex,
                    totalStreams = activeSources.size.coerceAtLeast(1),
                    progressFraction = 0f
                )
                acquireWakeLock()
                registerQuickSilenceMonitors()
                startProgressTicker()
                updateMediaSessionAndNotification()
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                // Respect system delayed focus (e.g., ongoing phone call): wait until AUDIOFOCUS_GAIN
                hasAudioFocus = false
                delayedFocusPending = true
                isUserPaused = false
                _playbackState.value = AdhanPlayerState(
                    activeMuezzinId = activeMuezzinId,
                    activeMuezzinName = activeMuezzinName,
                    status = AdhanPlaybackStatus.PAUSED,
                    activeStreamIndex = activeSourceIndex,
                    totalStreams = activeSources.size.coerceAtLeast(1)
                )
                releaseWakeLock()
                updateMediaSessionAndNotification()
            }
            else -> {
                // Focus denied (e.g., active emergency/call) -> remain paused cleanly
                hasAudioFocus = false
                isUserPaused = true
                _playbackState.value = AdhanPlayerState(
                    activeMuezzinId = activeMuezzinId,
                    activeMuezzinName = activeMuezzinName,
                    status = AdhanPlaybackStatus.PAUSED,
                    activeStreamIndex = activeSourceIndex,
                    totalStreams = activeSources.size.coerceAtLeast(1)
                )
                releaseWakeLock()
                updateMediaSessionAndNotification()
            }
        }
    }

    private fun playSystemAlarmFallback(token: Long) {
        if (token != sessionToken) return
        cancelLoadTimeout()
        releasePlayer()
        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: run {
                    stopPlaybackAndSelf()
                    return
                }
            val mp = createConfiguredMediaPlayer().apply {
                setDataSource(applicationContext, fallbackUri)
                isLooping = false
                setOnPreparedListener { player ->
                    if (token != sessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    isPrepared = true
                    startPreparedPlayer(player, isFallback = true)
                }
                setOnCompletionListener {
                    if (token == sessionToken) stopPlaybackAndSelf()
                }
                setOnErrorListener { _, _, _ ->
                    if (token == sessionToken) stopPlaybackAndSelf()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun pausePlayback(keepFocus: Boolean = false) {
        isUserPaused = !keepFocus
        stopProgressTicker()
        unregisterQuickSilenceMonitors()
        runCatching {
            if (isPrepared && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        }
        releaseWakeLock()
        if (!keepFocus) {
            abandonAudioFocus()
        }
        _playbackState.update { state ->
            if (state.status == AdhanPlaybackStatus.IDLE) state
            else state.copy(status = AdhanPlaybackStatus.PAUSED)
        }
        updateMediaSessionAndNotification()
    }

    private fun resumePlayback() {
        val mp = mediaPlayer ?: run {
            if (activeSources.isNotEmpty()) {
                val token = ++sessionToken
                trySourceAtIndex(activeSourceIndex, token)
            }
            return
        }
        if (!isPrepared) return

        val focusResult = if (hasAudioFocus) AudioManager.AUDIOFOCUS_REQUEST_GRANTED else requestAudioFocus()
        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasAudioFocus = true
            isUserPaused = false
            pausedByTransientFocusLoss = false
            delayedFocusPending = false
            QuranMediaEngine.pauseFromSystem()
            runCatching {
                mp.setVolume(1.0f, 1.0f)
                mp.start()
            }
            acquireWakeLock()
            registerQuickSilenceMonitors()
            _playbackState.update { state ->
                state.copy(
                    status = if (activeSourceIndex > 0) {
                        AdhanPlaybackStatus.FALLBACK_PLAYING
                    } else {
                        AdhanPlaybackStatus.PLAYING
                    }
                )
            }
            startProgressTicker()
            updateMediaSessionAndNotification()
        } else if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
            delayedFocusPending = true
        }
    }

    private fun seekToPosition(posMs: Long) {
        val mp = mediaPlayer ?: return
        if (!isPrepared) return
        runCatching {
            val dur = mp.duration
            val safePos = if (dur > 0) posMs.toInt().coerceIn(0, dur) else 0
            mp.seekTo(safePos)
            if (dur > 0) {
                _playbackState.update { it.copy(progressFraction = (safePos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)) }
            }
        }
        updateMediaSessionState()
    }

    private fun createConfiguredMediaPlayer(): MediaPlayer {
        val usage = if (playbackMode == PlaybackMode.ALARM) {
            AudioAttributes.USAGE_ALARM
        } else {
            AudioAttributes.USAGE_MEDIA
        }
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "RafiqAdhanMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onStop() {
                    stopPlaybackAndSelf()
                }

                override fun onSeekTo(pos: Long) {
                    seekToPosition(pos)
                }

                @Suppress("DEPRECATION")
                override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent != null &&
                        keyEvent.action == KeyEvent.ACTION_DOWN &&
                        (keyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                            keyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                            keyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) &&
                        PrayerScheduleStore.isAdhanSilenceOnVolumeEnabled(this@AdhanPlaybackService)
                    ) {
                        stopPlaybackAndSelf()
                        return true
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState() {
        val session = mediaSession ?: return
        val mp = mediaPlayer
        val durationMs = if (isPrepared) runCatching { mp?.duration ?: 0 }.getOrDefault(0).coerceAtLeast(0) else 0
        val positionMs = if (isPrepared) runCatching { mp?.currentPosition ?: 0 }.getOrDefault(0).coerceAtLeast(0) else 0

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, activeTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, activeSubtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "رفيق المسلم — الأذان")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs.toLong())
            .build()
        session.setMetadata(metadata)

        val status = _playbackState.value.status
        val pbState = when (status) {
            AdhanPlaybackStatus.LOADING -> PlaybackState.STATE_BUFFERING
            AdhanPlaybackStatus.PLAYING,
            AdhanPlaybackStatus.FALLBACK_PLAYING -> PlaybackState.STATE_PLAYING
            AdhanPlaybackStatus.PAUSED -> PlaybackState.STATE_PAUSED
            AdhanPlaybackStatus.ERROR -> PlaybackState.STATE_ERROR
            AdhanPlaybackStatus.IDLE -> PlaybackState.STATE_STOPPED
        }
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SEEK_TO

        val speed = if (pbState == PlaybackState.STATE_PLAYING) 1.0f else 0f
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(pbState, positionMs.toLong(), speed)
                .build()
        )
    }

    private fun updateMediaSessionAndNotification() {
        updateMediaSessionState()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildAdhanMediaNotification())
    }

    private fun promoteToForeground(): Boolean {
        updateMediaSessionState()
        val notification = buildAdhanMediaNotification()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (_: Exception) {
            stopPlaybackAndSelf()
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun buildAdhanMediaNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            4102,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePending = serviceActionPendingIntent(ACTION_PLAY_PAUSE, 4104)
        val stopPending = serviceActionPendingIntent(ACTION_STOP_ADHAN, 4103)

        val status = _playbackState.value.status
        val isActivePlaying = status == AdhanPlaybackStatus.PLAYING ||
            status == AdhanPlaybackStatus.FALLBACK_PLAYING ||
            status == AdhanPlaybackStatus.LOADING
        val playPauseIcon = if (isActivePlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (isActivePlaying) "إيقاف مؤقت" else "استئناف"

        val channelId = if (playbackMode == PlaybackMode.ALARM) CHANNEL_ID else MEDIA_CHANNEL_ID
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        val category = if (playbackMode == PlaybackMode.ALARM) {
            Notification.CATEGORY_ALARM
        } else {
            Notification.CATEGORY_TRANSPORT
        }

        return builder
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(activeTitle)
            .setContentText(activeSubtitle)
            .setSubText(if (status == AdhanPlaybackStatus.LOADING) "جاري تحميل الأذان..." else "الأذان")
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPending)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isActivePlaying)
            .setShowWhen(false)
            .setCategory(category)
            .addAction(
                Notification.Action.Builder(playPauseIcon, playPauseLabel, playPausePending).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "إيقاف الأذان",
                    stopPending
                ).build()
            )
            .setStyle(mediaStyle)
            .build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, AdhanPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val alarmChannel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عالية الأولوية عند دخول وقت الصلاة مع التحكم بالأذان"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val mediaChannel = NotificationChannel(
                MEDIA_CHANNEL_ID,
                "مشغّل الأذان في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار التحكم بصوت الأذان في الخلفية وشاشة القفل"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            nm.createNotificationChannel(alarmChannel)
            nm.createNotificationChannel(mediaChannel)
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus(): Int {
        val am = audioManager ?: return AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        val isAlarm = playbackMode == PlaybackMode.ALARM
        val focusGain = if (isAlarm) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        } else {
            AudioManager.AUDIOFOCUS_GAIN
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Abandon any previous focus request with different attributes
            audioFocusRequest?.let { prev -> runCatching { am.abandonAudioFocusRequest(prev) } }
            val attrs = AudioAttributes.Builder()
                .setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val req = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            val stream = if (isAlarm) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            am.requestAudioFocus(audioFocusChangeListener, stream, focusGain)
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { req -> runCatching { am.abandonAudioFocusRequest(req) } }
            audioFocusRequest = null
        } else {
            runCatching { am.abandonAudioFocus(audioFocusChangeListener) }
        }
        hasAudioFocus = false
        delayedFocusPending = false
    }

    private fun registerNoisyReceiver() {
        if (noisyReceiverRegistered) return
        runCatching {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(becomingNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(becomingNoisyReceiver, filter)
            }
            noisyReceiverRegistered = true
        }
    }

    private fun unregisterNoisyReceiver() {
        if (!noisyReceiverRegistered) return
        runCatching { unregisterReceiver(becomingNoisyReceiver) }
        noisyReceiverRegistered = false
    }

    private fun registerQuickSilenceMonitors() {
        // 1) Volume button receiver (dynamic, active ONLY during playback)
        if (!volumeReceiverRegistered && PrayerScheduleStore.isAdhanSilenceOnVolumeEnabled(this)) {
            runCatching {
                volumeMonitorArmedAtMs = SystemClock.elapsedRealtime()
                val filter = IntentFilter(ACTION_VOLUME_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(volumeButtonSilenceReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    registerReceiver(volumeButtonSilenceReceiver, filter)
                }
                volumeReceiverRegistered = true
            }
        }

        // 2) Flip-to-silence accelerometer (SENSOR_DELAY_NORMAL, active ONLY during playback)
        if (!flipSensorRegistered && PrayerScheduleStore.isAdhanSilenceOnFlipEnabled(this)) {
            runCatching {
                val sm = sensorManager ?: (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.also {
                    sensorManager = it
                }
                val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (accel != null) {
                    flipArmed = false
                    faceDownSinceMs = 0L
                    flipSensorRegistered = sm.registerListener(
                        flipToSilenceListener,
                        accel,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                }
            }
        }
    }

    private fun unregisterQuickSilenceMonitors() {
        if (volumeReceiverRegistered) {
            runCatching { unregisterReceiver(volumeButtonSilenceReceiver) }
            volumeReceiverRegistered = false
        }
        if (flipSensorRegistered) {
            runCatching { sensorManager?.unregisterListener(flipToSilenceListener) }
            flipSensorRegistered = false
        }
        flipArmed = false
        faceDownSinceMs = 0L
    }

    private fun isCurrentlyPlaying(): Boolean {
        return isPrepared && runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)
    }

    private fun startProgressTicker() {
        mainHandler.removeCallbacks(progressTickerRunnable)
        mainHandler.post(progressTickerRunnable)
    }

    private fun stopProgressTicker() {
        mainHandler.removeCallbacks(progressTickerRunnable)
    }

    private fun scheduleSafetyTimeout() {
        mainHandler.removeCallbacks(autoStopRunnable)
        mainHandler.postDelayed(autoStopRunnable, SAFETY_STOP_MS)
    }

    private fun cancelLoadTimeout() {
        loadTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        loadTimeoutRunnable = null
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RafiqAlMuslim::AdhanPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(SAFETY_STOP_MS)
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun releasePlayer() {
        isPrepared = false
        runCatching {
            mediaPlayer?.let { mp ->
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                if (mp.isPlaying) mp.stop()
                mp.reset()
                mp.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopPlaybackAndSelf() {
        sessionToken++
        cancelLoadTimeout()
        stopProgressTicker()
        unregisterQuickSilenceMonitors()
        mainHandler.removeCallbacks(autoStopRunnable)
        pausedByTransientFocusLoss = false
        delayedFocusPending = false
        isUserPaused = false
        releasePlayer()
        abandonAudioFocus()
        releaseWakeLock()
        _playbackState.value = AdhanPlayerState()
        updateMediaSessionState()
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        isServiceRunning = false
        sessionToken++
        cancelLoadTimeout()
        stopProgressTicker()
        unregisterQuickSilenceMonitors()
        mainHandler.removeCallbacks(autoStopRunnable)
        unregisterNoisyReceiver()
        releasePlayer()
        abandonAudioFocus()
        releaseWakeLock()
        _playbackState.value = AdhanPlayerState()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
