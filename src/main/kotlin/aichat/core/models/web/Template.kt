package aichat.core.models.web

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "web_templates")
data class Template(
    @Id
    val id: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 1000)
    val description: String,

    @Column(nullable = false)
    val category: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = true)
    val previewImage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    val fields: MutableList<TemplateField> = mutableListOf()
) {
    // Default constructor required by JPA
    constructor() : this(
        id = "",
        title = "",
        description = "",
        category = "",
        content = "",
        previewImage = null
    )
}

@Entity
@Table(name = "web_template_fields")
data class TemplateField(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val fieldId: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val label: String,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val required: Boolean = false,

    @Column(nullable = true, length = 1000)
    val description: String? = null,

    @Column(nullable = true)
    val placeholder: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val defaultValue: String? = null,

    @Column(nullable = true, columnDefinition = "TEXT")
    val options: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    var template: Template? = null
) {
    // Default constructor required by JPA
    constructor() : this(
        fieldId = "",
        name = "",
        label = "",
        type = "",
        required = false
    )
}
