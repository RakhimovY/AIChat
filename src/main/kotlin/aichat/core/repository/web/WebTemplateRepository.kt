package aichat.core.repository.web

import aichat.core.models.web.Template
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WebTemplateRepository : JpaRepository<Template, String> {
    /**
     * Find templates by category
     * @param category The category to search for
     * @return List of templates in the specified category
     */
    fun findByCategory(category: String): List<Template>

    /**
     * Search templates by title or description
     * @param query The search query
     * @return List of templates matching the search query
     */
    @Query("SELECT t FROM Template t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun searchByTitleOrDescription(@Param("query") query: String): List<Template>

    /**
     * Find templates by category and search query
     * @param category The category to filter by
     * @param query The search query
     * @return List of templates in the specified category matching the search query
     */
    @Query("SELECT t FROM Template t WHERE t.category = :category AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    fun findByCategoryAndSearchQuery(
        @Param("category") category: String,
        @Param("query") query: String
    ): List<Template>
}