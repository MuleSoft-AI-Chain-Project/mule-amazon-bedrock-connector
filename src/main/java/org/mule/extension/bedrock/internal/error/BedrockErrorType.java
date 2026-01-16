package org.mule.extension.bedrock.internal.error;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

public enum BedrockErrorType implements ErrorTypeDefinition<BedrockErrorType> {

  VALIDATION_ERROR(MuleErrors.VALIDATION), RESOURCE_NOT_FOUND(MuleErrors.CONNECTIVITY), ACCESS_DENIED(
      MuleErrors.SECURITY), THROTTLING_ERROR(
          MuleErrors.CONNECTIVITY), BEDROCK_ERROR, SERVICE_ERROR, CLIENT_ERROR, AWS_SDK_ERROR, AUTHORIZATION_NOT_FOUND, CONNECTIVITY;

  private ErrorTypeDefinition<?> parent;

  BedrockErrorType() {
    this.parent = null;
  }

  BedrockErrorType(ErrorTypeDefinition<?> parent) {
    this.parent = parent;
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parent);
  }
}
