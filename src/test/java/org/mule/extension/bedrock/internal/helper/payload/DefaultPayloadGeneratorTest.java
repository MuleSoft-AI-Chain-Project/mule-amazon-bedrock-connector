package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.api.params.BedrockParameters;

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
  }
}
