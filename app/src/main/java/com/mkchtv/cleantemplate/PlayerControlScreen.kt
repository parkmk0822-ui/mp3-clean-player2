package com.mkchtv.cleantemplate

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LyricLine(val time: Long, val text: String)

@Composable
fun PlayerControlScreen(
    title: String = "재생 중인 곡이 없습니다",
    artist: String = "Premium Player",
    albumArt: Bitmap? = null,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    currentTimeText: String = "00:00",
    totalTimeText: String = "00:00",
    lyrics: List<LyricLine> = emptyList(),
    currentPosition: Long = 0L,
    playbackSpeed: Float = 1.0f,
    playbackPitch: Float = 1.0f,
    isShuffleEnabled: Boolean = false,
    isRepeatEnabled: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onRepeatClick: () -> Unit = {}
) {
    val listState = rememberLazyListState()

    val activeLineIndex = remember(currentPosition, lyrics) {
        lyrics.findLast { it.time <= currentPosition }?.let { lyrics.indexOf(it) } ?: 0
    }

    LaunchedEffect(activeLineIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(activeLineIndex)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color(0xFF121212)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            // 배속 및 피치 조절 (노래방 기능)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("배속: ${String.format("%.1f", playbackSpeed)}x", color = Color.White, fontSize = 12.sp)
                Slider(value = playbackSpeed, onValueChange = onSpeedChange, valueRange = 0.5f..2.0f, modifier = Modifier.width(100.dp))

                Spacer(modifier = Modifier.width(16.dp))

                Text("피치: ${String.format("%.1f", playbackPitch)}", color = Color.White, fontSize = 12.sp)
                Slider(value = playbackPitch, onValueChange = onPitchChange, valueRange = 0.5f..1.5f, modifier = Modifier.width(100.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 실시간 싱크 가사 창
            Box(modifier = Modifier.height(120.dp).fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).padding(8.dp)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    itemsIndexed(lyrics) { index, line ->
                        Text(
                            text = line.text,
                            color = if (index == activeLineIndex) Color(0xFF1DB954) else Color.Gray,
                            fontSize = if (index == activeLineIndex) 18.sp else 14.sp,
                            fontWeight = if (index == activeLineIndex) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 앨범 아트 및 곡 정보
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2D2F31))) {
                    if (albumArt != null) Image(bitmap = albumArt.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                }
            }

            // 진행 바
            Slider(value = progress, onValueChange = onSeek, colors = SliderDefaults.colors(thumbColor = Color(0xFF1DB954), activeTrackColor = Color(0xFF1DB954)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currentTimeText, color = Color(0xFFB3B3B3), fontSize = 12.sp)
                Text(totalTimeText, color = Color(0xFFB3B3B3), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 재생 컨트롤 (셔플 - 이전곡 - 재생 - 다음곡 - 반복)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShuffleClick) { Icon(Icons.Default.Shuffle, null, tint = if (isShuffleEnabled) Color(0xFF1DB954) else Color.White, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = onPreviousClick) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp)) }

                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF1DB954)).clickable { onPlayPauseClick() }, contentAlignment = Alignment.Center) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(36.dp))
                }

                IconButton(onClick = onNextClick) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
                IconButton(onClick = onRepeatClick) { Icon(Icons.Default.Repeat, null, tint = if (isRepeatEnabled) Color(0xFF1DB954) else Color.White, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}