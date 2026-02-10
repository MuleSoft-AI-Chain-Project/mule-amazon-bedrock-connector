package com.mulesoft.connectors.bedrock.internal.error;

import org.mule.runtime.extension.api.exception.ModuleException;
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
   * Handles SdkClientException and converts it to ModuleException.
   *
   * @param exception the SDK client exception
   * @param modelName the model name for context
   * @return ModuleException with appropriate error type
   */
  public static ModuleException handleSdkClientException(SdkClientException exception, String modelName) {
    logger.error("Client error for model '{}': {}", modelName, exception.getMessage(), exception);
    return new ModuleException(
                               String.format("Failed to invoke model '%s': %s", modelName, exception.getMessage()),
                               BedrockErrorType.CLIENT_ERROR,
                               exception);
  }

  /**
   * Handles ValidationException and converts it to ModuleException.
   *
   * @param exception the validation exception
   * @return ModuleException with validation error type
   */
  public static ModuleException handleValidationException(ValidationException exception) {
    logger.error("Validation error: {}", exception.getMessage(), exception);
    return new ModuleException("Invalid request parameters", BedrockErrorType.VALIDATION_ERROR, exception);
  }

  /**
   * Handles ResourceNotFoundException and converts it to ModuleException.
   *
   * @param exception the resource not found exception
   * @return ModuleException with resource not found error type
   */
  public static ModuleException handleResourceNotFoundException(ResourceNotFoundException exception) {
    logger.error("Resource not found: {}", exception.getMessage(), exception);
    return new ModuleException("Resource not found", BedrockErrorType.RESOURCE_NOT_FOUND, exception);
  }

  /**
   * Handles AccessDeniedException and converts it to ModuleException.
   *
   * @param exception the access denied exception
   * @return ModuleException with access denied error type
   */
  public static ModuleException handleAccessDeniedException(AccessDeniedException exception) {
    logger.error("Access denied: {}", exception.getMessage(), exception);
    return new ModuleException("Access denied", BedrockErrorType.ACCESS_DENIED, exception);
  }

  /**
   * Handles ThrottlingException and converts it to ModuleException.
   *
   * @param exception the throttling exception
   * @return ModuleException with throttling error type
   */
  public static ModuleException handleThrottlingException(ThrottlingException exception) {
    logger.error("Request throttled: {}", exception.getMessage(), exception);
    return new ModuleException("Request throttled", BedrockErrorType.THROTTLING_ERROR, exception);
  }

  /**
   * Handles BedrockAgentException and converts it to ModuleException.
   *
   * @param exception the bedrock agent exception
   * @return ModuleException with service error type
   */
  public static ModuleException handleBedrockAgentException(BedrockAgentException exception) {
    logger.error("Bedrock service error: {}", exception.getMessage());
    logger.error("Error code: {}", exception.awsErrorDetails().errorCode());
    logger.error("Status code: {}", exception.statusCode());
    return new ModuleException("Bedrock service error", BedrockErrorType.SERVICE_ERROR, exception);
  }

  /**
   * Handles SdkServiceException and converts it to ModuleException.
   *
   * @param exception the SDK service exception
   * @return ModuleException with service error type
   */
  public static ModuleException handleSdkServiceException(SdkServiceException exception) {
    logger.error("Service error: {} - {}", exception.statusCode(), exception.getMessage());
    logger.error("Request ID: {}", exception.requestId());
    return new ModuleException(BedrockErrorType.SERVICE_ERROR, exception);
  }

  /**
   * Handles generic SdkException and converts it to ModuleException.
   *
   * @param exception the SDK exception
   * @return ModuleException with AWS SDK error type
   */
  public static ModuleException handleSdkException(SdkException exception) {
    logger.error("AWS SDK error: {}", exception.getMessage(), exception);
    return new ModuleException("AWS SDK error", BedrockErrorType.AWS_SDK_ERROR, exception);
  }

  /**
   * Handles IllegalStateException related to connection pool shutdown.
   *
   * @param exception the illegal state exception
   * @return ModuleException with connectivity error type if related to connection pool
   */
  public static ModuleException handleIllegalStateException(IllegalStateException exception) {
    if (exception.getCause() != null && "Connection pool shut down".equals(exception.getCause().getMessage())) {
      return new ModuleException(exception.getMessage(), BedrockErrorType.CONNECTIVITY, exception);
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
