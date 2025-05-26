package aichat.core.models.web

import aichat.core.models.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "web_documents")
data class WebDocument(
    @Id
    val id: String,
    
    @Column(nullable = false)
    val title: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    val template: Template,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    val values: String, // JSON string of field values
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Default constructor required by JPA
    constructor() : this(
        id = "",
        title = "",
        user = User(),
        template = Template(),
        values = "{}"
    )
    
    // Computed property to get the template name
    @Transient
    fun getTemplateName(): String = template.title
}

// DTO for document responses
data class WebDocumentDto(
    val id: String,
    val title: String,
    val templateId: String,
    val templateName: String,
    val values: Map<String, Any>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEntity(document: WebDocument, values: Map<String, Any>): WebDocumentDto {
            return WebDocumentDto(
                id = document.id,
                title = document.title,
                templateId = document.template.id,
                templateName = document.template.title,
                values = values,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt
            )
        }
    }
}