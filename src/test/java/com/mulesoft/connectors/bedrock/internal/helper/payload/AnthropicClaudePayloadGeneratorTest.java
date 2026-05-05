package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.BedrockConstants;

@DisplayName("AnthropicClaudePayloadGenerator")
class AnthropicClaudePayloadGeneratorTest {

  @Test
  @DisplayName("supports Anthropic Claude model IDs")
  void supportsClaude() {
    AnthropicClaudePayloadGenerator g = new AnthropicClaudePayloadGenerator();
    assertThat(g.supports("anthropic.claude-3-5-sonnet")).isTrue();
    assertThat(g.supports("anthropic.claude-3-haiku")).isTrue();
    assertThat(g.supports("amazon.nova-lite-v1:0")).isFalse();
  }

  @Test
  @DisplayName("generatePayload produces messages and anthropic_version")
  void generatePayload() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("anthropic.claude-3-5-sonnet");
    when(params.getTemperature()).thenReturn(0.7f);
    when(params.getMaxTokenCount()).thenReturn(256);
    when(params.getTopP()).thenReturn(0.9f);
    when(params.getTopK()).thenReturn(null);

    AnthropicClaudePayloadGenerator g = new AnthropicClaudePayloadGenerator();
    String payload = g.generatePayload("Hello Claude", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains("anthropic_version");
    assertThat(payload).contains(BedrockConstants.JsonKeys.USER);
    assertThat(payload).contains("Hello Claude");
    assertThat(payload).contains("temperature");
    assertThat(payload).contains("max_tokens");
  }

  @Test
  @DisplayName("generatePayload omits temperature/top_p/top_k for Claude Opus 4.7")
  void generatePayloadOpus47() {
    BedrockParameters params = mock(BedrockParameters.class);
    when(params.getModelName()).thenReturn("anthropic.claude-opus-4-7-20260101-v1:0");
    when(params.getTemperature()).thenReturn(0.7f);
    when(params.getMaxTokenCount()).thenReturn(512);
    when(params.getTopP()).thenReturn(0.9f);
    when(params.getTopK()).thenReturn(40);

    AnthropicClaudePayloadGenerator g = new AnthropicClaudePayloadGenerator();
    String payload = g.generatePayload("Hello Opus", params);

    assertThat(payload).contains(BedrockConstants.JsonKeys.MESSAGES);
    assertThat(payload).contains("anthropic_version");
    assertThat(payload).contains("max_tokens");
    assertThat(payload).doesNotContain("temperature");
    assertThat(payload).doesNotContain("top_p");
    assertThat(payload).doesNotContain("top_k");
  }
}
