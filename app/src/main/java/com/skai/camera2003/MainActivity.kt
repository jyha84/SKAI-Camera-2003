package com.skai.camera2003

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Lcd: Color @Composable get() = LocalPhoneSkin.current.lcd
private val Ink: Color @Composable get() = LocalPhoneSkin.current.ink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink)) {
                ProvideTextStyle(TextStyle(fontFamily = LcdFont, fontSize = 12.sp, lineHeight = 16.sp)) {
                    CameraScreen()
                }
            }
        }
    }
}

@Composable
private fun CameraScreen() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("ui", Context.MODE_PRIVATE) }
    var skin by remember { mutableStateOf(PhoneSkin.fromId(preferences.getString("phone_skin", null))) }
    CompositionLocalProvider(LocalPhoneSkin provides skin) {
        CameraContent(skin) {
            skin = it
            preferences.edit().putString("phone_skin", it.id).apply()
        }
    }
}

@Composable
private fun CameraContent(skin: PhoneSkin, setSkin: (PhoneSkin) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val uiPreferences = remember(context) { context.getSharedPreferences("ui", Context.MODE_PRIVATE) }
    var showKeyCaptions by remember { mutableStateOf(uiPreferences.getBoolean("key_captions", false)) }
    fun allowed(permission: String) = ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(allowed(Manifest.permission.CAMERA)) }
    val camera = remember { RetroCamera(context.applicationContext) }
    var screen by remember { mutableIntStateOf(0) } // camera, menu, photo
    var selected by remember { mutableIntStateOf(0) }
    var skinSelected by remember { mutableIntStateOf(skin.ordinal) }
    val repository = remember { PhotoRepository(context.applicationContext) }
    var albumPhotos by remember { mutableStateOf<List<PhotoEntry>>(emptyList()) }
    var albumSelected by remember { mutableIntStateOf(0) }
    var activePhoto by remember { mutableStateOf<PhotoEntry?>(null) }
    var albumLoading by remember { mutableStateOf(false) }
    var albumError by remember { mutableStateOf<String?>(null) }
    var readAllowed by remember { mutableStateOf(Build.VERSION.SDK_INT >= 29 || allowed(Manifest.permission.READ_EXTERNAL_STORAGE)) }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        readAllowed = it
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) camera.shoot() else camera.message = "사진 저장 권한이 필요합니다."
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            activePhoto = PhotoEntry(uri, "선택한 사진", System.currentTimeMillis())
            screen = 2
        }
    }
    LaunchedEffect(screen, camera.busy, readAllowed) {
        if (screen == 4 && readAllowed && !camera.busy) {
            albumLoading = true
            albumError = null
            try {
                albumPhotos = withContext(Dispatchers.IO) { repository.listPhotos() }
                albumSelected = albumSelected.coerceIn(0, maxOf(0, albumPhotos.lastIndex))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { albumError = "사진첩을 불러올 수 없습니다. 다시 열어 주세요." }
            finally { albumLoading = false }
        }
    }
    fun openPhoto(photo: PhotoEntry) { activePhoto = photo; screen = 2 }
    fun sharePhoto() {
        val photo = activePhoto ?: return
        try {
            val mime = context.contentResolver.getType(photo.uri) ?: "image/jpeg"
            context.startActivity(Intent.createChooser(PhotoRepository.shareIntent(photo.uri, mime), "사진 공유"))
        } catch (_: Exception) { albumError = "사진을 공유할 수 없습니다. 사진첩에서 다시 선택해 주세요." }
    }
    fun previous() { screen = if (screen == 2) 4 else if (screen == 5) 1 else 0 }
    fun movePhoto(step: Int) {
        if (albumPhotos.isEmpty()) return
        val current = if (screen == 2) albumPhotos.indexOfFirst { it.uri == activePhoto?.uri } else albumSelected
        if (screen == 2 && current < 0) return
        albumSelected = (current + step).coerceIn(0, albumPhotos.lastIndex)
        if (screen == 2) activePhoto = albumPhotos[albumSelected]
    }
    fun shoot() {
        if (Build.VERSION.SDK_INT <= 28 && !allowed(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else camera.shoot()
    }
    fun changeSetting(index: Int) {
        if (index == 3) { skinSelected = skin.ordinal; screen = 5; return }
        if (index == 4) { screen = 3; return }
        val settings = camera.settings
        camera.updateSettings(when (index) {
            0 -> settings.copy(dateStamp = !settings.dateStamp)
            1 -> settings.copy(width = if (settings.width == 480) 240 else 480)
            else -> settings.copy(style = 1 - settings.style)
        })
    }
    fun photos() {
        if (camera.busy) { camera.message = "사진 저장 중입니다. 잠시 기다려 주세요."; return }
        albumSelected = 0
        albumError = null
        screen = 4
        if (!readAllowed) readPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    BackHandler(screen != 0) { previous() }
    LaunchedEffect(Unit) { if (!granted) cameraPermission.launch(Manifest.permission.CAMERA) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = allowed(Manifest.permission.CAMERA)
                readAllowed = Build.VERSION.SDK_INT >= 29 || allowed(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    // Keep an in-flight capture alive if the user opens a menu before saving finishes.
    val keepCamera = granted && (screen == 0 || camera.busy)
    DisposableEffect(camera, owner, keepCamera) {
        if (keepCamera) camera.bind(owner)
        onDispose { camera.unbind() }
    }
    DisposableEffect(camera) { onDispose { camera.close() } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF171819)).safeDrawingPadding()) {
        // The art is 1024x1536. Fit its height and center it, cropping only the background.
        val artHeight = maxHeight
        val artWidth = artHeight * (2f / 3f)
        val originX = (maxWidth - artWidth) / 2f
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painterResource(skin.image), null,
                Modifier.requiredSize(artWidth, artHeight), contentScale = ContentScale.FillBounds)
        }
        val lcdWidth = artWidth * skin.lcdW
        val lcdHeight = artHeight * skin.lcdH
        if (skin == PhoneSkin.STARTAC) {
            Column(Modifier.offset(x = originX + artWidth * skin.lcdX, y = artHeight * skin.lcdY)
                .size(lcdWidth, lcdHeight).clip(RoundedCornerShape(3.dp)).background(Lcd)) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when (screen) {
                        1 -> {
                            val items = listOf("날짜 표시" to if (camera.settings.dateStamp) "ON" else "OFF",
                                "사진 크기" to if (camera.settings.width == 480) "VGA" else "QVGA",
                                "사진 느낌" to if (camera.settings.style == 0) "2003" else "2005",
                                "스킨" to skin.title, "도움말 · 출처" to "보기")
                            CompactMenuItem("${selected + 1}. ${items[selected].first}", items[selected].second) { changeSetting(selected) }
                        }
                        5 -> CompactMenuItem("SKIN ${skinSelected + 1}/3", PhoneSkin.entries[skinSelected].title) {
                            setSkin(PhoneSkin.entries[skinSelected]); screen = 1
                        }
                        2 -> PhotoViewer(activePhoto, repository)
                        4 -> when {
                            !readAllowed -> CompactMenuItem("사진 읽기 권한", "OK / 터치") { readPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) }
                            albumLoading -> CompactLcdText("불러오는 중…")
                            albumError != null -> CompactLcdText(albumError!!)
                            albumPhotos.isEmpty() -> CompactLcdText("사진 없음 · 5 촬영")
                            else -> PhotoViewer(albumPhotos.getOrNull(albumSelected), repository)
                        }
                        3 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(2.dp)) {
                            CompactLcdText("2 ↑ / 8 ↓ / 5·OK 선택\nMENU 이전 / MR 사진·공유\nM+ 아래 / 0 메뉴\n? 키 설명 토글\n사진: Myself, Commons\nCC BY 4.0 · 변형\n폰트: quiple, OFL 1.1")
                            Text("출처 ↗", fontSize = 10.sp, color = Ink, modifier = Modifier.clickable {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                    "https://commons.wikimedia.org/wiki/File:Startac_130_Movistar.jpg".toUri())) }
                                catch (_: Exception) { camera.message = "브라우저 없음" }
                            })
                        }
                        else -> if (granted) {
                            camera.frame?.let { Image(it.asImageBitmap(), "카메라 프리뷰", Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit, colorFilter = skin.imageFilter) }
                                ?: CompactLcdText(camera.message.ifBlank { "연결 중…" })
                        } else Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("권한", fontSize = 10.sp, color = Ink, modifier = Modifier.clickable { cameraPermission.launch(Manifest.permission.CAMERA) })
                            Text("설정", fontSize = 10.sp, color = Ink, modifier = Modifier.clickable {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                            })
                        }
                    }
                }
                Text(when (screen) {
                    1, 5 -> "2↑ 8↓ 5선택"
                    2 -> "MR 공유 · MENU 이전"
                    4 -> if (albumPhotos.isEmpty()) "MR 사진첩" else "${albumSelected + 1}/${albumPhotos.size} · OK 열기"
                    3 -> "SKAI 2003"
                    else -> if (camera.busy) "저장 중…" else if (camera.message.isNotBlank()) camera.message
                        else "${if (camera.settings.width == 480) "VGA" else "QVGA"} DATE ${if (camera.settings.dateStamp) "ON" else "OFF"}"
                }, fontFamily = LcdFont, fontSize = 8.sp, lineHeight = 9.sp, color = Ink, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().background(skin.bar), textAlign = TextAlign.Center)
            }
        } else Column(Modifier.offset(x = originX + artWidth * skin.lcdX, y = artHeight * skin.lcdY)
            .size(lcdWidth, lcdHeight).clip(RoundedCornerShape(5.dp)).background(Color.Black)) {
            Row(Modifier.fillMaxWidth().background(Lcd).padding(horizontal = 5.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(when (screen) { 0 -> "CAMERA"; 1 -> "MENU"; 2 -> "PHOTO"; 4 -> "ALBUM"; 5 -> "SKIN"; else -> "SKAI" },
                    color = Ink, fontSize = 11.sp, fontFamily = LcdFont)
                Text(SimpleDateFormat("HH:mm", Locale.US).format(Date()), color = Ink,
                    fontSize = 11.sp, fontFamily = LcdFont)
            }
            Box(Modifier.fillMaxWidth().weight(1f)
                .background(if (screen == 1 || screen == 3 || screen == 4 || screen == 5) Lcd else Color.Black), contentAlignment = Alignment.Center) {
                when (screen) {
                    1 -> SettingsScreen(camera.settings, skin.title, selected, { selected = it }, { changeSetting(it) })
                    5 -> SkinPicker(skinSelected) { skinSelected = it; setSkin(PhoneSkin.entries[it]); screen = 1 }
                    2 -> PhotoViewer(activePhoto, repository)
                    4 -> PhotoAlbum(albumPhotos, albumSelected, albumLoading, albumError, !readAllowed, repository,
                        { readPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE) },
                        { albumSelected = it }, { openPhoto(it) })
                    3 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(7.dp)) {
                        Text("SKAI Camera 2003", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                        Text("? : 키 설명 켜기/끄기\nOK · 5 : 촬영/사진 열기\n사진첩 ◀▶▲▼ 이동\n사진 오른쪽 키: 공유\n1 : 날짜 / 2 : 크기·위\n3 · ◀▶ : 사진 느낌\n8 : 아래 / 메뉴 → 스킨\n\n사진 기반 스킨\nSamsung: putnik / BY-SA 3.0\nNokia: J-P Kärnä / BY-SA 3.0\nStarTAC: Myself / BY 4.0\nWikimedia Commons\nSKAI용 이미지 변형\n\n폰트: 갈무리 / quiple\nSIL OFL 1.1", color = Ink, fontSize = 11.sp)
                        Text("출처 / 라이선스 ↗", color = Ink, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp).clickable {
                                try { context.startActivity(Intent(Intent.ACTION_VIEW,
                                    "https://commons.wikimedia.org/wiki/File:Samsung_SGH-X100.jpg".toUri())) }
                                catch (_: Exception) { camera.message = "브라우저를 찾을 수 없습니다." }
                            })
                    }
                    else -> if (granted) {
                        camera.frame?.let { Image(it.asImageBitmap(), "카메라 프리뷰",
                            Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = skin.imageFilter) }
                            ?: LcdMessage(camera.message.ifBlank { "카메라 연결 중…" })
                    } else Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(4.dp)) {
                        LcdMessage("카메라 권한이 필요합니다.")
                        Text("권한 허용", color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp).clickable { cameraPermission.launch(Manifest.permission.CAMERA) })
                        Text("앱 설정", color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp).clickable {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    "package:${context.packageName}".toUri()))
                            })
                    }
                }
            }
            Text(when {
                screen == 1 -> "▲▼ 이동  OK 변경"
                screen == 3 -> "SKAI / 2003"
                screen == 5 -> "▲▼ 선택  OK 적용"
                screen == 2 -> albumError ?: "◀▶ 이전 / 다음 사진"
                screen == 4 -> if (albumLoading) "불러오는 중…" else "사진 ${albumPhotos.size}장 · OK 열기"
                camera.busy -> "저장 중…"
                camera.message.isNotBlank() -> camera.message
                else -> "${if (camera.settings.width == 480) "VGA" else "QVGA"} · DATE ${if (camera.settings.dateStamp) "ON" else "OFF"}"
            }, Modifier.fillMaxWidth().background(Lcd).padding(horizontal = 3.dp, vertical = 2.dp),
                color = Ink, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
            Row(Modifier.fillMaxWidth().background(skin.bar).padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (screen == 0) "메뉴" else "이전", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                Text(if (screen == 0) "촬영" else if (screen == 1) "변경" else if (screen == 4) "열기" else "사진첩", color = Ink, fontSize = 11.sp)
                Text(if (screen == 0) "사진" else if (screen == 2) "공유" else if (screen == 4) "찾기" else "완료", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }
        fun centerAction() {
            when (screen) {
                0 -> if (granted && camera.ready && !camera.busy) shoot()
                1 -> changeSetting(selected)
                5 -> { setSkin(PhoneSkin.entries[skinSelected]); screen = 1 }
                4 -> if (!readAllowed) readPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    else albumPhotos.getOrNull(albumSelected)?.let { openPhoto(it) }
                2 -> screen = 4
                else -> screen = 0
            }
        }
        fun direction(up: Boolean) {
            when (screen) {
                1 -> selected = (selected + if (up) 4 else 1) % 5
                5 -> skinSelected = (skinSelected + if (up) 2 else 1) % 3
                4 -> movePhoto(if (skin == PhoneSkin.STARTAC) { if (up) -1 else 1 } else { if (up) -3 else 3 })
                2 -> movePhoto(if (up) -1 else 1)
                3 -> screen = 1
                else -> screen = 1
            }
        }
        fun tone() {
            if (screen == 1) changeSetting(selected)
            else if (screen == 0) camera.updateSettings(camera.settings.copy(style = 1 - camera.settings.style))
        }
        @Composable fun key(label: String, x: Float, y: Float, w: Float = 0.11f, h: Float = 0.04f,
                            enabled: Boolean = true, caption: String? = null, action: () -> Unit) {
            val mapped = if (skin == PhoneSkin.SILVER) SkinKey(x,y,w,h) else skin.key(label) ?: return
            PhotoKey(label, Modifier.offset(x = originX + artWidth * (mapped.x - mapped.w / 2), y = artHeight * (mapped.y - mapped.h / 2))
                .size(artWidth * mapped.w, artHeight * mapped.h), enabled, if (showKeyCaptions) caption else null, action)
        }
        // A small engraved '?' sits in the unused metal area beside the SKAI name.
        // The touch target is larger than the engraving and does not replace a keypad action.
        EngravedHelpKey(showKeyCaptions,
            Modifier.offset(x = originX + artWidth * skin.helpX - 24.dp, y = artHeight * skin.helpY - 24.dp)
                .size(48.dp)) { enabled ->
            showKeyCaptions = enabled
            uiPreferences.edit().putBoolean("key_captions", enabled).apply()
        }
        key("메뉴 / 이전", .332f, .533f, .13f, .056f) { if (screen == 0) screen = 1 else previous() }
        key("사진첩 / 공유 / 찾기 / 완료", .664f, .533f, .13f, .056f) {
            when (screen) { 0 -> photos(); 2 -> sharePhoto(); 4 -> photoPicker.launch("image/*"); else -> screen = 0 }
        }
        key("위", .5f, .517f, .067f, .03f) { direction(true) }
        key("아래", .5f, .607f, .067f, .03f) { direction(false) }
        key("왼쪽", .432f, .56f, .055f, .048f) { if (screen == 2 || screen == 4) movePhoto(-1) else tone() }
        key("오른쪽", .568f, .56f, .055f, .048f) { if (screen == 2 || screen == 4) movePhoto(1) else tone() }
        key("OK 촬영 / 선택", .5f, .563f, .087f, .06f,
            screen != 0 || (granted && camera.ready && !camera.busy)) { centerAction() }
        key("카메라", .35f, .616f, .15f, .059f) { screen = 0 }
        key("닫기", .657f, .616f, .15f, .059f) { screen = 0 }
        if (skin == PhoneSkin.SILVER) key("C 이전", .5f, .65f, .15f, .037f) { previous() }
        key("1 날짜 표시", .352f, .687f, .15f, caption = "날짜") { changeSetting(0) }
        key("2 사진 크기", .498f, .703f, .15f, caption = if (screen == 0) "크기" else "위") { if (screen == 0) changeSetting(1) else direction(true) }
        key("3 사진 느낌", .653f, .687f, .15f, caption = "색감") { changeSetting(2) }
        key("4 왼쪽", .346f, .743f, .15f) { if (screen == 2 || screen == 4) movePhoto(-1) else tone() }
        key("5 촬영", .498f, .757f, .15f, enabled = screen != 0 || (granted && camera.ready && !camera.busy),
            caption = when (screen) { 0 -> "촬영"; 1 -> "선택"; 4 -> "열기"; else -> "사진첩" }) { centerAction() }
        key("6 오른쪽", .658f, .743f, .15f) { if (screen == 2 || screen == 4) movePhoto(1) else tone() }
        key("7 사진", .344f, .796f, .15f, caption = "사진첩") { photos() }
        key("8 아래", .498f, .812f, .15f) { direction(false) }
        key("9 사진 찾기", .659f, .796f, .15f, caption = "찾기") { photoPicker.launch("image/*") }
        key("별 날짜", .35f, .85f, .15f) { changeSetting(0) }
        key("0 메뉴", .498f, .868f, .15f, caption = "메뉴") { screen = 1 }
        key("샵 사진 느낌", .657f, .853f, .15f) { changeSetting(2) }
    }
}

@Composable
private fun LcdMessage(text: String) {
    Text(text, color = Color(0xFFD3DFE5), fontSize = 13.sp,
        textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
}

@Composable
private fun SettingsScreen(settings: PhotoSettings, skinTitle: String, selected: Int, select: (Int) -> Unit, change: (Int) -> Unit) {
    val requests = remember { List(5) { BringIntoViewRequester() } }
    LaunchedEffect(selected) { requests[selected].bringIntoView() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(4.dp)) {
        val rows = listOf("날짜 표시" to if (settings.dateStamp) "켜기" else "끄기",
            "사진 크기" to if (settings.width == 480) "VGA 480×640" else "QVGA 240×320",
            "사진 느낌" to if (settings.style == 0) "피처폰 2003" else "디카 2005",
            "스킨" to skinTitle,
            "도움말 · 출처" to "보기")
        rows.forEachIndexed { index, item ->
            Column(Modifier.fillMaxWidth().bringIntoViewRequester(requests[index])
                .background(if (selected == index) Ink else Color.Transparent)
                .clickable { select(index); change(index) }.padding(horizontal = 5.dp, vertical = 6.dp)) {
                Text("${index + 1}. ${item.first}", color = if (selected == index) Color.White else Ink,
                    fontFamily = LcdFont, fontSize = 12.sp, lineHeight = 16.sp)
                Text("< ${item.second} >", color = if (selected == index) Lcd else Ink,
                    fontFamily = LcdFont, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun PhotoKey(label: String, modifier: Modifier, enabled: Boolean, caption: String?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    Box(modifier.clip(RoundedCornerShape(50)).background(if (pressed) Color.White.copy(alpha = 0.22f) else Color.Transparent)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button,
            onClickLabel = label, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() })
        .semantics { contentDescription = label }, contentAlignment = Alignment.BottomCenter) {
        if (caption != null) {
            Text(caption, color = Color(0xFF173247), fontFamily = KeyCaptionFont,
                fontSize = 7.sp, lineHeight = 9.sp, maxLines = 1, textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 1.dp).clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE8EDF1).copy(alpha = 0.94f))
                    .padding(horizontal = 3.dp).clearAndSetSemantics { })
        }
    }
}

@Composable
private fun EngravedHelpKey(enabled: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    Box(modifier.toggleable(value = enabled, interactionSource = interaction, indication = null,
        role = Role.Switch, onValueChange = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange(it)
        }).semantics { contentDescription = "키 설명 표시" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(22.dp).clearAndSetSemantics { }) {
            val radius = size.minDimension / 2 - 1.dp.toPx()
            if (pressed) drawCircle(Color.White.copy(alpha = .16f), radius)
            drawCircle(Color.White.copy(alpha = .45f), radius,
                center = center + Offset(0f, .7.dp.toPx()), style = Stroke(.8.dp.toPx()))
            drawCircle(Color(0xFF3E4A56).copy(alpha = if (enabled) .6f else .28f), radius,
                style = Stroke(.7.dp.toPx()))
        }
        Text("?", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
            fontSize = 13.sp, lineHeight = 16.sp,
            style = TextStyle(shadow = Shadow(Color.White.copy(alpha = .65f), Offset(0f, 1f), 0f)),
            color = if (enabled) Color(0xFF234F70) else Color(0xFF56606A),
            modifier = Modifier.clearAndSetSemantics { })
    }
}

@Composable
private fun SkinPicker(selected: Int, choose: (Int) -> Unit) {
    val requests = remember { List(3) { BringIntoViewRequester() } }
    LaunchedEffect(selected) { requests[selected].bringIntoView() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(4.dp)) {
        PhoneSkin.entries.forEachIndexed { index, skin ->
            Text("${index + 1}. ${skin.title}", color = if (selected == index) Lcd else Ink,
                fontSize = 12.sp, modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requests[index])
                    .background(if (selected == index) Ink else Color.Transparent)
                    .clickable { choose(index) }.padding(horizontal = 4.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun CompactLcdText(text: String) {
    Text(text, color = LocalPhoneSkin.current.ink, fontFamily = LcdFont, fontSize = 10.sp,
        lineHeight = 12.sp, modifier = Modifier.padding(horizontal = 2.dp), textAlign = TextAlign.Center)
}

@Composable
private fun CompactMenuItem(title: String, value: String, choose: () -> Unit) {
    Column(Modifier.fillMaxSize().clickable(onClick = choose), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(title, color = LocalPhoneSkin.current.ink, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
        Text("< $value >", color = LocalPhoneSkin.current.ink, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)
    }
}
