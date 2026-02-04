package org.mule.extension.bedrock.internal.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

@DisplayName("BedrockErrorType")
class BedrockErrorTypeTest {

  @Test
  @DisplayName("VALIDATION_ERROR has parent MuleErrors.VALIDATION")
  void validationErrorHasParent() {
    Optional<ErrorTypeDefinition<? extends Enum<?>>> parent = BedrockErrorType.VALIDATION_ERROR.getParent();
    assertThat(parent).isPresent();
  }

  @Test
  @DisplayName("CONNECTIVITY has no parent")
  void connectivityNoParent() {
    assertThat(BedrockErrorType.CONNECTIVITY.getParent()).isEmpty();
  }

  @Test
  @DisplayName("ACCESS_DENIED has security parent")
  void accessDeniedHasParent() {
    assertThat(BedrockErrorType.ACCESS_DENIED.getParent()).isPresent();
  }

  @Test
  @DisplayName("all enum values are defined")
  void allValuesDefined() {
    BedrockErrorType[] values = BedrockErrorType.values();
    assertThat(values).contains(
                                BedrockErrorType.VALIDATION_ERROR,
                                BedrockErrorType.RESOURCE_NOT_FOUND,
                                BedrockErrorType.ACCESS_DENIED,
                                BedrockErrorType.THROTTLING_ERROR,
                                BedrockErrorType.BEDROCK_ERROR,
                                BedrockErrorType.SERVICE_ERROR,
                                BedrockErrorType.CLIENT_ERROR,
                                BedrockErrorType.AWS_SDK_ERROR,
                                BedrockErrorType.CONNECTIVITY);
  }
}
