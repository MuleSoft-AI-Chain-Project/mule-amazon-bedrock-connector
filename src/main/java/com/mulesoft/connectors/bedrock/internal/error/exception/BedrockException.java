package com.mulesoft.connectors.bedrock.internal.error.exception;

import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;
import org.mule.runtime.extension.api.exception.ModuleException;

public class BedrockException extends ModuleException {

  public BedrockException(String message, BedrockErrorType errorType) {
    super(message, errorType);
  }

  public BedrockException(String message, BedrockErrorType errorType, Throwable cause) {
    super(message, errorType, cause);
  }

  public BedrockException(BedrockErrorType errorType, Throwable cause) {
    super(errorType, cause);
  }
}
