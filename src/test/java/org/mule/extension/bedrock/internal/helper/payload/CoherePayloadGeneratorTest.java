package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.BedrockConstants;

@DisplayName("CoherePayloadGenerator")
class CoherePayloadGeneratorTest {

  @Test
  @DisplayName("supports Cohere Command model IDs")
  void supportsCohere() {
    CoherePayloadGenerator g = new CoherePayloadGenerator();
    assertThat(g.supports("cohere.command-r")).isTrue();
    assertThat(g.supports("cohere.command-r-v1")).isTrue();
    assertThat(g.supports("meta.llama3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces messages with p and temperature")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("cohere.command-r");
    when(params.getTemperature()).thenReturn(0.6f);
    when(params.getMaxTokenCount()).thenReturn(300);
    when(params.getTopP()).thenReturn(0.95f);
    when(params.getTopK()).thenReturn(40);

    CoherePayloadGenerator g = new CoherePayloadGenerator();
    String payload = g.generatePayload("Hello Cohere", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains(BedrockConstants.JsonKeys.ROLE);
    assertThat(payload).contains(BedrockConstants.JsonKeys.CONTENT);
    assertThat(payload).contains("Hello Cohere");
    assertThat(payload).contains(BedrockConstants.JsonKeys.TEMPERATURE);
    assertThat(payload).contains(BedrockConstants.JsonKeys.P);
    assertThat(payload).contains(BedrockConstants.JsonKeys.K);
    assertThat(payload).contains("max_tokens");
  }
}
