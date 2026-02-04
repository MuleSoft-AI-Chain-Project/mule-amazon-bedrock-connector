package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;

@DisplayName("AmazonNovaPayloadGenerator")
class AmazonNovaPayloadGeneratorTest {

  @Test
  @DisplayName("supports Amazon Nova model IDs")
  void supportsNova() {
    AmazonNovaPayloadGenerator g = new AmazonNovaPayloadGenerator();
    assertThat(g.supports("amazon.nova-lite-v1:0")).isTrue();
    assertThat(g.supports("amazon.nova-micro-v1:0")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces messages and inferenceConfig")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("amazon.nova-lite-v1:0");
    when(params.getTemperature()).thenReturn(0.7f);
    when(params.getMaxTokenCount()).thenReturn(100);
    when(params.getTopP()).thenReturn(null);
    when(params.getTopK()).thenReturn(null);
    AmazonNovaPayloadGenerator g = new AmazonNovaPayloadGenerator();
    String payload = g.generatePayload("Hello", params);
    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains(BedrockConstants.JsonKeys.INFERENCE_CONFIG);
    assertThat(payload).contains("Hello");
  }
}
