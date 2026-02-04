package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AwsBedrockRegionNameProvider")
class AwsBedrockRegionNameProviderTest {

  @Test
  @DisplayName("resolve returns non-empty set of region names")
  void resolve() throws Exception {
    AwsBedrockRegionNameProvider provider = new AwsBedrockRegionNameProvider();
    assertThat(provider.resolve()).isNotEmpty();
    assertThat(provider.resolve().toString()).contains("us-east-1");
  }
}
