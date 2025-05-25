# Connection Reset Error Fix Documentation

## Issue Description

The application was experiencing connection reset errors when calling the OpenAI API:

```
2025-05-25T02:57:19.324+05:00 ERROR 22856 --- [AIChat] [nio-8080-exec-9] aichat.core.services.MessageService      : Error calling AI service (attempt 1): I/O error on POST request for "https://api.openai.com/v1/chat/completions": Connection reset
```

This error occurs when a network connection is unexpectedly closed during an HTTP request. It can be caused by:

1. Temporary network disruptions
2. API rate limiting
3. Timeout issues
4. Server-side issues at OpenAI

## Changes Made

### 1. MessageService Improvements

The retry mechanism in `MessageService.kt` has been enhanced with the following improvements:

- Increased the maximum number of retries from 3 to 5 for better resilience
- Increased the initial backoff time from 1 second to 2 seconds
- Added specific detection and logging for connection reset errors
- Added jitter to the backoff algorithm (±20% randomness) to prevent the thundering herd problem
- Improved error logging to provide more context

### 2. OpenAIVisionService Improvements

The `OpenAIVisionService.kt` previously had no retry mechanism. It has been updated with:

- A new retry mechanism with a maximum of 5 retries
- Exponential backoff starting at 2 seconds
- Jitter to prevent the thundering herd problem
- Specific detection and handling for connection reset errors
- Improved error logging

## Technical Details

### Exponential Backoff with Jitter

The improved retry mechanism uses exponential backoff with jitter:

1. **Exponential Backoff**: Each retry waits longer than the previous one, following an exponential pattern:
   - First retry: 2 seconds (base delay)
   - Second retry: ~4 seconds (2 * 2^1)
   - Third retry: ~8 seconds (2 * 2^2)
   - Fourth retry: ~16 seconds (2 * 2^3)
   - Fifth retry: ~32 seconds (2 * 2^4)

2. **Jitter**: A random factor (±20%) is applied to each delay to prevent multiple clients from retrying at exactly the same time, which could cause a "thundering herd" problem.

### Connection Reset Detection

The code now specifically detects connection reset errors by checking for the string "Connection reset" in both the exception message and its cause's message:

```kotlin
val isConnectionReset = e.message?.contains("Connection reset") == true || 
                       e.cause?.message?.contains("Connection reset") == true
```

This allows for more specific logging and potentially different handling for these types of errors.

## Recommendations for Preventing Similar Issues

1. **Network Stability**:
   - Ensure stable network connections for servers running the application
   - Consider using a more reliable network provider if issues persist

2. **API Rate Limiting**:
   - Monitor OpenAI API usage to avoid hitting rate limits
   - Implement rate limiting in the application to prevent excessive requests

3. **Timeout Configuration**:
   - Consider increasing connection and read timeouts for the RestTemplate
   - Add configuration options for these timeouts in application.properties

4. **Circuit Breaker Pattern**:
   - Consider implementing a circuit breaker (e.g., using Resilience4j) to prevent cascading failures
   - This would automatically stop making requests to OpenAI if too many failures occur

5. **Monitoring and Alerting**:
   - Set up monitoring for API call failures
   - Create alerts for repeated connection issues

6. **Fallback Mechanisms**:
   - Implement fallback responses when OpenAI is unavailable
   - Consider caching common responses to reduce API dependency

## Testing

To verify that the fix works:

1. Deploy the updated code to a test environment
2. Monitor logs for any remaining connection reset errors
3. If possible, simulate network issues to test the retry mechanism
4. Verify that the application gracefully handles temporary network disruptions

## Conclusion

The implemented changes should significantly improve the application's resilience to network issues when communicating with the OpenAI API. The combination of more retries, longer backoff times, and jitter should handle most temporary connection issues without requiring manual intervention.