package aichat.core.models

import aichat.core.enums.ChatMessageRole
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
data class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val role: ChatMessageRole,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat
) {
    constructor() : this(
        id = 0,
        role = ChatMessageRole.user,
        content = "",
        createdAt = LocalDateTime.now(),
        chat = Chat()
    )
}
