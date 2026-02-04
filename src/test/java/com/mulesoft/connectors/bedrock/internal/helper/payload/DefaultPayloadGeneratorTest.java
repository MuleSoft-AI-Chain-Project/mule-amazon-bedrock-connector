package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;

@DisplayName("DefaultPayloadGenerator")
class DefaultPayloadGeneratorTest {

  private static BedrockParameters params() {
    BedrockParameters p = mock(BedrockParameters.class);
    when(p.getModelName()).thenReturn("unknown.model");
    when(p.getTemperature()).thenReturn(0.7f);
    when(p.getMaxTokenCount()).thenReturn(100);
    when(p.getTopP()).thenReturn(null);
    when(p.getTopK()).thenReturn(null);
    return p;
  }

  @Test
  @DisplayName("supports always returns true")
  void supportsAlwaysTrue() {
    DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.TEXT);
    assertThat(g.supports("any")).isTrue();
  }

  @Nested
  @DisplayName("generatePayload")
  class GeneratePayload {

    @Test
    @DisplayName("TEXT type produces prompt-based or message-based JSON")
    void textPayload() {
      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.TEXT);
      String payload = g.generatePayload("Hello world", params());
      assertThat(payload).isNotBlank();
      assertThat(payload).contains("Hello world");
    }

    @Test
    @DisplayName("VISION type produces message-based JSON")
    void visionPayload() {
      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.VISION);
      String payload = g.generatePayload("Describe this", params());
      assertThat(payload).contains("messages");
      assertThat(payload).contains("Describe this");
    }

    @Test
    @DisplayName("MODERATION type produces inputText JSON")
    void moderationPayload() {
      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.MODERATION);
      String payload = g.generatePayload("Check this text", params());
      assertThat(payload).contains("inputText");
    }

    @Test
    @DisplayName("IMAGE type produces text_prompts JSON")
    void imagePayload() {
      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.IMAGE);
      String payload = g.generatePayload("A red apple", params());
      assertThat(payload).contains("text_prompts");
    }

    @Test
    @DisplayName("TEXT type with message-based model name uses messages format")
    void textPayloadMessageBased() {
      BedrockParameters p = mock(BedrockParameters.class);
      when(p.getModelName()).thenReturn("anthropic.claude-3-5-sonnet");
      when(p.getTemperature()).thenReturn(0.7f);
      when(p.getMaxTokenCount()).thenReturn(100);
      when(p.getTopP()).thenReturn(null);
      when(p.getTopK()).thenReturn(null);

      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.TEXT);
      String payload = g.generatePayload("Hello", p);
      assertThat(payload).contains("messages");
      assertThat(payload).contains("Hello");
    }

    @Test
    @DisplayName("TEXT type with nova in model name uses messages format")
    void textPayloadNovaModel() {
      BedrockParameters p = mock(BedrockParameters.class);
      when(p.getModelName()).thenReturn("amazon.nova-lite");
      when(p.getTemperature()).thenReturn(0.5f);
      when(p.getMaxTokenCount()).thenReturn(200);
      when(p.getTopP()).thenReturn(0.9f);
      when(p.getTopK()).thenReturn(null);

      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.TEXT);
      String payload = g.generatePayload("Hi", p);
      assertThat(payload).contains("messages");
      assertThat(payload).contains("topp");
    }

    @Test
    @DisplayName("TEXT type with topP set includes topP in payload")
    void textPayloadWithTopP() {
      BedrockParameters p = mock(BedrockParameters.class);
      when(p.getModelName()).thenReturn("unknown.model");
      when(p.getTemperature()).thenReturn(0.7f);
      when(p.getMaxTokenCount()).thenReturn(100);
      when(p.getTopP()).thenReturn(0.95f);
      when(p.getTopK()).thenReturn(null);

      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.TEXT);
      String payload = g.generatePayload("Test", p);
      assertThat(payload).contains("0.95");
      assertThat(payload).contains("prompt");
    }

    @Test
    @DisplayName("VISION type with topP set includes topP")
    void visionPayloadWithTopP() {
      BedrockParameters p = mock(BedrockParameters.class);
      when(p.getModelName()).thenReturn("vision.model");
      when(p.getTemperature()).thenReturn(0.6f);
      when(p.getMaxTokenCount()).thenReturn(150);
      when(p.getTopP()).thenReturn(0.9f);
      when(p.getTopK()).thenReturn(null);

      DefaultPayloadGenerator g = new DefaultPayloadGenerator(ModelType.VISION);
      String payload = g.generatePayload("Describe", p);
      assertThat(payload).contains("messages");
      assertThat(payload).contains("0.9");
    }
  }
}
