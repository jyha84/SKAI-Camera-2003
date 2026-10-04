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
