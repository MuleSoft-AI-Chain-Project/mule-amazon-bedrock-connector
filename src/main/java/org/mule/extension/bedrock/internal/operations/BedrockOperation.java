package org.mule.extension.bedrock.internal.operations;

import org.mule.connectors.atlantic.commons.builder.execution.ExecutionBuilder;
import org.mule.connectors.atlantic.commons.builder.lambda.function.BiFunction;
import org.mule.connectors.commons.template.operation.ConnectorOperations;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.BedrockErrorType;
import org.mule.extension.bedrock.internal.error.exception.BedrockException;
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

public class BedrockOperation<SERVICE extends ConnectorService>
    extends ConnectorOperations<BedrockConfiguration, BedrockConnection, SERVICE> {

  private static final Logger logger = LoggerFactory.getLogger(BedrockOperation.class);

  public BedrockOperation(BiFunction<BedrockConfiguration, BedrockConnection, SERVICE> serviceConstructor) {
    super(serviceConstructor);
  }

  protected ExecutionBuilder<SERVICE> newExecutionBuilder(BedrockConfiguration config, BedrockConnection connection) {
    return super.newExecutionBuilder(config, connection).withExceptionHandler(BedrockException.class, (e) -> {
      throw e;
    }).withExceptionHandler(SdkClientException.class, (e) -> {
      logger.error("Client error: {}", e.getMessage());
      throw new BedrockException("Client error", BedrockErrorType.CLIENT_ERROR, e);
    }).withExceptionHandler(IllegalStateException.class, (e) -> {
      if ("Connection pool shut down".equals(e.getCause().getMessage())) {
        throw new BedrockException(e.getMessage(), BedrockErrorType.CONNECTIVITY, e);
      } else {
        throw e;
      }
    }).withExceptionHandler(ValidationException.class, (e) -> {
      logger.error("Validation error: {}", e.getMessage());
      throw new BedrockException("Invalid request parameters", BedrockErrorType.VALIDATION_ERROR, e);
    }).withExceptionHandler(ResourceNotFoundException.class, (e) -> {
      logger.error("Resource not found: {}", e.getMessage());
      throw new BedrockException("Resource not found", BedrockErrorType.RESOURCE_NOT_FOUND, e);
    }).withExceptionHandler(AccessDeniedException.class, (e) -> {
      logger.error("Access denied: {}", e.getMessage());
      throw new BedrockException("Access denied", BedrockErrorType.ACCESS_DENIED, e);
    }).withExceptionHandler(ThrottlingException.class, (e) -> {
      logger.error("Throttled: {}", e.getMessage());
      throw new BedrockException("Request throttled", BedrockErrorType.THROTTLING_ERROR, e);
    }).withExceptionHandler(BedrockAgentException.class, (e) -> {
      logger.error("Bedrock service error: {}", e.getMessage());
      logger.error("Error code: {}", e.awsErrorDetails().errorCode());
      logger.error("Status code: {}", e.statusCode());
      throw new BedrockException("Bedrock service error", BedrockErrorType.SERVICE_ERROR, e);
    }).withExceptionHandler(SdkServiceException.class, (e) -> {
      logger.error("Service error: {} - {}", e.statusCode(), e.getMessage());
      logger.error("Request ID: {}", e.requestId());
      throw new BedrockException(BedrockErrorType.SERVICE_ERROR, e);
    }).withExceptionHandler(SdkException.class, (e) -> {
      logger.error("AWS SDK error: {}", e.getMessage());
      throw new BedrockException("AWS SDK error", BedrockErrorType.AWS_SDK_ERROR, e);
    }).withExceptionHandler(RuntimeException.class, (e) -> {
      logger.error("Unexpected runtime exception in Bedrock operation", e);
      throw e;
    });
  }
}
