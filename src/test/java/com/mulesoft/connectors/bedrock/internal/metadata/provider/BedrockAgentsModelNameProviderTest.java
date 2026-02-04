package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockAgentsModelNameProvider")
class BedrockAgentsModelNameProviderTest {

  @Test
  @DisplayName("resolve returns agent model names")
  void resolve() throws Exception {
    BedrockAgentsModelNameProvider provider = new BedrockAgentsModelNameProvider();
    assertThat(provider.resolve()).isNotEmpty();
    assertThat(provider.resolve().toString()).contains("amazon.nova-lite-v1:0");
  }
}
