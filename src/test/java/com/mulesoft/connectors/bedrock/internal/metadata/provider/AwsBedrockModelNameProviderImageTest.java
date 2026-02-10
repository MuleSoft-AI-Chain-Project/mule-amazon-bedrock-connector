package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AwsBedrockModelNameProviderImage")
class AwsBedrockModelNameProviderImageTest {

  @Test
  @DisplayName("resolve returns image model names")
  void resolve() throws Exception {
    AwsBedrockModelNameProviderImage provider = new AwsBedrockModelNameProviderImage();
    assertThat(provider.resolve()).isNotEmpty();
    assertThat(provider.resolve().toString()).contains("stability.stable-diffusion-xl-v1");
  }
}
