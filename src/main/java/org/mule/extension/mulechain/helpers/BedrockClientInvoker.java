package org.mule.extension.mulechain.helpers;

import org.mule.extension.mulechain.internal.BedrockError;
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
 * Provides error handling for Bedrock calls
 */
public class BedrockClientInvoker {

  private static final Logger logger = LoggerFactory.getLogger(BedrockClientInvoker.class);

  // For operations that return a value
  public static <T> T executeWithErrorHandling(java.util.function.Supplier<T> operation) {
    try {
      return operation.get();
    } catch (ValidationException e) {
      logger.error("Validation error: {}", e.getMessage());
      throw new ModuleException("Invalid request parameters", BedrockError.VALIDATION_ERROR, e);

    } catch (ResourceNotFoundException e) {
      logger.error("Resource not found: {}", e.getMessage());
      throw new ModuleException("Resource not found", BedrockError.RESOURCE_NOT_FOUND, e);

    } catch (AccessDeniedException e) {
      logger.error("Access denied: {}", e.getMessage());
      throw new ModuleException("Access denied", BedrockError.ACCESS_DENIED, e);

    } catch (ThrottlingException e) {
      logger.error("Throttled: {}", e.getMessage());
      throw new ModuleException("Request throttled", BedrockError.THROTTLING_ERROR, e);

    } catch (BedrockAgentException e) {
      logger.error("Bedrock service error: {}", e.getMessage());
      logger.error("Error code: {}", e.awsErrorDetails().errorCode());
      logger.error("Status code: {}", e.statusCode());
      throw new ModuleException("Bedrock service error", BedrockError.SERVICE_ERROR, e);

    } catch (SdkServiceException e) {
      logger.error("Service error: {} - {}", e.statusCode(), e.getMessage());
      logger.error("Request ID: {}", e.requestId());
      throw new ModuleException(BedrockError.SERVICE_ERROR, e);

    } catch (SdkClientException e) {
      logger.error("Client error: {}", e.getMessage());
      throw new ModuleException("Client error", BedrockError.CLIENT_ERROR, e);

    } catch (SdkException e) {
      logger.error("AWS SDK error: {}", e.getMessage());
      throw new ModuleException("AWS SDK error", BedrockError.AWS_SDK_ERROR, e);

    } catch (RuntimeException e) {
      // Log for observability but don't swallow
      logger.error("Unexpected runtime exception in Bedrock operation", e);
      throw e; // Re-throw - don't mask the error
    }
  }

  // For operations that don't return a value
  public static void executeWithErrorHandling(Runnable operation) {
    executeWithErrorHandling(() -> {
      operation.run();
      return null;
    });
  }
}
