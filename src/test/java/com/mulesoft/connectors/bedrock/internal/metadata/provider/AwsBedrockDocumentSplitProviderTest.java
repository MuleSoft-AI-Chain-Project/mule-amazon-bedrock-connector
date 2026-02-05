package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.api.provider.AwsBedrockDocumentSplitProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AwsBedrockDocumentSplitProvider")
class AwsBedrockDocumentSplitProviderTest {

  @Test
  @DisplayName("resolve returns FULL PARAGRAPH SENTENCES")
  void resolve() throws Exception {
    AwsBedrockDocumentSplitProvider provider = new AwsBedrockDocumentSplitProvider();
    assertThat(provider.resolve()).hasSize(3);
    assertThat(provider.resolve().toString()).contains("FULL").contains("PARAGRAPH").contains("SENTENCES");
  }
}
