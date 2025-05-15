# Environment Variables Setup

This document explains how to set up environment variables for the LegalGPT application to keep sensitive information secure.

## Why Use Environment Variables?

Environment variables allow you to:
- Keep sensitive information out of your codebase
- Configure the application differently in different environments (development, testing, production)
- Avoid committing secrets to version control

## Required Environment Variables

The application uses the following environment variables:

### Spring Security Configuration
- `SPRING_SECURITY_USER`: Username for Spring Security
- `SPRING_SECURITY_PASSWORD`: Password for Spring Security

### Database Configuration
- `DB_URL`: JDBC URL for your PostgreSQL database
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

### JWT Configuration
- `JWT_SECRET`: Secret key used to sign JWT tokens
- `JWT_ACCESS_TOKEN_EXPIRATION`: Expiration time for access tokens in milliseconds
- `JWT_REFRESH_TOKEN_EXPIRATION`: Expiration time for refresh tokens in milliseconds

### OpenAI Configuration
- `OPENAI_API_KEY`: Your OpenAI API key

### Email Configuration
- `EMAIL_USERNAME`: Email address used for sending emails
- `EMAIL_APP_PASSWORD`: App password for the email account

### Application Configuration
- `FRONTEND_URL`: URL of the frontend application

## Setting Up Environment Variables

### Using a .env File (Development)

1. Create a `.env` file in the root directory of the project
2. Copy the contents of `.env.example` to your `.env` file
3. Replace the placeholder values with your actual values

Example:
```
# Spring Security Configuration
SPRING_SECURITY_USER=admin
SPRING_SECURITY_PASSWORD=secure_admin_password

# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/my_database
DB_USERNAME=postgres
DB_PASSWORD=my_secure_password

# JWT Configuration
JWT_SECRET=my_very_secure_jwt_secret_key
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=86400000

# OpenAI Configuration
OPENAI_API_KEY=sk-...

# Email Configuration
EMAIL_USERNAME=my.email@gmail.com
EMAIL_APP_PASSWORD=abcdefghijklmnop

# Application Configuration
FRONTEND_URL=http://localhost:3000
```

### Using System Environment Variables (Production)

For production environments, it's recommended to set environment variables at the system level:

#### Linux/macOS
```bash
export SPRING_SECURITY_USER=admin
export SPRING_SECURITY_PASSWORD=secure_admin_password
export DB_URL=jdbc:postgresql://localhost:5432/my_database
export DB_USERNAME=postgres
export DB_PASSWORD=my_secure_password
# ... and so on for other variables
```

#### Windows (PowerShell)
```powershell
$env:SPRING_SECURITY_USER = "admin"
$env:SPRING_SECURITY_PASSWORD = "secure_admin_password"
$env:DB_URL = "jdbc:postgresql://localhost:5432/my_database"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "my_secure_password"
# ... and so on for other variables
```

## Default Values

The application provides default values for most environment variables to make development easier. However, for security-sensitive variables like `SPRING_SECURITY_PASSWORD`, `JWT_SECRET`, `DB_PASSWORD`, and `EMAIL_APP_PASSWORD`, you should always provide your own values in production.

## Security Considerations

- Never commit your `.env` file to version control
- Use strong, unique values for secrets
- Rotate secrets regularly
- In production, use a secure method to manage environment variables (e.g., Kubernetes Secrets, AWS Parameter Store)
