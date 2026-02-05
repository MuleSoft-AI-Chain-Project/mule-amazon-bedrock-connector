package com.mulesoft.connectors.bedrock.internal.helper.payload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@DisplayName("ModelTypeIdentifier")
class ModelTypeIdentifierTest {

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("returns TEXT for null and blank")
  void nullAndBlankReturnsText(String modelId) {
    assertThat(ModelTypeIdentifier.identifyType(modelId)).isEqualTo(ModelType.TEXT);
  }

  @Test
  @DisplayName("identifies MODERATION")
  void identifiesModeration() {
    assertThat(ModelTypeIdentifier.identifyType("amazon.moderation-v1")).isEqualTo(ModelType.MODERATION);
    assertThat(ModelTypeIdentifier.identifyType("model-moderate-1")).isEqualTo(ModelType.MODERATION);
  }

  @Test
  @DisplayName("identifies VISION")
  void identifiesVision() {
    assertThat(ModelTypeIdentifier.identifyType("anthropic.claude-3-vision")).isEqualTo(ModelType.VISION);
    assertThat(ModelTypeIdentifier.identifyType("mistral.pixtral-large")).isEqualTo(ModelType.VISION);
    assertThat(ModelTypeIdentifier.identifyType("meta.llama3-3-multimodal")).isEqualTo(ModelType.VISION);
  }

  @Test
  @DisplayName("identifies IMAGE")
  void identifiesImage() {
    assertThat(ModelTypeIdentifier.identifyType("stability.stable-diffusion")).isEqualTo(ModelType.IMAGE);
    assertThat(ModelTypeIdentifier.identifyType("amazon.titan-image-generator")).isEqualTo(ModelType.IMAGE);
  }

  @Test
  @DisplayName("identifies TEXT for common text models")
  void identifiesText() {
    assertThat(ModelTypeIdentifier.identifyType("amazon.titan-text-express")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("amazon.nova-lite")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("anthropic.claude-2")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("ai21.jamba")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("mistral.mistral-7b")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("cohere.command")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("meta.llama3")).isEqualTo(ModelType.TEXT);
    assertThat(ModelTypeIdentifier.identifyType("amazon.titan-embed-text")).isEqualTo(ModelType.TEXT);
  }

  @Test
  @DisplayName("returns TEXT for blank (whitespace only)")
  void blankReturnsText() {
    assertThat(ModelTypeIdentifier.identifyType("   ")).isEqualTo(ModelType.TEXT);
  }

  @Test
  @DisplayName("identifies VISION for claude-4")
  void identifiesClaude4Vision() {
    assertThat(ModelTypeIdentifier.identifyType("anthropic.claude-4-sonnet")).isEqualTo(ModelType.VISION);
  }

  @Test
  @DisplayName("identifies IMAGE for diffusion")
  void identifiesImageDiffusion() {
    assertThat(ModelTypeIdentifier.identifyType("custom.diffusion-model")).isEqualTo(ModelType.IMAGE);
  }

  @Test
  @DisplayName("returns TEXT for unknown model id")
  void unknownModelReturnsText() {
    assertThat(ModelTypeIdentifier.identifyType("unknown.xyz-model")).isEqualTo(ModelType.TEXT);
  }
}
