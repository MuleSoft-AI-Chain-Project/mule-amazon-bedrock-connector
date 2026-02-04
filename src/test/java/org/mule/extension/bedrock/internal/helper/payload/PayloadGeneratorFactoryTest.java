package org.mule.extension.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.extension.bedrock.api.params.BedrockParameters;

@DisplayName("PayloadGeneratorFactory")
class PayloadGeneratorFactoryTest {

  private static BedrockParameters minimalParams() {
    BedrockParameters p = mock(BedrockParameters.class);
    when(p.getModelName()).thenReturn("amazon.titan-text-express-v1");
    when(p.getTemperature()).thenReturn(0.7f);
    when(p.getMaxTokenCount()).thenReturn(100);
    when(p.getTopP()).thenReturn(null);
    when(p.getTopK()).thenReturn(null);
    return p;
  }

  @Nested
  @DisplayName("getGenerator")
  class GetGenerator {

    @Test
    @DisplayName("returns generator for known model IDs")
    void returnsGeneratorForKnownModels() {
      assertThat(PayloadGeneratorFactory.getGenerator("amazon.nova-lite-v1:0")).isNotNull();
      assertThat(PayloadGeneratorFactory.getGenerator("anthropic.claude-3-5-sonnet")).isNotNull();
      assertThat(PayloadGeneratorFactory.getGenerator("ai21.jamba-1")).isNotNull();
      assertThat(PayloadGeneratorFactory.getGenerator("mistral.mistral-7b")).isNotNull();
      assertThat(PayloadGeneratorFactory.getGenerator("cohere.command-r")).isNotNull();
      assertThat(PayloadGeneratorFactory.getGenerator("meta.llama3-70b")).isNotNull();
    }

    @Test
    @DisplayName("returns default generator for null and blank")
    void returnsDefaultForNullAndBlank() {
      PayloadGenerator g1 = PayloadGeneratorFactory.getGenerator(null);
      PayloadGenerator g2 = PayloadGeneratorFactory.getGenerator("");
      PayloadGenerator g3 = PayloadGeneratorFactory.getGenerator("   ");
      assertThat(g1).isNotNull();
      assertThat(g2).isNotNull();
      assertThat(g3).isNotNull();
      assertThat(g1.supports("any")).isTrue();
    }

    @Test
    @DisplayName("returned generator produces payload for prompt")
    void producesPayload() {
      PayloadGenerator g = PayloadGeneratorFactory.getGenerator("amazon.nova-lite-v1:0");
      String payload = g.generatePayload("Hello", minimalParams());
      assertThat(payload).isNotBlank();
    }
  }
}
