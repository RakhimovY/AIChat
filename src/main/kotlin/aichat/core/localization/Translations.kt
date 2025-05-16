package aichat.core.localization

/**
 * Translations for system messages
 */
object Translations {
    private val translations = mapOf(
        "ru" to mapOf(
            "error" to "Произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте еще раз позже.",
            "defaultTitle" to "Новый чат"
        ),
        "kk" to mapOf(
            "error" to "Сұрауыңызды өңдеу кезінде қате орын алды. Кейінірек қайталап көріңіз.",
            "defaultTitle" to "Жаңа чат"
        ),
        "en" to mapOf(
            "error" to "An error occurred while processing your request. Please try again later.",
            "defaultTitle" to "New Chat"
        )
    )

    /**
     * Get a translation for a key in the specified language
     * @param key The translation key
     * @param language The language code (ru, kk, en)
     * @return The translated string or the Russian translation if the language is not supported
     */
    fun get(key: String, language: String?): String {
        // Default to Russian if language is null or not supported
        val lang = if (language != null && translations.containsKey(language)) language else "ru"
        return translations[lang]?.get(key) ?: translations["ru"]?.get(key) ?: key
    }
}