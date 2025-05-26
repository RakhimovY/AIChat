package aichat.core.models.web

import java.time.LocalDateTime

/**
 * Data Transfer Object for Template entity
 * Used for transferring template data to the frontend
 */
data class TemplateDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val previewImage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val fields: List<TemplateFieldDto>
) {
    companion object {
        fun fromEntity(template: Template): TemplateDto {
            return TemplateDto(
                id = template.id,
                title = template.title,
                description = template.description,
                category = template.category,
                previewImage = template.previewImage,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt,
                fields = template.fields.map { TemplateFieldDto.fromEntity(it) }
            )
        }
    }
}

/**
 * Data Transfer Object for TemplateField entity
 */
data class TemplateFieldDto(
    val id: Long,
    val fieldId: String,
    val name: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val description: String?,
    val placeholder: String?,
    val defaultValue: String?,
    val options: List<String>?
) {
    companion object {
        fun fromEntity(field: TemplateField): TemplateFieldDto {
            // Parse options string to list if not null
            val optionsList = field.options?.let {
                try {
                    it.split(",").map { option -> option.trim() }
                } catch (e: Exception) {
                    null
                }
            }
            
            return TemplateFieldDto(
                id = field.id,
                fieldId = field.fieldId,
                name = field.name,
                label = field.label,
                type = field.type,
                required = field.required,
                description = field.description,
                placeholder = field.placeholder,
                defaultValue = field.defaultValue,
                options = optionsList
            )
        }
    }
}