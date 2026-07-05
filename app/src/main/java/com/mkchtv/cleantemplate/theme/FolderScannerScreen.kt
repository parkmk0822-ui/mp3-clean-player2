package com.mkchtv.cleantemplate

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun FolderScannerScreen(onSongSelected: (Int, List<Pair<Uri, String>>, Map<String, Uri>) -> Unit) {
    val context = LocalContext.current
    var mp3Files by remember { mutableStateOf<List<Pair<Uri, String>>>(emptyList()) }
    var lrcMap by remember { mutableStateOf<Map<String, Uri>>(emptyMap()) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val documentTree = DocumentFile.fromTreeUri(context, selectedUri)
            val files = documentTree?.listFiles() ?: emptyArray()

            mp3Files = files
                .filter { it.isFile && it.name?.endsWith(".mp3", ignoreCase = true) == true }
                .map { Pair(it.uri, it.name ?: "이름 없음") }

            lrcMap = files
                .filter { it.isFile && it.name?.endsWith(".lrc", ignoreCase = true) == true }
                .associate { (it.name ?: "").substringBeforeLast(".") to it.uri }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { folderPickerLauncher.launch(null) }, modifier = Modifier.padding(top = 32.dp)) {
            Text("음악 폴더 지정하기")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (mp3Files.isEmpty()) {
            Text("선택된 MP3 파일이 없습니다.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(mp3Files) { index, fileInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            onSongSelected(index, mp3Files, lrcMap)
                        }
                    ) {
                        Text(text = "🎵 ${fileInfo.second}", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}