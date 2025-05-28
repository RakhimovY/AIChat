package aichat.core.services.web

import aichat.core.models.web.Template
import aichat.core.repository.web.WebTemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Qualifier("webTemplateService")
class TemplateService(
    private val webTemplateRepository: WebTemplateRepository
) {
    private val logger = LoggerFactory.getLogger(TemplateService::class.java)

    /**
     * Get all templates
     * @return List of all templates
     */
    @Transactional(readOnly = true)
    fun getAllTemplates(): List<Template> {
        logger.info("Getting all templates")
        return webTemplateRepository.findAll()
    }

    /**
     * Get a template by ID
     * @param id The template ID
     * @return The template or null if not found
     */
    @Transactional(readOnly = true)
    fun getTemplateById(id: String): Template? {
        logger.info("Getting template with ID: $id")
        return webTemplateRepository.findById(id).orElse(null)
    }

    /**
     * Get templates by category
     * @param category The category
     * @return List of templates in the category
     */
    @Transactional(readOnly = true)
    fun getTemplatesByCategory(category: String): List<Template> {
        logger.info("Getting templates in category: $category")
        return webTemplateRepository.findByCategory(category)
    }

    /**
     * Search templates by title or description
     * @param query The search query
     * @return List of templates matching the search query
     */
    @Transactional(readOnly = true)
    fun searchTemplates(query: String): List<Template> {
        logger.info("Searching templates with query: $query")
        return webTemplateRepository.searchByTitleOrDescription(query)
    }

    /**
     * Search templates by category and query
     * @param category The category
     * @param query The search query
     * @return List of templates in the category matching the search query
     */
    @Transactional(readOnly = true)
    fun searchTemplatesByCategoryAndQuery(category: String, query: String): List<Template> {
        logger.info("Searching templates in category: $category with query: $query")
        return webTemplateRepository.findByCategoryAndSearchQuery(category, query)
    }
}
