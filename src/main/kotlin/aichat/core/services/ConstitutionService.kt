package aichat.core.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Service
class ConstitutionService {
    @Value("classpath:/constitutions/constitution_kz.txt")
    private lateinit var constitutionKzResource: Resource

    fun getConstitution(countryCode: String?): String? {
        return when (countryCode?.lowercase()) {
            "kz", "kazakhstan" -> loadConstitution(constitutionKzResource)
            else -> null // Или можно возвращать конституцию по умолчанию
        }
    }

    private fun loadConstitution(resource: Resource): String? {
        return try {
            val path = Paths.get(resource.uri)
            Files.readString(path, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            println("Ошибка при загрузке конституции: ${e.message}")
            null
        }
    }
}