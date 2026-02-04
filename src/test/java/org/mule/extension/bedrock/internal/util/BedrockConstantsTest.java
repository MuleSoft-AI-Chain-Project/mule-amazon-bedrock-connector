package org.mule.extension.bedrock.internal.util;

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
  @DisplayName("DEFAULT_AWS_ACCOUNT_ID and INFERENCE_PROFILE_ARN_TEMPLATE are set")
  void awsConstants() {
    assertThat(BedrockConstants.DEFAULT_AWS_ACCOUNT_ID).isNotEmpty();
    assertThat(BedrockConstants.INFERENCE_PROFILE_ARN_TEMPLATE).contains("arn:aws:bedrock:");
  }

  @Test
  @DisplayName("ModelPatterns contains expected patterns")
  void modelPatterns() {
    assertThat(BedrockConstants.ModelPatterns.AMAZON_TITAN_TEXT).isEqualTo("amazon.titan-text");
    assertThat(BedrockConstants.ModelPatterns.AMAZON_NOVA).isEqualTo("amazon.nova");
    assertThat(BedrockConstants.ModelPatterns.ANTHROPIC_CLAUDE).isEqualTo("anthropic.claude");
  }

  @Test
  @DisplayName("ResponseGroups contains expected groups")
  void responseGroups() {
    assertThat(BedrockConstants.ResponseGroups.CLAUDE).isEqualTo("claude");
    assertThat(BedrockConstants.ResponseGroups.DEFAULT).isEqualTo("default");
  }

  @Test
  @DisplayName("QUESTION_WORDS and JsonKeys are non-empty")
  void questionWordsAndJsonKeys() {
    assertThat(BedrockConstants.QUESTION_WORDS).isNotEmpty();
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
