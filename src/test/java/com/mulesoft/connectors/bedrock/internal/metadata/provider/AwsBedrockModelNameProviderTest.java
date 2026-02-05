package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.api.provider.AwsBedrockModelNameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AwsBedrockModelNameProvider")
class AwsBedrockModelNameProviderTest {

  @Test
  @DisplayName("resolve returns non-empty set of model names")
  void resolve() throws Exception {
    AwsBedrockModelNameProvider provider = new AwsBedrockModelNameProvider();
    assertThat(provider.resolve()).isNotEmpty();
    assertThat(provider.resolve().toString()).contains("amazon.nova-lite-v1:0");
  }
}
