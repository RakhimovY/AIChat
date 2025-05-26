package aichat.core.repository.web

import aichat.core.models.User
import aichat.core.models.web.WebDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WebDocumentRepository : JpaRepository<WebDocument, String> {
    /**
     * Find all documents for a user
     * @param user The user
     * @return List of documents for the user
     */
    fun findByUser(user: User): List<WebDocument>
    
    /**
     * Find all documents for a user ID
     * @param userId The user ID
     * @return List of documents for the user
     */
    fun findByUserId(userId: Long): List<WebDocument>
    
    /**
     * Find a document by ID and user
     * @param id The document ID
     * @param user The user
     * @return The document or null if not found
     */
    fun findByIdAndUser(id: String, user: User): Optional<WebDocument>
    
    /**
     * Find a document by ID and user ID
     * @param id The document ID
     * @param userId The user ID
     * @return The document or null if not found
     */
    fun findByIdAndUserId(id: String, userId: Long): Optional<WebDocument>
    
    /**
     * Search documents by title for a user
     * @param query The search query
     * @param user The user
     * @return List of documents matching the search query for the user
     */
    @Query("SELECT d FROM WebDocument d WHERE d.user = :user AND LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByTitleForUser(
        @Param("query") query: String,
        @Param("user") user: User
    ): List<WebDocument>
    
    /**
     * Search documents by title for a user ID
     * @param query The search query
     * @param userId The user ID
     * @return List of documents matching the search query for the user
     */
    @Query("SELECT d FROM WebDocument d WHERE d.user.id = :userId AND LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByTitleForUserId(
        @Param("query") query: String,
        @Param("userId") userId: Long
    ): List<WebDocument>
}