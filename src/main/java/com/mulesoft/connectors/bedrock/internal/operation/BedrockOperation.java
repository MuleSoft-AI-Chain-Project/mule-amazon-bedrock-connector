package com.mulesoft.connectors.bedrock.internal.operation;

import org.mule.connectors.atlantic.commons.builder.execution.ExecutionBuilder;
import org.mule.connectors.atlantic.commons.builder.lambda.function.BiFunction;
import org.mule.connectors.commons.template.operation.ConnectorOperations;
import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.ErrorHandler;
import org.mule.runtime.extension.api.exception.ModuleException;
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

  public BedrockOperation(BiFunction<BedrockConfiguration, BedrockConnection, SERVICE> serviceConstructor) {
    super(serviceConstructor);
  }

  protected ExecutionBuilder<SERVICE> newExecutionBuilder(BedrockConfiguration config, BedrockConnection connection) {
    return super.newExecutionBuilder(config, connection)
        .withExceptionHandler(ModuleException.class, (e) -> {
          // Already a ModuleException, re-throw as-is
          throw e;
        })
        .withExceptionHandler(SdkClientException.class, (e) -> {
          // Use ErrorHandler - model name not available at operation level, use generic message
          throw ErrorHandler.handleSdkClientException(e, "Unknown");
        })
        .withExceptionHandler(IllegalStateException.class, (e) -> {
          throw ErrorHandler.handleIllegalStateException(e);
        })
        .withExceptionHandler(ValidationException.class, (e) -> {
          throw ErrorHandler.handleValidationException(e);
        })
        .withExceptionHandler(ResourceNotFoundException.class, (e) -> {
          throw ErrorHandler.handleResourceNotFoundException(e);
        })
        .withExceptionHandler(AccessDeniedException.class, (e) -> {
          throw ErrorHandler.handleAccessDeniedException(e);
        })
        .withExceptionHandler(ThrottlingException.class, (e) -> {
          throw ErrorHandler.handleThrottlingException(e);
        })
        .withExceptionHandler(BedrockAgentException.class, (e) -> {
          throw ErrorHandler.handleBedrockAgentException(e);
        })
        .withExceptionHandler(SdkServiceException.class, (e) -> {
          throw ErrorHandler.handleSdkServiceException(e);
        })
        .withExceptionHandler(SdkException.class, (e) -> {
          throw ErrorHandler.handleSdkException(e);
        })
        .withExceptionHandler(RuntimeException.class, (e) -> {
          // Log and re-throw - don't convert to ModuleException
          ErrorHandler.handleRuntimeException(e, "BedrockOperation");
          throw e;
        });
  }
}
