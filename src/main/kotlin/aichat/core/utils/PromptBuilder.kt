package aichat.core.utils

/**
 * Utility class for building AI prompts
 */
class PromptBuilder {
    companion object {
        // Pre-built language name mapping to avoid repeated string operations
        private val languageNameMap = mapOf(
            "ru" to "русском",
            "kk" to "казахском",
            "en" to "английском"
        )

        // Cache for country name mapping to avoid repeated string operations
        private val countryNameCache = mutableMapOf<String, String>()

        /**
         * Static system prompt to avoid recreating it for each request
         */
        private const val SYSTEM_PROMPT = """
            Ты - высококвалифицированный юридический ассистент с глубоким знанием правовых систем разных стран.
            Твоя задача - предоставлять точные, подробные и хорошо структурированные ответы на юридические вопросы.

            Содержание и структура ответов:
            1. Предоставляй точную и актуальную информацию, основанную на действующем законодательстве страны пользователя
            2. Всегда цитируй конкретные статьи и пункты законов в формате "Статья X, пункт Y" с правильной нумерацией
            3. Объясняй юридические термины простым и понятным языком, сохраняя юридическую точность
            4. Предоставляй исчерпывающие ответы, рассматривая различные аспекты вопроса
            5. Если вопрос касается толкования законов, указывай на возможные различные интерпретации
            6. Если вопрос выходит за рамки правовой тематики, укажи это и предложи общую информацию
            7. Если не можешь найти ответ в законодательстве, честно признай это и предложи, где пользователь может найти нужную информацию
            8. Сохраняй профессиональный, но доступный тон общения

            Форматирование для лучшей читаемости:
            1. Структурируй ответы логически, разделяя параграфы пустой строкой
            2. Выделяй важные моменты **двойными звездочками** вокруг текста
            3. Используй списки для перечисления пунктов:
               - Дефисы для ненумерованных списков
               - Цифры с точкой для нумерованных списков

            Помни, что твои ответы должны быть информативными, хорошо структурированными и полезными для пользователя.
        """

        /**
         * Function to map ISO country codes to full country names with caching
         * @param code ISO country code
         * @return Full country name
         */
        private fun getCountryNameByCode(code: String): String {
            // Check if the country name is already in the cache
            return countryNameCache.getOrPut(code) {
                when (code) {
                    "RU" -> "Россия"
                    "KZ" -> "Казахстан"
                    "BY" -> "Беларусь"
                    "UA" -> "Украина"
                    "UZ" -> "Узбекистан"
                    "KG" -> "Кыргызстан"
                    "TJ" -> "Таджикистан"
                    "TM" -> "Туркменистан"
                    "AZ" -> "Азербайджан"
                    "AM" -> "Армения"
                    "GE" -> "Грузия"
                    "MD" -> "Молдова"
                    "US" -> "США"
                    "GB" -> "Великобритания"
                    "DE" -> "Германия"
                    "FR" -> "Франция"
                    "IT" -> "Италия"
                    "ES" -> "Испания"
                    "CN" -> "Китай"
                    "JP" -> "Япония"
                    "IN" -> "Индия"
                    "BR" -> "Бразилия"
                    "CA" -> "Канада"
                    "AU" -> "Австралия"
                    else -> code // Return the code itself if not found in the mapping
                }
            }
        }

        /**
         * Builds a system prompt with country and language instructions
         * @param country Country code (optional)
         * @param language Language code (optional)
         * @return Complete system prompt
         */
        fun buildSystemPrompt(country: String?, language: String?): String {
            val promptBuilder = StringBuilder(SYSTEM_PROMPT.length + 500)

            // Start with system prompt
            promptBuilder.append(SYSTEM_PROMPT)
            promptBuilder.append("\n\n")

            // Add country-specific instructions if country is provided
            if (country != null) {
                val countryName = getCountryNameByCode(country)
                promptBuilder.append("Пользователь находится в стране: ")
                    .append(countryName)
                    .append(".\n")
                    .append("Адаптируй свой ответ к правовой системе и законодательству этой страны.\n")
                    .append("Используй актуальные законы, нормативные акты и правовые документы этой страны.\n")
                    .append("Если цитируешь законы, конституцию или другие правовые документы, указывай их точные названия и номера статей.\n\n")
            }

            // Add language-specific instructions if language is provided
            if (language != null) {
                val languageName = languageNameMap[language] ?: "русском"
                promptBuilder.append("Пользователь предпочитает общение на ")
                    .append(languageName)
                    .append(" языке.\n")
                    .append("Пожалуйста, отвечай на ")
                    .append(languageName)
                    .append(" языке.\n\n")
            }

            return promptBuilder.toString()
        }

        /**
         * Builds a complete prompt with document content and user message
         * @param country Country code (optional)
         * @param language Language code (optional)
         * @param documentContent Document content (optional)
         * @param imageInfo Image information (optional)
         * @param userMessage User message
         * @return Complete prompt
         */
        fun buildCompletePrompt(
            country: String?,
            language: String?,
            documentContent: String?,
            imageInfo: String?,
            userMessage: String
        ): String {
            val systemPrompt = buildSystemPrompt(country, language)
            val promptBuilder = StringBuilder(systemPrompt.length + 1000)

            // Start with system prompt
            promptBuilder.append(systemPrompt)

            // Add document content if provided
            if (documentContent != null) {
                promptBuilder.append("Пользователь предоставил следующий документ. Используй информацию из него для ответа на вопрос:\n\n")
                    .append("--- НАЧАЛО ДОКУМЕНТА ---\n")
                    .append(documentContent)
                    .append("\n--- КОНЕЦ ДОКУМЕНТА ---\n\n")
                    .append("Анализируй информацию из документа и используй ее для ответа на вопрос пользователя. ")
                    .append("Если документ содержит релевантную информацию, ссылайся на нее в своем ответе.\n\n")
            }

            // Add image information if provided
            if (imageInfo != null) {
                promptBuilder.append("Пользователь предоставил изображение: ")
                    .append(imageInfo)
                    .append("\n")
                    .append("Пожалуйста, учти, что я не могу видеть изображение, но пользователь может описать его в своем вопросе. ")
                    .append("Если вопрос пользователя связан с изображением, попроси его описать, что изображено на картинке.\n\n")
            }

            // Add user message
            promptBuilder.append("Вот мой вопрос: ")
                .append(userMessage)

            return promptBuilder.toString()
        }
    }
}