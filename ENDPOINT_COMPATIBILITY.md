# Endpoint Compatibility for Document and Image Uploads

## Issue Description
There was a discrepancy between the frontend and backend implementations for document and image uploads. The frontend was sending requests to `/chat/document` and `/chat/image`, but the backend was expecting requests at `/ask-with-document` and `/ask-with-image`.

## Solution
To resolve this issue, two new endpoints were added to the backend that match what the frontend expects, while keeping the existing endpoints for backward compatibility:

1. `/document` for document uploads, which reuses the existing implementation
2. `/image` for image uploads, which reuses the existing implementation

## Changes Made
The following changes were made to the `ChatController.kt` file:

```kotlin
// Additional endpoint to match frontend expectations for document uploads
@PostMapping("/document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun chatWithDocumentAlternative(
    @RequestParam("content") content: String,
    @RequestParam("document") document: MultipartFile,
    @RequestParam("chatId", required = false) chatId: Long?,
    @RequestParam("country", required = false) country: String?,
    @RequestParam("language", required = false) language: String?,
    principal: Principal
): List<ChatRespDTO> {
    // Reuse the existing implementation
    return chatWithDocument(content, document, chatId, country, language, principal)
}

// Additional endpoint to match frontend expectations for image uploads
@PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun chatWithImageAlternative(
    @RequestParam("content") content: String,
    @RequestParam("image") image: MultipartFile,
    @RequestParam("chatId", required = false) chatId: Long?,
    @RequestParam("country", required = false) country: String?,
    @RequestParam("language", required = false) language: String?,
    principal: Principal
): List<ChatRespDTO> {
    // Reuse the existing implementation
    return chatWithImage(content, image, chatId, country, language, principal)
}
```

## Testing Instructions
To verify that both endpoints work correctly, follow these steps:

1. Start the backend server
2. Start the frontend server
3. Log in to the application
4. Test document uploads:
   - Upload a document in the chat
   - Verify that the document is uploaded successfully
   - Verify that the document is displayed in the chat
5. Test image uploads:
   - Upload an image in the chat
   - Verify that the image is uploaded successfully
   - Verify that the image is displayed in the chat

## Backward Compatibility
The existing endpoints (`/ask-with-document` and `/ask-with-image`) are kept for backward compatibility. If there are any other clients or services that use these endpoints, they will continue to work as before.

## Future Considerations
In the future, it would be beneficial to standardize the API endpoints to avoid such discrepancies. Consider the following:

1. Use consistent naming conventions for endpoints
2. Document all API endpoints and their expected parameters
3. Implement API versioning to handle changes in the API
4. Use API contracts or specifications (e.g., OpenAPI) to ensure consistency between frontend and backend