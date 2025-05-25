# AI Chat Backend

## Recent Updates

### Unified Chat Endpoint

The chat API has been unified to use a single endpoint for all types of messages:

- `/api/chat/ask` now handles text messages, document uploads, and image uploads
- The `/api/chat/document` and `/api/chat/image` endpoints have been removed

This simplifies the API and makes it more consistent. All client code should be updated to use the unified endpoint.

## API Documentation

### Unified Chat Endpoint

**Endpoint:** `POST /api/chat/ask`

**Content Types:**
- `application/json` - For text messages
- `multipart/form-data` - For messages with document or image uploads

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| content | String | Yes | The message content |
| chatId | Long | No | The ID of the chat (if continuing an existing chat) |
| country | String | No | The country code for localization |
| language | String | No | The language code for localization |
| document | File | No | A document file to upload |
| image | File | No | An image file to upload |

**Response:**
```json
[
  {
    "id": 123,
    "role": "user",
    "content": "Your message",
    "createdAt": "2023-05-25T12:34:56Z",
    "chatId": 456,
    "documentUrl": "http://example.com/api/documents/789",
    "documentName": "document.pdf",
    "imageUrl": null,
    "imageAlt": null
  },
  {
    "id": 124,
    "role": "assistant",
    "content": "AI response",
    "createdAt": "2023-05-25T12:35:00Z",
    "chatId": 456,
    "documentUrl": null,
    "documentName": null,
    "imageUrl": null,
    "imageAlt": null
  }
]
```

## Implementation Notes

The unified endpoint uses a single controller method that can handle both JSON and form data requests. It validates the input based on the content type and constructs a MessageDTO object that is passed to the message service.

The message service processes the message and generates a response using the appropriate AI model based on the message type (text, document, or image).
