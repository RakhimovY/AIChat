package aichat.web.controllers

import aichat.core.models.web.TemplateDto
import aichat.core.services.web.TemplateService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/web/templates")
class WebTemplateController(
    private val templateService: TemplateService
) {
    private val logger = LoggerFactory.getLogger(WebTemplateController::class.java)

    /**
     * Get all templates
     * @param category Optional category filter
     * @param query Optional search query
     * @return List of templates
     */
    @GetMapping
    fun getAllTemplates(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) query: String?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<TemplateDto>> {
        logger.info("Getting all templates for user: ${userDetails.username}, category: $category, query: $query")
        
        val templates = when {
            category != null && query != null -> templateService.searchTemplatesByCategoryAndQuery(category, query)
            category != null -> templateService.getTemplatesByCategory(category)
            query != null -> templateService.searchTemplates(query)
            else -> templateService.getAllTemplates()
        }
        
        return ResponseEntity.ok(templates.map { TemplateDto.fromEntity(it) })
    }

    /**
     * Get a template by ID
     * @param id The template ID
     * @return The template or 404 if not found
     */
    @GetMapping("/{id}")
    fun getTemplateById(
        @PathVariable id: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<TemplateDto> {
        logger.info("Getting template with ID: $id for user: ${userDetails.username}")
        
        val template = templateService.getTemplateById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(TemplateDto.fromEntity(template))
    }
}