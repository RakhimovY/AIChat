package aichat.core.modles

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "email")
    var email: String,

    @Column(name = "password")
    var password: String,

    @Column(name = "registrationDate")
    var registrationDate: String? = LocalDate.now().toString(),

    ) {
    constructor() : this(0, "", "", "") {
    }
}