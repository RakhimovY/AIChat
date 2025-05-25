# Image Upload Functionality Documentation

## Overview

The image upload functionality allows users to upload images to the chat and receive AI-generated responses based on the content of the images. This document provides information on how to use this feature, its API endpoints, and integration points.

## User Guide

### Uploading an Image

1. In the chat interface, locate the image upload button (camera icon) at the bottom of the message input area.
2. Click the button to open the file selection dialog.
3. Select an image file (supported formats: JPG, PNG, WebP).
4. The selected image will appear in a preview below the input area.
5. Type your message related to the image.
6. Click the send button or press Enter to send the message with the image.

### Viewing Images

1. Images in the chat will appear as thumbnails.
2. Click on an image to view it in full-screen mode.
3. In full-screen mode, you can:
   - Click the close button (X) to return to the chat.
   - Click outside the image to return to the chat.

### Limitations

- Maximum image size: 50MB
- Supported formats: JPG, PNG, WebP
- Only one attachment (either document or image) can be included per message

## Developer Guide

### API Endpoints

#### Upload Image with Message

```
POST /api/chat/image
Content-Type: multipart/form-data
Authorization: Bearer <token>

Form fields:
- content: The message text
- image: The image file
- chatId: (optional) The ID of the existing chat
- country: (optional) The country code for legal context
- language: (optional) The language code for response language
```

#### Get Image

```
GET /api/images/{id}
Authorization: Bearer <token>
```

This endpoint returns a redirect to a presigned URL for downloading the image.

### Data Models

#### Image Model

```kotlin
data class Image(
    var id: Long = 0,
    val name: String,
    val contentType: String,
    val size: Long,
    val objectName: String,
    val bucketName: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val chat: Chat
)
```

#### Message with Image

```kotlin
data class ChatRespDTO(
    val id: Long,
    val role: ChatMessageRole,
    val content: String,
    val createdAt: LocalDateTime,
    val chatId: Long,
    val documentId: Long? = null,
    val documentName: String? = null,
    val documentUrl: String? = null,
    val imageId: Long? = null,
    val imageName: String? = null,
    val imageUrl: String? = null,
    val imageAlt: String? = null
)
```

### Integration Points

#### Frontend Components

- **ImageUploadButton**: Button component for selecting images
- **ImagePreview**: Component for displaying a preview of the selected image
- **ImageMessage**: Component for displaying images in the chat

#### Backend Services

- **ImageService**: Service for handling image uploads, storage, and retrieval
- **MinioService**: Service for interacting with MinIO object storage
- **MessageService**: Service for creating and retrieving messages with images

### Error Handling

The API returns the following error codes:

- **400 Bad Request**: Invalid request parameters or unsupported image type
- **401 Unauthorized**: Missing or invalid authentication token
- **413 Payload Too Large**: Image size exceeds the maximum allowed size (50MB)
- **500 Internal Server Error**: Server-side error

### Security Considerations

- All API endpoints require authentication
- Images are stored in MinIO with unique object names
- Access to images is controlled through presigned URLs with expiration times
- Input validation is performed on all user inputs

## Troubleshooting

### Common Issues

1. **Image upload fails with 400 error**
   - Ensure the image format is supported (JPG, PNG, WebP)
   - Check that the image is not corrupted

2. **Image upload fails with 413 error**
   - Reduce the image size to be under 50MB

3. **Image not displaying in chat**
   - Check network connectivity
   - Verify that the image URL is correct
   - Ensure the authentication token is valid

### Logging

The application logs image-related operations with the following loggers:

- `aichat.core.services.ImageService`: Logs image upload and retrieval operations
- `aichat.core.services.MinioService`: Logs MinIO storage operations
- `aichat.web.ImageController`: Logs image API requests

## Future Enhancements

Planned enhancements for the image upload functionality:

1. Image compression to reduce storage requirements
2. Image analysis using AI vision models
3. Support for additional image formats (GIF, SVG)
4. Image editing capabilities (crop, resize, etc.)
5. Bulk image upload