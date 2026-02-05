package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("AmazonTitanPayloadGenerator")
class AmazonTitanPayloadGeneratorTest {

  @Test
  @DisplayName("supports Amazon Titan text model IDs")
  void supportsTitan() {
    AmazonTitanPayloadGenerator g = new AmazonTitanPayloadGenerator();
    assertThat(g.supports("amazon.titan-text-express-v1")).isTrue();
    assertThat(g.supports("amazon.titan-text-premier-v1")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces inputText and textGenerationConfig")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("amazon.titan-text-express-v1");
    when(params.getTemperature()).thenReturn(0.7f);
    when(params.getMaxTokenCount()).thenReturn(100);
    when(params.getTopP()).thenReturn(0.9f);
    when(params.getTopK()).thenReturn(40);

    AmazonTitanPayloadGenerator g = new AmazonTitanPayloadGenerator();
    String payload = g.generatePayload("Hello Titan", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.INPUT_TEXT);
    assertThat(payload).contains(BedrockConstants.JsonKeys.TEXT_GENERATION_CONFIG);
    assertThat(payload).contains("Hello Titan");
    assertThat(payload).contains("temperature");
    assertThat(payload).contains("maxtokens");
    assertThat(payload).contains("100");
  }
}
