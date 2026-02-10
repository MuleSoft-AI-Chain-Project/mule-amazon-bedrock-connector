package com.mulesoft.connectors.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ModelIdentifier")
class ModelIdentifierTest {

  @Nested
  @DisplayName("requiresInferenceProfileArn")
  class RequiresInferenceProfileArn {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("returns false for null and blank")
    void returnsFalseForNullAndBlank(String modelId) {
      assertThat(ModelIdentifier.requiresInferenceProfileArn(modelId)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "amazon.nova-premier-v1",
        "anthropic.claude-3-5-sonnet",
        "mistral.pixtral-large",
        "meta.llama4",
        "meta.llama3-3-70b",
        "meta.llama3-2-90b",
        "meta.llama3-1-8b"
    })
    @DisplayName("returns true for inference-profile models")
    void returnsTrueForInferenceProfileModels(String modelId) {
      assertThat(ModelIdentifier.requiresInferenceProfileArn(modelId)).isTrue();
    }

    @Test
    @DisplayName("returns false for standard models")
    void returnsFalseForStandardModels() {
      assertThat(ModelIdentifier.requiresInferenceProfileArn("amazon.titan-text-express-v1")).isFalse();
      assertThat(ModelIdentifier.requiresInferenceProfileArn("amazon.nova-lite-v1:0")).isFalse();
    }
  }

  @Nested
  @DisplayName("buildInferenceProfileArn")
  class BuildInferenceProfileArn {

    @Test
    @DisplayName("builds ARN with region account and model")
    void buildsArn() {
      String arn = ModelIdentifier.buildInferenceProfileArn("us-east-1", "123456789", "anthropic.claude-3");
      assertThat(arn).isEqualTo("arn:aws:bedrock:us-east-1:123456789:inference-profile/us.anthropic.claude-3");
    }
  }

  @Nested
  @DisplayName("getResponseGroup")
  class GetResponseGroup {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("returns DEFAULT for null and blank")
    void returnsDefaultForNullAndBlank(String modelId) {
      assertThat(ModelIdentifier.getResponseGroup(modelId)).isEqualTo(BedrockConstants.ResponseGroups.DEFAULT);
    }

    @Test
    @DisplayName("returns correct group for model IDs")
    void returnsCorrectGroup() {
      assertThat(ModelIdentifier.getResponseGroup("anthropic.claude-3")).isEqualTo(BedrockConstants.ResponseGroups.CLAUDE);
      assertThat(ModelIdentifier.getResponseGroup("mistral.pixtral-large"))
          .isEqualTo(BedrockConstants.ResponseGroups.MISTRAL_PIXTRAL);
      assertThat(ModelIdentifier.getResponseGroup("mistral.mistral-7b"))
          .isEqualTo(BedrockConstants.ResponseGroups.MISTRAL_MISTRAL);
      assertThat(ModelIdentifier.getResponseGroup("ai21.jamba")).isEqualTo(BedrockConstants.ResponseGroups.JAMBA);
      assertThat(ModelIdentifier.getResponseGroup("meta.llama3")).isEqualTo(BedrockConstants.ResponseGroups.LLAMA);
      assertThat(ModelIdentifier.getResponseGroup("amazon.titan-text")).isEqualTo(BedrockConstants.ResponseGroups.TITAN);
    }
  }

  @Nested
  @DisplayName("matchesPattern")
  class MatchesPattern {

    @Test
    @DisplayName("returns false for null or blank modelId")
    void returnsFalseForNullOrBlank() {
      assertThat(ModelIdentifier.matchesPattern(null, "titan")).isFalse();
      assertThat(ModelIdentifier.matchesPattern("", "titan")).isFalse();
      assertThat(ModelIdentifier.matchesPattern("   ", "titan")).isFalse();
    }

    @Test
    @DisplayName("returns true when modelId contains pattern")
    void returnsTrueWhenContains() {
      assertThat(ModelIdentifier.matchesPattern("amazon.titan-text", "titan")).isTrue();
    }

    @Test
    @DisplayName("returns false when modelId does not contain pattern")
    void returnsFalseWhenNotContains() {
      assertThat(ModelIdentifier.matchesPattern("amazon.nova", "titan")).isFalse();
    }
  }

  @Nested
  @DisplayName("requireAccountId")
  class RequireAccountId {

    @Test
    @DisplayName("returns account ID when non-null and non-blank")
    void returnsProvided() {
      assertThat(ModelIdentifier.requireAccountId("123456789")).isEqualTo("123456789");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("throws ModuleException when null or blank")
    void throwsWhenNullOrBlank(String accountId) {
      assertThatThrownBy(() -> ModelIdentifier.requireAccountId(accountId))
          .isInstanceOf(org.mule.runtime.extension.api.exception.ModuleException.class)
          .hasMessageContaining("AWS account ID is required");
    }
  }
}
