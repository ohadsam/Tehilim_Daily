package com.ohadsam.tehilim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System fonts for now; user-selectable bundled Hebrew fonts arrive with the settings batch.
private val Base = Typography()

val TehilimTypography = Typography(
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
)

/** Reader text style; size/spacing become user settings later. */
val ReaderTextStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 22.sp,
    lineHeight = 36.sp,
)
