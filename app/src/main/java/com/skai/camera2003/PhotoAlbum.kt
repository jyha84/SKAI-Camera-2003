package com.skai.camera2003

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AlbumBlue: Color @Composable get() = LocalPhoneSkin.current.ink

private data class LoadedPhoto(val bitmap: Bitmap? = null, val loading: Boolean = true, val failed: Boolean = false)

@Composable
private fun rememberPhoto(entry: PhotoEntry, repository: PhotoRepository, maxEdge: Int): LoadedPhoto {
    val state by produceState(LoadedPhoto(), entry.uri, maxEdge) {
        value = LoadedPhoto()
        try {
            value = LoadedPhoto(withContext(Dispatchers.IO) { repository.loadBitmap(entry.uri, maxEdge) }, loading = false)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { value = LoadedPhoto(loading = false, failed = true) }
    }
    return state
}

@Composable
fun PhotoAlbum(photos: List<PhotoEntry>, selected: Int, loading: Boolean, error: String?,
               needsPermission: Boolean, repository: PhotoRepository,
               requestPermission: () -> Unit, select: (Int) -> Unit, open: (PhotoEntry) -> Unit) {
    val grid = rememberLazyGridState()
    LaunchedEffect(selected, photos.size) {
        if (photos.isNotEmpty()) grid.animateScrollToItem(selected.coerceIn(photos.indices))
    }
    when {
        needsPermission -> Column(Modifier.fillMaxSize().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text("사진첩을 보려면 읽기 권한이 필요합니다.", color = AlbumBlue, fontSize = 12.sp)
            Text("권한 허용", color = AlbumBlue, fontSize = 12.sp,
                modifier = Modifier.padding(10.dp).clickable(onClick = requestPermission))
        }
        loading -> AlbumMessage("사진첩 불러오는 중…")
        error != null -> AlbumMessage(error)
        photos.isEmpty() -> AlbumMessage("아직 사진이 없습니다.\n카메라에서 첫 사진을 찍어보세요.")
        else -> LazyVerticalGrid(columns = GridCells.Fixed(3), state = grid,
            contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, entry ->
                val loaded = rememberPhoto(entry, repository, 160)
                Column(Modifier.fillMaxWidth().border(if (selected == index) 2.dp else 1.dp,
                    if (selected == index) AlbumBlue else Color(0xFF93A8BA))
                    .clickable { select(index); open(entry) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(Color.Black),
                        contentAlignment = Alignment.Center) {
                        loaded.bitmap?.let { Image(it.asImageBitmap(), "사진 ${index + 1}",
                            Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = LocalPhoneSkin.current.imageFilter) }
                            ?: Text(if (loaded.failed) "!" else "…", color = Color.White, fontSize = 12.sp)
                    }
                    Text(SimpleDateFormat("MM.dd", Locale.US).format(Date(entry.takenAt)), color = AlbumBlue,
                        fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun PhotoViewer(entry: PhotoEntry?, repository: PhotoRepository) {
    if (entry == null) { AlbumMessage("사진을 선택해 주세요."); return }
    val loaded = rememberPhoto(entry, repository, 1280)
    loaded.bitmap?.let { Image(it.asImageBitmap(), "사진", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = LocalPhoneSkin.current.imageFilter) }
        ?: AlbumMessage(if (loaded.failed) "사진을 열 수 없습니다.\n갤러리에서 삭제되었을 수 있습니다." else "사진 불러오는 중…", if (LocalPhoneSkin.current == PhoneSkin.STARTAC) LocalPhoneSkin.current.ink else Color.White)
}

@Composable
private fun AlbumMessage(text: String, color: Color? = null) {
    Text(text, color = color ?: AlbumBlue, fontSize = 12.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(7.dp))
}
