package aichat.core.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val size: Long,

    @Column(nullable = false)
    val objectName: String,

    @Column(nullable = false)
    val bucketName: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_reference_id", nullable = false)
    val chat: Chat
) {
    constructor() : this(
        id = 0,
        name = "",
        contentType = "",
        size = 0,
        objectName = "",
        bucketName = "",
        createdAt = LocalDateTime.now(),
        chat = Chat()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (id != other.id) return false
        if (name != other.name) return false
        if (contentType != other.contentType) return false
        if (size != other.size) return false
        if (objectName != other.objectName) return false
        if (bucketName != other.bucketName) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + objectName.hashCode()
        result = 31 * result + bucketName.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
