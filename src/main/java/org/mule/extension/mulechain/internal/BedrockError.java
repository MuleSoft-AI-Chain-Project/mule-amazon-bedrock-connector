package org.mule.extension.mulechain.internal;

import java.util.Optional;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

public enum BedrockError implements ErrorTypeDefinition<BedrockError> {

  VALIDATION_ERROR(MuleErrors.VALIDATION), RESOURCE_NOT_FOUND(MuleErrors.CONNECTIVITY), ACCESS_DENIED(
      MuleErrors.SECURITY), THROTTLING_ERROR(MuleErrors.CONNECTIVITY), BEDROCK_ERROR, SERVICE_ERROR, CLIENT_ERROR, AWS_SDK_ERROR;

  private final ErrorTypeDefinition<?> parent;

  BedrockError() {
    this.parent = null;
  }

  BedrockError(ErrorTypeDefinition<?> parent) {
    this.parent = parent;
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return Optional.ofNullable(parent);
  }
}
