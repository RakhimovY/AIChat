# Test Plan for Image Upload Functionality

## 1. Frontend Tests

### 1.1 Image Upload Button
- Verify that the image upload button is visible in the chat input area
- Verify that the button is disabled when a document is already selected
- Verify that the button is disabled when an image is already selected
- Verify that the button is disabled when the chat is loading

### 1.2 Image Preview
- Verify that the selected image is displayed in the preview area
- Verify that the image name and size are displayed correctly
- Verify that the remove button works correctly
- Verify that the preview is removed when the message is sent

### 1.3 Image Display in Chat
- Verify that uploaded images are displayed correctly in the chat
- Verify that clicking on an image opens it in full-screen mode
- Verify that the close button in full-screen mode works correctly

### 1.4 Error Handling
- Verify that an error message is displayed when trying to upload an unsupported file type
- Verify that an error message is displayed when trying to upload a file larger than 50MB
- Verify that the error message is cleared when selecting a valid file

## 2. Backend Tests

### 2.1 Image Upload API
- Verify that the `/api/chat/image` endpoint accepts image uploads
- Verify that the endpoint returns a 400 error for unsupported file types
- Verify that the endpoint returns a 413 error for files larger than 50MB
- Verify that the endpoint returns a 401 error for unauthenticated requests

### 2.2 Image Storage
- Verify that uploaded images are stored correctly in MinIO
- Verify that image metadata is stored correctly in the database
- Verify that image URLs are generated correctly

### 2.3 Image Retrieval
- Verify that images can be retrieved using the generated URLs
- Verify that the URLs are accessible only to authenticated users
- Verify that the URLs expire after the configured time

### 2.4 AI Integration
- Verify that the AI service receives the image information
- Verify that the AI response includes references to the image if relevant

## 3. Integration Tests

### 3.1 End-to-End Flow
- Verify the complete flow from image upload to display in the chat
- Verify that multiple images can be uploaded in different messages
- Verify that images and documents can be used in the same conversation

### 3.2 Performance
- Verify that image uploads don't significantly impact chat performance
- Verify that large images are handled efficiently
- Verify that the application remains responsive during image uploads

## 4. Security Tests

### 4.1 Authentication and Authorization
- Verify that only authenticated users can upload images
- Verify that users can only access their own images
- Verify that image URLs are properly secured

### 4.2 Input Validation
- Verify that all user inputs are properly validated
- Verify that file content is properly sanitized
- Verify that the application is resistant to common attacks (XSS, CSRF, etc.)

## 5. Compatibility Tests

### 5.1 Browser Compatibility
- Verify that image uploads work in all supported browsers (Chrome, Firefox, Safari, Edge)
- Verify that image display works correctly in all supported browsers
- Verify that the application is responsive on different screen sizes

### 5.2 File Format Compatibility
- Verify that all supported image formats (JPG, PNG, WebP) work correctly
- Verify that images with different color profiles are displayed correctly
- Verify that images with transparency are displayed correctly