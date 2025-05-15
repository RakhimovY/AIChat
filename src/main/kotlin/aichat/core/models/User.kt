package aichat.core.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "name", nullable = true)
    var name: String? = null,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "google_id", nullable = true, unique = true)
    var googleId: String? = null,

    @Column(name = "picture", nullable = true)
    var picture: String? = null,

    @Column(name = "provider", nullable = true)
    var provider: String? = "credentials",

    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "client", cascade = [CascadeType.ALL], orphanRemoval = true)
    val chats: MutableList<Chat> = mutableListOf()
) {
    constructor() : this(
        id = 0,
        email = "",
        name = null,
        passwordHash = "",
        googleId = null,
        picture = null,
        provider = "credentials",
        createdAt = LocalDateTime.now(),
        chats = mutableListOf()
    )
}
