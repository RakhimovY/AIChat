package aichat.core.services

import org.springframework.stereotype.Service

/**
 * Service for handling country-related operations
 */
@Service
class CountryService {
    // Cache for country name mapping to avoid repeated string operations
    private val countryNameCache = mutableMapOf<String, String>()

    // Map of country codes to country names
    private val countryMap = mapOf(
        "RU" to "Россия",
        "KZ" to "Казахстан",
        "BY" to "Беларусь",
        "UA" to "Украина",
        "UZ" to "Узбекистан",
        "KG" to "Кыргызстан",
        "TJ" to "Таджикистан",
        "TM" to "Туркменистан",
        "AZ" to "Азербайджан",
        "AM" to "Армения",
        "GE" to "Грузия",
        "MD" to "Молдова",
        "US" to "США",
        "GB" to "Великобритания",
        "DE" to "Германия",
        "FR" to "Франция",
        "IT" to "Италия",
        "ES" to "Испания",
        "CN" to "Китай",
        "JP" to "Япония",
        "IN" to "Индия",
        "BR" to "Бразилия",
        "CA" to "Канада",
        "AU" to "Австралия"
    )

    /**
     * Get country name by ISO country code with caching
     */
    fun getCountryNameByCode(code: String): String {
        // Check if the country name is already in the cache
        return countryNameCache.getOrPut(code) {
            countryMap[code] ?: code // Return the code itself if not found in the mapping
        }
    }
}