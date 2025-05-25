# GPT-4-Vision Integration for Image Analysis

## Overview

This document describes the integration of OpenAI's GPT-4-Vision model for image analysis in the chat application. The implementation allows the AI to analyze images uploaded by users and provide responses based on the image content.

## Implementation Details

### New Components

1. **OpenAIVisionService**
   - Handles communication with the OpenAI API for image analysis
   - Downloads images from MinIO storage
   - Converts images to Base64 encoding for API requests
   - Formats requests according to the GPT-4-Vision API requirements
   - Processes responses from the API

2. **WebConfig**
   - Provides RestTemplate and ObjectMapper beans for dependency injection
   - Configures ObjectMapper to ignore unknown properties during deserialization

### Modified Components

1. **MessageService**
   - Added a helper method `buildSystemPrompt` to create system prompts with country and language instructions
   - Modified the `askAI` method to check for attached images
   - Integrated OpenAIVisionService for image analysis
   - Implemented fallback to standard text-based AI if vision analysis fails
   - Maintained backward compatibility with existing code

## How It Works

1. When a user sends a message with an image:
   - The image is saved to MinIO storage
   - The message is saved to the database with a reference to the image

2. When processing the message:
   - The system checks if an image is attached
   - If an image is found, it uses GPT-4-Vision to analyze it
   - The image is downloaded from MinIO and converted to Base64
   - A request is sent to the OpenAI API with the image and user's prompt
   - The response is saved and returned to the user

3. Error handling:
   - If the vision analysis fails, the system falls back to standard text-based AI
   - The standard AI will inform the user that it cannot see the image but can help based on the user's description

## Configuration

The application is configured to use GPT-4-Vision through the following properties in `application.properties`:

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4-vision-preview
```

## Testing

To test the image analysis functionality:

1. Start the application
2. Upload an image in the chat interface
3. Ask a question about the image
4. Verify that the AI responds with an analysis of the image content

### Test Cases

1. **Basic Image Analysis**
   - Upload a simple image (e.g., a photo of a cat)
   - Ask "What's in this image?"
   - The AI should describe the image content

2. **Complex Image Analysis**
   - Upload an image with text or complex elements
   - Ask about specific details in the image
   - The AI should be able to read text and identify specific elements

3. **Error Handling**
   - Upload a corrupted image or very large image
   - Verify that the system falls back to standard text-based AI
   - The AI should inform the user that it cannot analyze the image

4. **Different Image Formats**
   - Test with different image formats (JPEG, PNG, WebP)
   - All supported formats should work correctly

## Limitations

1. The maximum image size is 50MB
2. Only JPEG, PNG, and WebP formats are supported
3. The OpenAI API may have rate limits that affect the number of image analyses that can be performed
4. Very complex or low-quality images may not be analyzed accurately

## Future Improvements

1. Add image compression to reduce API costs and improve performance
2. Implement caching for image analysis results
3. Add support for multiple images in a single message
4. Improve error handling and user feedback for failed analyses
5. Add metrics and monitoring for image analysis performance