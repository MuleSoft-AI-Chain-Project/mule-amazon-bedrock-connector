package org.mule.extension.mulechain.internal;

import java.util.Set;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

public class BedrockErrorsProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Set.of(
                  BedrockError.VALIDATION_ERROR,
                  BedrockError.RESOURCE_NOT_FOUND,
                  BedrockError.ACCESS_DENIED,
                  BedrockError.THROTTLING_ERROR,
                  BedrockError.BEDROCK_ERROR,
                  BedrockError.SERVICE_ERROR,
                  BedrockError.CLIENT_ERROR,
                  BedrockError.AWS_SDK_ERROR);
  }

}
