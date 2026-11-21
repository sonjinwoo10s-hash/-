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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Lightweight Foreground Service with native Android MediaSession, MediaStyle lock-screen
 * notification, Audio Focus handling, and Becoming-Noisy (headphone unplug) handling for
 * uninterrupted background Quran recitation on Android 8+ (including 2GB-3GB RAM devices).
 */
class QuranAudioPlaybackService : Service() {

    companion object {
        const val ACTION_UPDATE_STATE = "com.example.action.QURAN_UPDATE_STATE"
        const val ACTION_PLAY_PAUSE = "com.example.action.QURAN_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.QURAN_NEXT"
        const val ACTION_PREVIOUS = "com.example.action.QURAN_PREVIOUS"
        const val ACTION_STOP = "com.example.action.QURAN_STOP"

        const val CHANNEL_ID = "rafiq_quran_media_channel"
        const val NOTIFICATION_ID = 4201

        @Volatile
        private var isRunning: Boolean = false

        fun syncForegroundState(context: Context, snapshot: QuranPlaybackNotificationState) {
            val appCtx = context.applicationContext
            if (!snapshot.isActive) {
                if (isRunning) {
                    val stopIntent = Intent(appCtx, QuranAudioPlaybackService::class.java).apply {
                        action = ACTION_STOP
                    }
                    runCatching { appCtx.startService(stopIntent) }
                }
                return
            }

            val intent = Intent(appCtx, QuranAudioPlaybackService::class.java).apply {
                action = ACTION_UPDATE_STATE
            }
            runCatching {
                if (isRunning) {
                    appCtx.startService(intent)
                } else {
                    ContextCompat.startForegroundService(appCtx, intent)
                }
            }
        }

        fun requestStop(context: Context) {
            if (!isRunning) return
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, QuranAudioPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { appCtx.startService(intent) }
        }
    }

    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var pausedByTransientFocusLoss: Boolean = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var noisyReceiverRegistered: Boolean = false

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pausedByTransientFocusLoss = false
                QuranMediaEngine.pauseFromSystem()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Another media app permanently took focus -> pause cleanly & abandon focus
                hasAudioFocus = false
                pausedByTransientFocusLoss = false
                QuranMediaEngine.pauseFromSystem()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Incoming phone call or temporary audio prompt -> pause and resume when finished
                if (QuranMediaEngine.isCurrentlyPlaying()) {
                    pausedByTransientFocusLoss = true
                    QuranMediaEngine.pauseFromSystem()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Gently lower volume during navigation/notification voice
                QuranMediaEngine.setDucking(true)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                QuranMediaEngine.setDucking(false)
                if (pausedByTransientFocusLoss) {
                    pausedByTransientFocusLoss = false
                    QuranMediaEngine.resumeFromSystem()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
        initMediaSession()
        registerNoisyReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                pausedByTransientFocusLoss = false
                QuranMediaEngine.stopAllFromNotification()
                stopForegroundAndCleanup()
                return START_NOT_STICKY
            }
            ACTION_PLAY_PAUSE -> {
                pausedByTransientFocusLoss = false
                QuranMediaEngine.togglePlayPauseFromNotification()
            }
            ACTION_NEXT -> {
                pausedByTransientFocusLoss = false
                QuranMediaEngine.skipNextFromNotification()
            }
            ACTION_PREVIOUS -> {
                pausedByTransientFocusLoss = false
                QuranMediaEngine.skipPreviousFromNotification()
            }
        }

        val snapshot = QuranMediaEngine.currentNotificationState()
        if (!snapshot.isActive) {
            stopForegroundAndCleanup()
            return START_NOT_STICKY
        }

        if (snapshot.isPlaying || snapshot.isBuffering) {
            requestAudioFocusIfNeeded()
            acquireWakeLockIfNeeded()
        } else {
            releaseWakeLock()
        }

        updateMediaSessionState(snapshot)
        val notification = buildMediaNotification(snapshot)
        try {
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
        } catch (_: Exception) {
            stopForegroundAndCleanup()
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing smoothly if actively reciting; otherwise clean up immediately to free RAM
        if (!QuranMediaEngine.isCurrentlyPlaying()) {
            stopForegroundAndCleanup()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "RafiqQuranMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    pausedByTransientFocusLoss = false
                    requestAudioFocusIfNeeded()
                    QuranMediaEngine.resumeFromSystem()
                }

                override fun onPause() {
                    pausedByTransientFocusLoss = false
                    QuranMediaEngine.pauseFromSystem()
                }

                override fun onStop() {
                    pausedByTransientFocusLoss = false
                    QuranMediaEngine.stopAllFromNotification()
                    stopForegroundAndCleanup()
                }

                override fun onSkipToNext() {
                    pausedByTransientFocusLoss = false
                    QuranMediaEngine.skipNextFromNotification()
                }

                override fun onSkipToPrevious() {
                    pausedByTransientFocusLoss = false
                    QuranMediaEngine.skipPreviousFromNotification()
                }

                override fun onSeekTo(pos: Long) {
                    QuranMediaEngine.seekToMsFromSystem(pos.toInt())
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState(snapshot: QuranPlaybackNotificationState) {
        val session = mediaSession ?: return
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, snapshot.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, snapshot.subtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "رفيق المسلم — القرآن الكريم")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, snapshot.durationMs.toLong().coerceAtLeast(0L))
            .build()
        session.setMetadata(metadata)

        val pbState = when {
            snapshot.isBuffering -> PlaybackState.STATE_BUFFERING
            snapshot.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SEEK_TO

        val stateBuilder = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                pbState,
                snapshot.positionMs.toLong().coerceAtLeast(0L),
                if (snapshot.isPlaying) snapshot.playbackSpeed else 0f
            )
        session.setPlaybackState(stateBuilder.build())
    }

    @Suppress("DEPRECATION")
    private fun buildMediaNotification(snapshot: QuranPlaybackNotificationState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            4202,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = serviceActionPendingIntent(ACTION_PREVIOUS, 4203)
        val playPausePending = serviceActionPendingIntent(ACTION_PLAY_PAUSE, 4204)
        val nextPending = serviceActionPendingIntent(ACTION_NEXT, 4205)
        val stopPending = serviceActionPendingIntent(ACTION_STOP, 4206)

        val playPauseIcon = if (snapshot.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (snapshot.isPlaying) "إيقاف مؤقت" else "تشغيل"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(1, 2, 3)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        return builder
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(snapshot.title)
            .setContentText(snapshot.subtitle)
            .setSubText(if (snapshot.isBuffering) "جاري التحميل..." else "تلاوة القرآن الكريم")
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPending)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(snapshot.isPlaying || snapshot.isBuffering)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_previous,
                    "السابقة",
                    prevPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    playPauseIcon,
                    playPauseLabel,
                    playPausePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_media_next,
                    "التالية",
                    nextPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "إيقاف",
                    stopPending
                ).build()
            )
            .setStyle(mediaStyle)
            .build()
    }

    private fun serviceActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, QuranAudioPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مشغّل القرآن الكريم في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار التحكم بتلاوة القرآن الكريم في الخلفية وشاشة القفل"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocusIfNeeded() {
        if (hasAudioFocus) return
        val am = audioManager ?: return
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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
    }

    private fun acquireWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RafiqAlMuslim::QuranPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
                // 3-hour safety ceiling so battery is never drained if forgotten
                acquire(3 * 60 * 60 * 1000L)
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

    private fun stopForegroundAndCleanup() {
        abandonAudioFocus()
        releaseWakeLock()
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        unregisterNoisyReceiver()
        abandonAudioFocus()
        releaseWakeLock()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
