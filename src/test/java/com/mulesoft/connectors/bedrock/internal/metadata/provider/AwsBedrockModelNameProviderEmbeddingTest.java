package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.api.provider.AwsBedrockModelNameProviderEmbedding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AwsBedrockModelNameProviderEmbedding")
class AwsBedrockModelNameProviderEmbeddingTest {

  @Test
  @DisplayName("resolve returns embedding model names")
  void resolve() throws Exception {
    AwsBedrockModelNameProviderEmbedding provider = new AwsBedrockModelNameProviderEmbedding();
    assertThat(provider.resolve()).isNotEmpty();
    assertThat(provider.resolve().toString()).contains("amazon.titan-embed-text-v1");
  }
}
