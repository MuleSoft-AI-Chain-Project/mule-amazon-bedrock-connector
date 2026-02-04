package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("AI21JambaPayloadGenerator")
class AI21JambaPayloadGeneratorTest {

  @Test
  @DisplayName("supports AI21 Jamba model IDs")
  void supportsJamba() {
    AI21JambaPayloadGenerator g = new AI21JambaPayloadGenerator();
    assertThat(g.supports("ai21.jamba-1")).isTrue();
    assertThat(g.supports("ai21.jamba-instruct")).isTrue();
    assertThat(g.supports("anthropic.claude-3")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces messages with role and content")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("ai21.jamba-1");
    when(params.getTemperature()).thenReturn(0.5f);
    when(params.getMaxTokenCount()).thenReturn(200);
    when(params.getTopP()).thenReturn(0.95f);
    when(params.getTopK()).thenReturn(null);

    AI21JambaPayloadGenerator g = new AI21JambaPayloadGenerator();
    String payload = g.generatePayload("Hello Jamba", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains(BedrockConstants.JsonKeys.ROLE);
    assertThat(payload).contains(BedrockConstants.JsonKeys.CONTENT);
    assertThat(payload).contains("Hello Jamba");
    assertThat(payload).contains(BedrockConstants.JsonKeys.TEMPERATURE);
    assertThat(payload).contains("maxtokens");
    assertThat(payload).contains("200");
  }
}
