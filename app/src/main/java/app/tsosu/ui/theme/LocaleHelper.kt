package app.tsosu.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class LanguageOption {
    SYSTEM,
    ENGLISH,
    ZH_TW,
}

object LocaleHelper {
    fun current(): LanguageOption {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            tags.isEmpty() -> LanguageOption.SYSTEM
            tags.startsWith("zh") -> LanguageOption.ZH_TW
            else -> LanguageOption.ENGLISH
        }
    }

    fun apply(option: LanguageOption) {
        val locales = when (option) {
            LanguageOption.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            LanguageOption.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            LanguageOption.ZH_TW -> LocaleListCompat.forLanguageTags("zh-TW")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
