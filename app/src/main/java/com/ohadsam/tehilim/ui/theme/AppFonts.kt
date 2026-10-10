@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.ohadsam.tehilim.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ohadsam.tehilim.R

/** Reader fonts bundled with the app (all SIL OFL; licences in assets/licenses and the About screen). */
enum class ReaderFont(val key: String, val nameHe: String, val family: FontFamily) {
    SYSTEM("system", "ברירת מחדל של המערכת", FontFamily.Default),
    FRANK("frank", "פרנק רויל", variable(R.font.frank_ruhl_libre)),
    DAVID("david", "דוד", FontFamily(Font(R.font.david_libre_regular, FontWeight.Normal), Font(R.font.david_libre_bold, FontWeight.Bold))),
    NOTO_SERIF("noto_serif", "נוטו סריף עברית (תומך בטעמים)", variable(R.font.noto_serif_hebrew)),
    ALEF("alef", "אלף", FontFamily(Font(R.font.alef_regular, FontWeight.Normal), Font(R.font.alef_bold, FontWeight.Bold))),
    HEEBO("heebo", "היבו", variable(R.font.heebo)),
    ASSISTANT("assistant", "אסיסטנט", variable(R.font.assistant)),
    RUBIK("rubik", "רוביק", variable(R.font.rubik)),
    ;

    companion object {
        fun fromKey(key: String?): ReaderFont = entries.firstOrNull { it.key == key } ?: FRANK
    }
}

private fun variable(res: Int): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.Bold).map { w ->
        Font(res, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
    },
)
