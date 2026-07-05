@file:OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class,
)

package com.mkchtv.cleantemplate

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.mkchtv.cleantemplate.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay

fun parseLrc(context: Context, lrcUri: Uri?): List<LyricLine> {
    if (lrcUri == null) return listOf(LyricLine(0, "가사 파일(.lrc)이 없습니다."))
    val lyrics = mutableListOf<LyricLine>()
    try {
        context.contentResolver.openInputStream(lrcUri)?.bufferedReader()?.useLines { lines ->
            val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
            lines.forEach { line ->
                val matchResult = regex.find(line)
                if (matchResult != null) {
                    val (min, sec, msStr, text) = matchResult.destructured
                    val ms = msStr.padEnd(3, '0').toLong()
                    val timeMs = min.toLong() * 60000 + sec.toLong() * 1000 + ms
                    if (text.isNotBlank()) lyrics.add(LyricLine(timeMs, text.trim()))
                }
            }
        }
    } catch (e: Exception) { return listOf(LyricLine(0, "가사 파싱 오류")) }
    return if (lyrics.isEmpty()) listOf(LyricLine(0, "가사 정보가 없습니다.")) else lyrics.sortedBy { it.time }
}

fun extractMetaData(context: Context, uri: Uri): Triple<String, String, Bitmap?> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val artBytes = retriever.embeddedPicture
        val bitmap = if (artBytes != null) BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size) else null
        Triple(title ?: "알 수 없는 제목", artist ?: "알 수 없는 아티스트", bitmap)
    } catch (e: Exception) { Triple("알 수 없는 제목", "알 수 없는 아티스트", null) } finally { retriever.release() }
}

fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppContent() }
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current

    // 💡 백그라운드 서비스에 연결할 리모컨(MediaController)을 준비합니다.
    var player by remember { mutableStateOf<Player?>(null) }
    var controllerFuture by remember { mutableStateOf<ListenableFuture<MediaController>?>(null) }

    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            { player = controllerFuture?.get() },
            ContextCompat.getMainExecutor(context)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            controllerFuture?.let { MediaController.releaseFuture(it) }
        }
    }

    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("미디어 엔진 시작 중...", color = Color.White)
        }
        return
    }

    val exoPlayer = player!! // 이제 이 화면은 단순한 리모컨입니다!

    var currentTitle by remember { mutableStateOf("재생 중인 곡이 없습니다") }
    var currentArtist by remember { mutableStateOf("Premium Player") }
    var currentAlbumArt by remember { mutableStateOf<Bitmap?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    var currentLyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var lrcFilesMap by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }

    var speed by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var isShuffleEnabled by remember { mutableStateOf(false) }
    var isRepeatEnabled by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) { isPlaying = isPlayingState }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { item ->
                    val uri = item.localConfiguration?.uri
                    val fileName = item.mediaId
                    if (uri != null) {
                        val (title, artist, bitmap) = extractMetaData(context, uri)
                        currentTitle = if (title == "알 수 없는 제목") fileName else title
                        currentArtist = artist
                        currentAlbumArt = bitmap

                        val baseName = fileName.substringBeforeLast(".")
                        currentLyrics = parseLrc(context, lrcFilesMap[baseName])
                    }
                }
                currentPosition = 0L
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
        exoPlayer.addListener(listener)
        // 💡 서비스가 관리하므로 여기서 release()를 호출하지 않습니다.
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            if (exoPlayer.duration > 0) duration = exoPlayer.duration
            delay(300L)
        }
    }

    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    FolderScannerScreen(onSongSelected = { index, list, lrcs ->
                        lrcFilesMap = lrcs
                        val mediaItems = list.map { MediaItem.Builder().setUri(it.first).setMediaId(it.second).build() }
                        exoPlayer.setMediaItems(mediaItems, index, 0L)
                        exoPlayer.prepare(); exoPlayer.play()
                    })
                }

                PlayerControlScreen(
                    title = currentTitle, artist = currentArtist, albumArt = currentAlbumArt, isPlaying = isPlaying,
                    progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    currentTimeText = formatTime(currentPosition), totalTimeText = formatTime(duration),
                    lyrics = currentLyrics, currentPosition = currentPosition,
                    playbackSpeed = speed, playbackPitch = pitch,
                    isShuffleEnabled = isShuffleEnabled, isRepeatEnabled = isRepeatEnabled,
                    onPlayPauseClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    onSeek = {
                        val seekPos = (duration * it).toLong()
                        exoPlayer.seekTo(seekPos)
                        currentPosition = seekPos
                    },
                    onPreviousClick = { exoPlayer.seekToPrevious() },
                    onNextClick = { exoPlayer.seekToNext() },
                    onSpeedChange = { speed = it; exoPlayer.playbackParameters = PlaybackParameters(speed, pitch) },
                    onPitchChange = { pitch = it; exoPlayer.playbackParameters = PlaybackParameters(speed, pitch) },
                    onShuffleClick = { isShuffleEnabled = !isShuffleEnabled; exoPlayer.shuffleModeEnabled = isShuffleEnabled },
                    onRepeatClick = { isRepeatEnabled = !isRepeatEnabled; exoPlayer.repeatMode = if (isRepeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF }
                )
            }
        }
    }
}