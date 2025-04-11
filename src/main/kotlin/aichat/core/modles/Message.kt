package aichat.core.modles

import aichat.core.enums.ChatMessageRole
import jakarta.persistence.*
import java.time.LocalDate

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
    val content: String,

    @Column(name = "created_at")
    val createdAt: String = LocalDate.now().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat", nullable = false)
    val chats: Chat
)