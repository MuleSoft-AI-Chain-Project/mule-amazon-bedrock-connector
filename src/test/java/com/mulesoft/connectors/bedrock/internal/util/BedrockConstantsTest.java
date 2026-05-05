package com.mulesoft.connectors.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockConstants")
class BedrockConstantsTest {

  @Test
  @DisplayName("utility class has private constructor")
  void privateConstructor() throws Exception {
    Constructor<BedrockConstants> c = BedrockConstants.class.getDeclaredConstructor();
    c.setAccessible(true);
    c.newInstance();
    assertThat(c.getModifiers()).isEqualTo(2); // private
  }

  @Test
  @DisplayName("INFERENCE_PROFILE_ARN_TEMPLATE is set")
  void awsConstants() {
    assertThat(BedrockConstants.INFERENCE_PROFILE_ARN_TEMPLATE).contains("arn:aws:bedrock:");
  }

  @Test
  @DisplayName("ModelPatterns contains expected patterns")
  void modelPatterns() {
    assertThat(BedrockConstants.ModelPatterns.AMAZON_TITAN_TEXT).isEqualTo("amazon.titan-text");
    assertThat(BedrockConstants.ModelPatterns.AMAZON_NOVA).isEqualTo("amazon.nova");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE).isEqualTo("anthropic.claude");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_OPUS).isEqualTo("anthropic.claude-opus-4");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_SONNET).isEqualTo("anthropic.claude-sonnet-4");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_4_HAIKU).isEqualTo("anthropic.claude-haiku-4");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE_OPUS_4_7).isEqualTo("claude-opus-4-7");
  }

  @Test
  @DisplayName("GeoPrefix constants are defined")
  void geoPrefix() {
    assertThat(BedrockConstants.GeoPrefix.US).isEqualTo("us.");
    assertThat(BedrockConstants.GeoPrefix.EU).isEqualTo("eu.");
    assertThat(BedrockConstants.GeoPrefix.APAC).isEqualTo("apac.");
    assertThat(BedrockConstants.GeoPrefix.JP).isEqualTo("jp.");
    assertThat(BedrockConstants.GeoPrefix.AU).isEqualTo("au.");
    assertThat(BedrockConstants.GeoPrefix.GLOBAL).isEqualTo("global.");
    assertThat(BedrockConstants.GeoPrefix.NONE).isEqualTo("");
  }

  @Test
  @DisplayName("ResponseGroups contains expected groups")
  void responseGroups() {
    assertThat(BedrockConstants.ResponseGroups.CLAUDE).isEqualTo("claude");
    assertThat(BedrockConstants.ResponseGroups.DEFAULT).isEqualTo("default");
  }

  @Test
  @DisplayName("JsonKeys are set")
  void jsonKeys() {
    assertThat(BedrockConstants.JsonKeys.TEXT).isEqualTo("text");
    assertThat(BedrockConstants.JsonKeys.USAGE).isEqualTo("usage");
  }

  @Test
  @DisplayName("ErrorMessages and CLAUDE prefix/suffix are set")
  void errorMessagesAndClaudeFormat() {
    assertThat(BedrockConstants.ErrorMessages.MODEL_INVOCATION_FAILED).contains("%s");
    assertThat(BedrockConstants.CLAUDE_HUMAN_PREFIX).contains("Human");
    assertThat(BedrockConstants.CLAUDE_ASSISTANT_SUFFIX).contains("Assistant");
  }
}
