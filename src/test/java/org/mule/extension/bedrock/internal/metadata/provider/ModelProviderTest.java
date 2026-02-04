package org.mule.extension.bedrock.internal.metadata.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ModelProvider")
class ModelProviderTest {

  @Test
  @DisplayName("getModelIdPrefix returns prefix")
  void getModelIdPrefix() {
    assertThat(ModelProvider.AMAZON.getModelIdPrefix()).isEqualTo("amazon.titan-text");
    assertThat(ModelProvider.ANTHROPIC.getModelIdPrefix()).isEqualTo("anthropic.claude");
  }

  @Nested
  @DisplayName("fromModelId")
  class FromModelId {

    @Test
    @DisplayName("returns matching provider when model ID contains prefix")
    void returnsMatching() {
      assertThat(ModelProvider.fromModelId("amazon.titan-text-express-v1")).hasValue(ModelProvider.AMAZON);
      assertThat(ModelProvider.fromModelId("anthropic.claude-3-5-sonnet")).hasValue(ModelProvider.ANTHROPIC);
      assertThat(ModelProvider.fromModelId("ai21.j2-mid-v1")).hasValue(ModelProvider.AI21);
      assertThat(ModelProvider.fromModelId("mistral.mistral-7b")).hasValue(ModelProvider.MISTRAL);
      assertThat(ModelProvider.fromModelId("meta.llama3-70b")).hasValue(ModelProvider.META);
      assertThat(ModelProvider.fromModelId("cohere.command-r")).hasValue(ModelProvider.COHERE);
      assertThat(ModelProvider.fromModelId("stability.stable-diffusion")).hasValue(ModelProvider.STABILITY);
    }

    @Test
    @DisplayName("returns empty when no provider matches")
    void returnsEmptyWhenNoMatch() {
      assertThat(ModelProvider.fromModelId("unknown.model")).isEmpty();
    }

    @Test
    @DisplayName("first matching provider is returned when multiple could match")
    void firstMatchWins() {
      Optional<ModelProvider> result = ModelProvider.fromModelId("amazon.nova-lite");
      assertThat(result).isPresent();
    }
  }
}
