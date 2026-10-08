package com.aliaygor.taptoflip
import java.util.Locale
internal fun gameText(tr: String, en: String): String = if (Locale.getDefault().language == "tr") tr else en
