package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.internal.error.provider.BedrockErrorsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;

@DisplayName("BedrockErrorsProvider")
class BedrockErrorsProviderTest {

  @Test
  @DisplayName("getErrorTypes returns all BedrockErrorType values")
  void getErrorTypes() {
    BedrockErrorsProvider provider = new BedrockErrorsProvider();
    assertThat(provider.getErrorTypes()).isNotEmpty();
    assertThat(provider.getErrorTypes()).contains(BedrockErrorType.VALIDATION_ERROR);
    assertThat(provider.getErrorTypes()).contains(BedrockErrorType.SERVICE_ERROR);
  }
}
