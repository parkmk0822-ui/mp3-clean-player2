import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun FolderScannerScreen() {
    val context = LocalContext.current
    var mp3Files by remember { mutableStateOf<List<String>>(emptyList()) }

    // 1. 폴더 선택 창을 띄우고 결과를 받아오는 런처
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // 2. 선택한 폴더에 대한 영구 접근 권한 저장 (앱을 껐다 켜도 유지되도록)
            context.contentResolver.takePersistableUriPermission(
                selectedUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // 3. 폴더 안의 파일 스캔
            val documentTree = DocumentFile.fromTreeUri(context, selectedUri)
            val files = documentTree?.listFiles() ?: emptyArray()

            // 4. .mp3 로 끝나는 파일 이름만 필터링해서 리스트에 저장
            mp3Files = files
                .filter { it.isFile && it.name?.endsWith(".mp3", ignoreCase = true) == true }
                .map { it.name ?: "이름 없음" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { folderPickerLauncher.launch(null) },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("음악 폴더 지정하기")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mp3Files.isEmpty()) {
            Text("선택된 MP3 파일이 없습니다.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(mp3Files) { fileName ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎵 $fileName",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
