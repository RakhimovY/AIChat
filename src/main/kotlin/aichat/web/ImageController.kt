package aichat.web

import aichat.core.services.ImageService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("api/images")
class ImageController(
    private val imageService: ImageService
) {
    /**
     * Download an image by ID
     * @param id The image ID
     * @return A redirect to a presigned URL for downloading the image
     */
    @GetMapping("/{id}")
    fun downloadImage(@PathVariable id: Long): ResponseEntity<Void> {
        val image = imageService.getImageById(id)
            ?: return ResponseEntity.notFound().build()

        // Generate a presigned URL for downloading the image
        val presignedUrl = imageService.getImageUrl(image)

        // Redirect to the presigned URL
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build()
    }
}