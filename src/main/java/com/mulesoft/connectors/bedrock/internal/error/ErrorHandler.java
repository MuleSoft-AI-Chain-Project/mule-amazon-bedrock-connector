package com.mulesoft.connectors.bedrock.internal.error;

import com.mulesoft.connectors.bedrock.internal.error.exception.BedrockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrockagent.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockagent.model.BedrockAgentException;
import software.amazon.awssdk.services.bedrockagent.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockagent.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockagent.model.ValidationException;

/**
 * Centralized error handler for Bedrock operations. Implements Strategy pattern for consistent error handling across services.
 */
public final class ErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

  private ErrorHandler() {
    // Utility class - prevent instantiation
  }

  /**
   * Handles SdkClientException and converts it to BedrockException.
   *
   * @param exception the SDK client exception
   * @param modelName the model name for context
   * @return BedrockException with appropriate error type
   */
  public static BedrockException handleSdkClientException(SdkClientException exception, String modelName) {
    logger.error("Client error for model '{}': {}", modelName, exception.getMessage(), exception);
    return new BedrockException(
                                String.format("Failed to invoke model '%s': %s", modelName, exception.getMessage()),
                                BedrockErrorType.CLIENT_ERROR,
                                exception);
  }

  /**
   * Handles ValidationException and converts it to BedrockException.
   *
   * @param exception the validation exception
   * @return BedrockException with validation error type
   */
  public static BedrockException handleValidationException(ValidationException exception) {
    logger.error("Validation error: {}", exception.getMessage(), exception);
    return new BedrockException("Invalid request parameters", BedrockErrorType.VALIDATION_ERROR, exception);
  }

  /**
   * Handles ResourceNotFoundException and converts it to BedrockException.
   *
   * @param exception the resource not found exception
   * @return BedrockException with resource not found error type
   */
  public static BedrockException handleResourceNotFoundException(ResourceNotFoundException exception) {
    logger.error("Resource not found: {}", exception.getMessage(), exception);
    return new BedrockException("Resource not found", BedrockErrorType.RESOURCE_NOT_FOUND, exception);
  }

  /**
   * Handles AccessDeniedException and converts it to BedrockException.
   *
   * @param exception the access denied exception
   * @return BedrockException with access denied error type
   */
  public static BedrockException handleAccessDeniedException(AccessDeniedException exception) {
    logger.error("Access denied: {}", exception.getMessage(), exception);
    return new BedrockException("Access denied", BedrockErrorType.ACCESS_DENIED, exception);
  }

  /**
   * Handles ThrottlingException and converts it to BedrockException.
   *
   * @param exception the throttling exception
   * @return BedrockException with throttling error type
   */
  public static BedrockException handleThrottlingException(ThrottlingException exception) {
    logger.error("Request throttled: {}", exception.getMessage(), exception);
    return new BedrockException("Request throttled", BedrockErrorType.THROTTLING_ERROR, exception);
  }

  /**
   * Handles BedrockAgentException and converts it to BedrockException.
   *
   * @param exception the bedrock agent exception
   * @return BedrockException with service error type
   */
  public static BedrockException handleBedrockAgentException(BedrockAgentException exception) {
    logger.error("Bedrock service error: {}", exception.getMessage());
    logger.error("Error code: {}", exception.awsErrorDetails().errorCode());
    logger.error("Status code: {}", exception.statusCode());
    return new BedrockException("Bedrock service error", BedrockErrorType.SERVICE_ERROR, exception);
  }

  /**
   * Handles SdkServiceException and converts it to BedrockException.
   *
   * @param exception the SDK service exception
   * @return BedrockException with service error type
   */
  public static BedrockException handleSdkServiceException(SdkServiceException exception) {
    logger.error("Service error: {} - {}", exception.statusCode(), exception.getMessage());
    logger.error("Request ID: {}", exception.requestId());
    return new BedrockException(BedrockErrorType.SERVICE_ERROR, exception);
  }

  /**
   * Handles generic SdkException and converts it to BedrockException.
   *
   * @param exception the SDK exception
   * @return BedrockException with AWS SDK error type
   */
  public static BedrockException handleSdkException(SdkException exception) {
    logger.error("AWS SDK error: {}", exception.getMessage(), exception);
    return new BedrockException("AWS SDK error", BedrockErrorType.AWS_SDK_ERROR, exception);
  }

  /**
   * Handles IllegalStateException related to connection pool shutdown.
   *
   * @param exception the illegal state exception
   * @return BedrockException with connectivity error type if related to connection pool
   */
  public static BedrockException handleIllegalStateException(IllegalStateException exception) {
    if (exception.getCause() != null && "Connection pool shut down".equals(exception.getCause().getMessage())) {
      return new BedrockException(exception.getMessage(), BedrockErrorType.CONNECTIVITY, exception);
    }
    // Re-throw if not related to connection pool
    throw exception;
  }

  /**
   * Handles generic RuntimeException for unexpected errors.
   *
   * @param exception the runtime exception
   * @param context additional context message
   * @return the same exception (re-thrown) after logging
   */
  public static RuntimeException handleRuntimeException(RuntimeException exception, String context) {
    logger.error("Unexpected runtime exception in {}: {}", context, exception.getMessage(), exception);
    return exception;
  }
}
