# SKAI Camera 2003 v0.10 — 전체 코드

사진 스킨 PNG와 갈무리 폰트는 소스 ZIP에 포함되어 있습니다.

## `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "SKAI Camera 2003"
include(":app")

```

## `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application") version "8.9.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}

```

## `gradle.properties`

```text
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official

```

## `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.skai.camera2003"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skai.camera2003"
        minSdk = 23
        targetSdk = 35
        versionCode = 10
        versionName = "0.10.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
}

```

## `app/src/main/java/com/skai/camera2003/LcdTypography.kt`

```kotlin
package com.skai.camera2003

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Korean pixel fonts, bundled locally under SIL OFL 1.1. */
val LcdFont = FontFamily(Font(R.font.galmuri11, weight = FontWeight.Normal))
val KeyCaptionFont = FontFamily(Font(R.font.galmuri9, weight = FontWeight.Normal))

```

## `app/src/main/java/com/skai/camera2003/MainActivity.kt`

```kotlin
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

```

## `app/src/main/java/com/skai/camera2003/PhoneSkin.kt`

```kotlin
package com.skai.camera2003

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

data class SkinKey(val x: Float, val y: Float, val w: Float = .14f, val h: Float = .045f)
enum class PhoneSkin(val id: String, val title: String, val image: Int,
    val lcdX: Float, val lcdY: Float, val lcdW: Float, val lcdH: Float,
    val helpX: Float, val helpY: Float) {
    SILVER("silver", "실버 2003", R.drawable.phone_skin, .308f, .149f, .381f, .282f, .642f, .473f),
    STARTAC("startac", "스타택 · 초록 LCD", R.drawable.phone_startac, .380f, .535f, .238f, .071f, .65f, .385f),
    NOKIA("nokia", "노키아 3310", R.drawable.phone_nokia, .298f, .250f, .392f, .196f, .65f, .195f);
    val lcd: Color get() = if (this == SILVER) Color(0xFFD3DFE5) else Color(0xFFA9BD79)
    val ink: Color get() = if (this == SILVER) Color(0xFF17364E) else Color(0xFF243419)
    val bar: Color get() = if (this == SILVER) Color(0xFFAEBFD0) else Color(0xFF8FA762)
    val imageFilter: ColorFilter? get() = if (this == SILVER) null else ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        .18f,.35f,.07f,0f,30f,
        .22f,.43f,.09f,0f,40f,
        .10f,.20f,.04f,0f,15f,
        0f,0f,0f,1f,0f)))
    fun key(label: String): SkinKey? {
        if (this == SILVER) return null
        val name = label.substringBefore(" ")
        val number = name.toIntOrNull()
        val column = when (number) { 1,4,7 -> 0; 2,5,8,0 -> 1; 3,6,9 -> 2; else -> if (name == "별") 0 else 2 }
        if (number != null || name == "별" || name == "샵") {
            val row = when (number) { 1,2,3 -> 0; 4,5,6 -> 1; 7,8,9 -> 2; else -> 3 }
            return SkinKey((if (this == STARTAC) .395f else .334f) + column * (if (this == STARTAC) .105f else .164f), (if (this == STARTAC) .659f else .651f) + row * (if (this == STARTAC) .0435f else .072f) + (if (this == NOKIA && column == 1) .012f else 0f), if (this == STARTAC) .089f else .14f)
        }
        return if (this == STARTAC) when (name) {
            "메뉴" -> SkinKey(.600f,.837f,.088f,.033f)
            "사진첩" -> SkinKey(.398f,.837f,.088f,.033f)
            "아래" -> SkinKey(.500f,.837f,.088f,.033f)
            "닫기" -> SkinKey(.400f,.882f,.084f,.033f)
            "위" -> SkinKey(.500f,.882f,.084f,.033f)
            "OK" -> SkinKey(.598f,.882f,.084f,.033f)
            else -> null
        } else when (name) {
            "메뉴" -> SkinKey(.35f,.55f,.12f,.065f)
            "OK" -> SkinKey(.50f,.51f,.21f,.047f)
            "위" -> SkinKey(.658f,.538f,.10f,.031f)
            "아래" -> SkinKey(.61f,.58f,.10f,.031f)
            else -> null
        }
    }
    companion object { fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: SILVER }
}
val LocalPhoneSkin = staticCompositionLocalOf { PhoneSkin.SILVER }

```

## `app/src/main/java/com/skai/camera2003/PhotoAlbum.kt`

```kotlin
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

```

## `app/src/main/java/com/skai/camera2003/PhotoRepository.kt`

```kotlin
package com.skai.camera2003

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoEntry(val uri: Uri, val name: String, val takenAt: Long)

class PhotoRepository(private val context: Context) {
    /** On Android 10+ the platform exposes this app's own MediaStore photos without a read permission. */
    fun listPhotos(): List<PhotoEntry> {
        if (Build.VERSION.SDK_INT >= 29) {
            val columns = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_ADDED)
            return context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns, "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.IS_PENDING} = 0",
                arrayOf("Pictures/SKAI 2003/"), "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC")
                ?.use { readRows(it) } ?: emptyList()
        }
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SKAI 2003")
        if (directory.exists() && !directory.canRead()) throw SecurityException("Photo directory is not readable")
        return directory.listFiles().orEmpty().filter { it.isFile && isSkaiPhoto(it.name) }
            .map { PhotoEntry(FileProvider.getUriForFile(context, "${context.packageName}.photos", it),
                it.name, filenameDate(it.name) ?: it.lastModified()) }
            .sortedWith(compareByDescending<PhotoEntry> { it.takenAt }.thenByDescending { it.name })
    }

    fun loadBitmap(uri: Uri, maxEdge: Int = 640): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); Unit }
            ?: error("Photo is no longer available")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not a decodable image" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Cannot decode photo")
    }

    companion object {
        fun isSkaiPhoto(name: String) = name.startsWith("SKAI_") && name.endsWith(".jpg", ignoreCase = true)
        private fun filenameDate(name: String): Long? = try {
            SimpleDateFormat("'SKAI_'yyyyMMdd_HHmmss_SSS'.jpg'", Locale.US)
                .apply { isLenient = false }.parse(name)?.time
        } catch (_: Exception) { null }

        internal fun readRows(cursor: Cursor): List<PhotoEntry> {
            val id = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val taken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val added = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val result = mutableListOf<PhotoEntry>()
            while (cursor.moveToNext()) {
                val title = cursor.getString(name) ?: continue
                if (!isSkaiPhoto(title)) continue
                val date = cursor.getLong(taken).takeIf { it > 0 } ?: filenameDate(title) ?: cursor.getLong(added) * 1000
                result += PhotoEntry(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(id)), title, date)
            }
            return result.sortedWith(compareByDescending<PhotoEntry> { it.takenAt }.thenByDescending { it.name })
        }

        /** Grants the chosen target temporary read access to the original JPEG, including its date stamp. */
        fun shareIntent(uri: Uri, mimeType: String = "image/jpeg"): Intent {
            require(uri.scheme == "content") { "Sharing requires a content URI" }
            return Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("SKAI photo", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}

```

## `app/src/main/java/com/skai/camera2003/RetroCamera.kt`

```kotlin
package com.skai.camera2003

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import android.media.MediaScannerConnection

class RetroCamera(private val context: Context) {
    var frame by mutableStateOf<Bitmap?>(null)
        private set
    var ready by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    private val preferences = context.getSharedPreferences("camera", Context.MODE_PRIVATE)
    var settings by mutableStateOf(PhotoSettings(
        preferences.getBoolean("date", true),
        preferences.getInt("width", 480).let { if (it == 240) 240 else 480 },
        preferences.getInt("style", 0).coerceIn(0, 1)))
        private set
    fun updateSettings(value: PhotoSettings) {
        settings = value
        preferences.edit().putBoolean("date", value.dateStamp)
            .putInt("width", value.width).putInt("style", value.style).apply()
    }
    var message by mutableStateOf("")
    private val main = ContextCompat.getMainExecutor(context)
    private val worker = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var capture: ImageCapture? = null
    private var analysis: ImageAnalysis? = null
    private var generation = 0
    private var closed = false
    private var lastFrame = 0L

    @Suppress("DEPRECATION")
    fun bind(owner: LifecycleOwner) {
        val current = ++generation
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!closed && current == generation) {
                try {
                    val cameras = future.get()
                    provider = cameras
                    val photo = ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(Surface.ROTATION_0)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                    val stream = ImageAnalysis.Builder().setTargetResolution(Size(640, 480))
                        .setTargetRotation(Surface.ROTATION_0)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    stream.setAnalyzer(worker) { image ->
                        try {
                            val now = System.currentTimeMillis()
                            if (now - lastFrame >= 110) {
                                lastFrame = now
                                val small = RetroImage.fromYuv(image)
                                val chosen = settings
                                val shown = RetroImage.finish(small, 240, Date(now), chosen.dateStamp, chosen.style)
                                small.recycle()
                                main.execute {
                                    if (!closed && current == generation) frame = shown
                                }
                            }
                        } catch (exception: Exception) {
                            Log.e("SKAI", "Preview processing failed", exception)
                        } finally { image.close() }
                    }
                    cameras.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, photo, stream)
                    capture = photo
                    analysis = stream
                    ready = true
                    message = ""
                } catch (exception: Exception) {
                    Log.e("SKAI", "Camera binding failed", exception)
                    message = "카메라를 시작할 수 없습니다. 앱을 다시 실행해 주세요."
                }
            }
        }, main)
    }

    fun shoot() {
        val camera = capture ?: return
        if (busy || closed) return
        busy = true
        message = ""
        val date = Date()
        val chosen = settings
        camera.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample })
                        ?: error("Cannot decode capture")
                    val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height,
                        Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, false)
                    if (rotated !== original) original.recycle()
                    val result = RetroImage.finish(rotated, chosen.width, date, chosen.dateStamp, chosen.style)
                    rotated.recycle()
                    try { save(result, date) } finally { result.recycle() }
                    main.execute {
                        busy = false
                        if (!closed) message = "사진 저장 완료 · 갤러리의 SKAI 2003 폴더"
                    }
                } catch (exception: Exception) {
                    Log.e("SKAI", "Saving failed", exception)
                    main.execute {
                        busy = false
                        if (!closed) message = "저장하지 못했습니다. 권한과 저장 공간을 확인해 주세요."
                    }
                } finally { image.close() }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("SKAI", "Capture failed", exception)
                main.execute {
                    busy = false
                    if (!closed) message = "촬영하지 못했습니다. 다시 시도해 주세요."
                }
            }
        })
    }

    private fun save(bitmap: Bitmap, date: Date) {
        val name = "SKAI_" + SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date) + ".jpg"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, date.time)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SKAI 2003")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create gallery entry")
            try {
                resolver.openOutputStream(uri)?.use {
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it))
                } ?: error("Cannot open photo")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                check(resolver.update(uri, values, null, null) > 0)
            } catch (exception: Exception) {
                resolver.delete(uri, null, null)
                throw exception
            }
        } else {
            @Suppress("DEPRECATION")
            val directory = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SKAI 2003")
            check(directory.exists() || directory.mkdirs())
            val file = File(directory, name)
            try {
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 55, it)) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"), null)
            } catch (exception: Exception) { file.delete(); throw exception }
        }
    }

    fun unbind() {
        generation++
        ready = false
        analysis?.clearAnalyzer()
        val useCases = listOfNotNull(capture, analysis)
        if (useCases.isNotEmpty()) provider?.unbind(*useCases.toTypedArray())
        capture = null
        analysis = null
        frame = null
    }

    fun close() {
        closed = true
        unbind()
        // Allow an in-flight photo to finish saving before the worker exits.
        worker.shutdown()
    }
}

/** Settings are frozen at shutter time and persisted between launches. */
data class PhotoSettings(val dateStamp: Boolean = true, val width: Int = 480, val style: Int = 0)

```

## `app/src/main/java/com/skai/camera2003/RetroImage.kt`

```kotlin
package com.skai.camera2003

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import androidx.camera.core.ImageProxy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Low-resolution digital sensor look, without film scratches or sepia. */
object RetroImage {
    fun fromYuv(image: ImageProxy): Bitmap {
        val step = maxOf(1, image.width / 240)
        val width = image.width / step
        val height = image.height / step
        val pixels = IntArray(width * height)
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        val yBuffer = y.buffer.duplicate()
        val uBuffer = u.buffer.duplicate()
        val vBuffer = v.buffer.duplicate()
        val yStart = yBuffer.position()
        val uStart = uBuffer.position()
        val vStart = vBuffer.position()
        for (row in 0 until height) {
            val sourceY = row * step
            for (col in 0 until width) {
                val sourceX = col * step
                val yy = (yBuffer.get(yStart + sourceY * y.rowStride + sourceX * y.pixelStride).toInt() and 255) - 16
                val uu = (uBuffer.get(uStart + sourceY / 2 * u.rowStride + sourceX / 2 * u.pixelStride).toInt() and 255) - 128
                val vv = (vBuffer.get(vStart + sourceY / 2 * v.rowStride + sourceX / 2 * v.pixelStride).toInt() and 255) - 128
                val luma = 298 * maxOf(0, yy)
                pixels[row * width + col] = Color.rgb(
                    ((luma + 409 * vv + 128) shr 8).coerceIn(0, 255),
                    ((luma - 100 * uu - 208 * vv + 128) shr 8).coerceIn(0, 255),
                    ((luma + 516 * uu + 128) shr 8).coerceIn(0, 255))
            }
        }
        val raw = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val rotated = Bitmap.createBitmap(raw, 0, 0, width, height,
            Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, false)
        if (rotated !== raw) raw.recycle()
        return rotated
    }

    fun finish(source: Bitmap, outputWidth: Int, date: Date, dateStamp: Boolean = true, style: Int = 0): Bitmap {
        val outputHeight = outputWidth * 4 / 3
        val cropWidth = minOf(source.width, source.height * 3 / 4)
        val cropHeight = minOf(source.height, source.width * 4 / 3)
        val cropped = Bitmap.createBitmap(source, (source.width - cropWidth) / 2,
            (source.height - cropHeight) / 2, cropWidth, cropHeight)
        // A 240 x 320 sensor, enlarged without smoothing for VGA output.
        val small = Bitmap.createScaledBitmap(cropped, 240, 320, false)
        val pixels = IntArray(240 * 320)
        small.getPixels(pixels, 0, 240, 0, 0, 240, 320)
        for (index in pixels.indices) {
            val color = pixels[index]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val gray = (r * 30 + g * 59 + b * 11) / 100
            val grain = ((index * 1103515245 + 2003) ushr 24 and 15) - 7
            val noise = if (style == 0) grain else grain / 2
            fun channel(value: Int, shift: Int): Int {
                val muted = gray + (value - gray) * if (style == 0) 0.82f else 0.95f
                return ((((muted - 128) * (if (style == 0) 1.12f else 1.02f) + 128 + noise + shift).toInt()
                    .coerceIn(0, 255) / 8) * 8).coerceIn(0, 255)
            }
            pixels[index] = Color.rgb(channel(r, if (style == 0) -2 else 3),
                channel(g, if (style == 0) 2 else 0), channel(b, if (style == 0) 4 else -2))
        }
        val filtered = Bitmap.createBitmap(pixels, 240, 320, Bitmap.Config.ARGB_8888)
        val scaled = Bitmap.createScaledBitmap(filtered, outputWidth, outputHeight, false)
        val output = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled !== filtered) scaled.recycle()
        filtered.recycle()
        if (small !== source && small !== cropped) small.recycle()
        if (cropped !== source) cropped.recycle()
        if (!dateStamp) return output
        val paint = Paint().apply {
            color = Color.rgb(255, 184, 65)
            textSize = outputWidth / 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = false
            textAlign = Paint.Align.RIGHT
        }
        val text = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(date)
        val canvas = Canvas(output)
        val margin = outputWidth / 30f
        paint.color = Color.BLACK
        canvas.drawText(text, outputWidth - margin + 1, outputHeight - margin + 1, paint)
        paint.color = Color.rgb(255, 184, 65)
        canvas.drawText(text, outputWidth - margin, outputHeight - margin, paint)
        return output
    }
}

```

## `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <application
        android:allowBackup="false"
        android:label="@string/app_name"
        android:icon="@drawable/ic_camera"
        android:supportsRtl="true"
        android:theme="@style/Theme.Skai">
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.photos"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/photo_paths" />
        </provider>
        <activity android:name=".MainActivity" android:exported="true" android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>

```

## `app/src/main/res/drawable/ic_camera.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="48dp" android:height="48dp" android:viewportWidth="48" android:viewportHeight="48">
    <path android:fillColor="#182521" android:pathData="M0,0h48v48h-48z" />
    <path android:fillColor="#B8C4A0" android:pathData="M7,14h34v25h-34zM14,9h14v5h-14z" />
    <path android:fillColor="#182521" android:pathData="M24,18a9,9 0,1 0,0 18a9,9 0,1 0,0 -18M33,17h5v4h-5z" />
    <path android:fillColor="#F4A54B" android:pathData="M21,24h6v6h-6z" />
</vector>

```

## `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">SKAI Camera 2003</string>
</resources>

```

## `app/src/main/res/values/themes.xml`

```xml
<resources>
    <style name="Theme.Skai" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:statusBarColor">#000000</item>
        <item name="android:navigationBarColor">#000000</item>
        <item name="android:windowActionModeOverlay">true</item>
    </style>
</resources>

```

## `app/src/main/res/xml/photo_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="skai_photos" path="Pictures/SKAI 2003/" />
</paths>

```
