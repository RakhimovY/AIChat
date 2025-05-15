package aichat.core.services

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    @Value("\${spring.mail.username}")
    private lateinit var fromEmail: String

    @Value("\${app.frontend-url}")
    private lateinit var frontendUrl: String

    /**
     * Send a password reset email
     * @param to The recipient email address
     * @param token The password reset token
     * @return True if the email was sent successfully, false otherwise
     */
    fun sendPasswordResetEmail(to: String, token: String): Boolean {
        val resetUrl = "$frontendUrl/auth/reset-password?token=$token"
        val subject = "Сброс пароля LegalGPT"
        val content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #333;">Сброс пароля</h2>
                <p>Вы получили это письмо, потому что запросили сброс пароля для вашей учетной записи LegalGPT.</p>
                <p>Для сброса пароля, пожалуйста, перейдите по следующей ссылке:</p>
                <p>
                    <a href="$resetUrl" style="display: inline-block; padding: 10px 20px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 4px;">
                        Сбросить пароль
                    </a>
                </p>
                <p>Или скопируйте и вставьте следующую ссылку в ваш браузер:</p>
                <p><a href="$resetUrl">$resetUrl</a></p>
                <p>Эта ссылка действительна в течение 24 часов.</p>
                <p>Если вы не запрашивали сброс пароля, пожалуйста, проигнорируйте это письмо.</p>
                <p>С уважением,<br>Команда LegalGPT</p>
            </div>
        """.trimIndent()

        return sendEmail(to, subject, content)
    }

    /**
     * Send an email
     * @param to The recipient email address
     * @param subject The email subject
     * @param content The email content (HTML)
     * @return True if the email was sent successfully, false otherwise
     */
    private fun sendEmail(to: String, subject: String, content: String): Boolean {
        return try {
            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(content, true) // true indicates HTML content

            mailSender.send(message)
            true
        } catch (e: MessagingException) {
            println("Failed to send email: ${e.message}")
            e.printStackTrace()
            false
        } catch (e: Exception) {
            println("Unexpected error sending email: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}
