package org.mule.extension.bedrock.internal.metadata.provider;

import java.util.Set;
import org.mule.extension.bedrock.internal.error.BedrockErrorType;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public class BedrockErrorsProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Set.of(BedrockErrorType.VALIDATION_ERROR,
                  BedrockErrorType.RESOURCE_NOT_FOUND,
                  BedrockErrorType.ACCESS_DENIED,
                  BedrockErrorType.THROTTLING_ERROR,
                  BedrockErrorType.BEDROCK_ERROR,
                  BedrockErrorType.SERVICE_ERROR,
                  BedrockErrorType.CLIENT_ERROR,
                  BedrockErrorType.AWS_SDK_ERROR);
  }
}
