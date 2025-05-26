# Document Generation Backend Implementation - Summary

## Overview

This document provides a summary of the backend implementation for the document generation functionality in the AIuris application. The implementation includes models, repositories, services, and controllers for managing templates and documents, as well as functionality for generating and exporting documents in PDF and DOCX formats.

## Components Implemented

### Models

1. **Template**
   - Represents a document template with fields and content
   - Contains placeholders that can be replaced with actual values

2. **WebDocument**
   - Represents a document created from a template
   - Stores the values used to fill in the template
   - Associated with a user and a template

3. **DTOs (Data Transfer Objects)**
   - `TemplateDto`: For transferring template data to the frontend
   - `WebDocumentDto`: For transferring document data to the frontend

### Repositories

1. **TemplateRepository**
   - Manages persistence of templates
   - Provides methods for finding templates by ID, category, and search query

2. **WebDocumentRepository**
   - Manages persistence of documents
   - Provides methods for finding documents by ID and user

### Services

1. **TemplateService**
   - Business logic for template management
   - Methods for getting all templates, getting templates by category, and searching templates

2. **WebDocumentService**
   - Business logic for document management
   - Methods for creating, updating, and deleting documents
   - Document generation and export functionality
   - PDF and DOCX export using Apache PDFBox and Apache POI

### Controllers

1. **WebTemplateController**
   - REST API endpoints for template management
   - GET /api/web/templates - List all templates
   - GET /api/web/templates/{id} - Get a specific template

2. **WebDocumentController**
   - REST API endpoints for document management
   - GET /api/web/documents - List all documents for the current user
   - GET /api/web/documents/{id} - Get a specific document
   - POST /api/web/documents - Create a new document
   - PUT /api/web/documents/{id} - Update an existing document
   - DELETE /api/web/documents/{id} - Delete a document
   - POST /api/web/documents/export - Export a document as PDF or DOCX

## Document Generation and Export

The document generation process works as follows:

1. A template contains content with placeholders in the format `{{fieldName}}`
2. When a document is created, the user provides values for these placeholders
3. The values are stored in the document as a JSON string
4. When the document is viewed or exported, the placeholders are replaced with the actual values

The export functionality supports two formats:

1. **PDF**
   - Uses Apache PDFBox to create a PDF document
   - Converts the document content to PDF format with appropriate formatting

2. **DOCX**
   - Uses Apache POI to create a DOCX document
   - Converts the document content to DOCX format with appropriate formatting

## API Endpoints

### Template Endpoints

#### GET /api/web/templates
- Returns a list of all templates
- Optional query parameters:
  - `category`: Filter templates by category
  - `query`: Search templates by title or description

#### GET /api/web/templates/{id}
- Returns a specific template by ID
- Returns 404 if the template is not found

### Document Endpoints

#### GET /api/web/documents
- Returns a list of all documents for the current user

#### GET /api/web/documents/{id}
- Returns a specific document by ID
- Returns 404 if the document is not found or doesn't belong to the current user

#### POST /api/web/documents
- Creates a new document
- Request body:
  ```json
  {
    "templateId": "string",
    "title": "string (optional)",
    "values": {
      "fieldName1": "value1",
      "fieldName2": "value2"
    }
  }
  ```
- Returns the created document with status 201

#### PUT /api/web/documents/{id}
- Updates an existing document
- Request body:
  ```json
  {
    "title": "string (optional)",
    "values": {
      "fieldName1": "value1",
      "fieldName2": "value2"
    }
  }
  ```
- Returns the updated document
- Returns 404 if the document is not found or doesn't belong to the current user

#### DELETE /api/web/documents/{id}
- Deletes a document
- Returns 204 No Content if successful
- Returns 404 if the document is not found or doesn't belong to the current user

#### POST /api/web/documents/export
- Exports a document as PDF or DOCX
- Request body:
  ```json
  {
    "templateId": "string",
    "values": {
      "fieldName1": "value1",
      "fieldName2": "value2"
    },
    "format": "pdf | docx"
  }
  ```
- Returns the exported document file with appropriate content type and filename

## Authentication and Authorization

All endpoints are protected by Spring Security and require authentication. The controllers use the `@AuthenticationPrincipal` annotation to get the current user, and the services ensure that users can only access their own documents.

## Next Steps

1. **Testing**
   - Write unit tests for services and controllers
   - Write integration tests for the API endpoints
   - Test the document generation and export functionality with various templates and values

2. **Data Initialization**
   - Create a data initialization script to populate the database with sample templates

3. **Performance Optimization**
   - Add caching for templates to improve performance
   - Optimize document generation for large documents

4. **Feature Enhancements**
   - Add support for more export formats (e.g., HTML, plain text)
   - Add support for template versioning
   - Add support for document sharing and collaboration

## Conclusion

The backend implementation for the document generation functionality is now complete. It provides a robust foundation for creating, managing, and exporting documents based on templates. The implementation follows best practices for Spring Boot applications, with a clear separation of concerns between models, repositories, services, and controllers.