package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("StabilityPayloadGenerator")
class StabilityPayloadGeneratorTest {

  @Test
  @DisplayName("supports Stability model IDs")
  void supportsStability() {
    StabilityPayloadGenerator g = new StabilityPayloadGenerator();
    assertThat(g.supports("stability.stable-diffusion-xl-v1")).isTrue();
    assertThat(g.supports("stability.stable-diffusion")).isTrue();
    assertThat(g.supports("amazon.titan-text")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces text_prompts with text")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("stability.stable-diffusion-xl-v1");

    StabilityPayloadGenerator g = new StabilityPayloadGenerator();
    String payload = g.generatePayload("A red apple", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.TEXT_PROMPTS);
    assertThat(payload).contains(BedrockConstants.JsonKeys.TEXT);
    assertThat(payload).contains("A red apple");
  }
}
