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
        "anthropic.claude-opus-4-20250514-v1:0",
        "anthropic.claude-opus-4-1-20250805-v1:0",
        "anthropic.claude-sonnet-4-5-20250929-v1:0",
        "anthropic.claude-haiku-4-5-20251001-v1:0",
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

    @ParameterizedTest
    @ValueSource(strings = {
        "us.anthropic.claude-sonnet-4-5-20250929-v1:0",
        "eu.anthropic.claude-sonnet-4-20250514-v1:0",
        "global.anthropic.claude-opus-4-7-20260101-v1:0",
        "apac.anthropic.claude-haiku-4-5-20251001-v1:0"
    })
    @DisplayName("returns true for model IDs already carrying a geo prefix")
    void returnsTrueForGeoPrefixed(String modelId) {
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
    @DisplayName("builds ARN with us. prefix for US regions")
    void buildsArn() {
      String arn = ModelIdentifier.buildInferenceProfileArn("us-east-1", "123456789", "anthropic.claude-3");
      assertThat(arn).isEqualTo("arn:aws:bedrock:us-east-1:123456789:inference-profile/us.anthropic.claude-3");
    }

    @Test
    @DisplayName("builds ARN with eu. prefix for EU regions")
    void buildsArnEu() {
      String arn = ModelIdentifier.buildInferenceProfileArn("eu-west-1", "123456789",
                                                            "anthropic.claude-sonnet-4-20250514-v1:0");
      assertThat(arn).isEqualTo(
                                "arn:aws:bedrock:eu-west-1:123456789:inference-profile/eu.anthropic.claude-sonnet-4-20250514-v1:0");
    }

    @Test
    @DisplayName("builds ARN with apac. prefix for APAC regions")
    void buildsArnApac() {
      String arn = ModelIdentifier.buildInferenceProfileArn("ap-south-1", "123456789", "anthropic.claude-3");
      assertThat(arn).isEqualTo("arn:aws:bedrock:ap-south-1:123456789:inference-profile/apac.anthropic.claude-3");
    }

    @Test
    @DisplayName("uses global. prefix for Claude Opus 4.7 regardless of region")
    void buildsArnOpus47Global() {
      String arn = ModelIdentifier.buildInferenceProfileArn("us-east-1", "123456789",
                                                            "anthropic.claude-opus-4-7-20260101-v1:0");
      assertThat(arn).isEqualTo(
                                "arn:aws:bedrock:us-east-1:123456789:inference-profile/global.anthropic.claude-opus-4-7-20260101-v1:0");
    }

    @Test
    @DisplayName("uses pre-qualified model ID verbatim when geo prefix already present")
    void buildsArnPreQualified() {
      String arn = ModelIdentifier.buildInferenceProfileArn("us-east-1", "123456789",
                                                            "global.anthropic.claude-opus-4-7-20260101-v1:0");
      assertThat(arn).isEqualTo(
                                "arn:aws:bedrock:us-east-1:123456789:inference-profile/global.anthropic.claude-opus-4-7-20260101-v1:0");
    }
  }

  @Nested
  @DisplayName("resolveGeoPrefix")
  class ResolveGeoPrefix {

    @Test
    @DisplayName("returns global. for Opus 4.7 in any region")
    void opus47Global() {
      assertThat(ModelIdentifier.resolveGeoPrefix("us-east-1", "anthropic.claude-opus-4-7-20260101-v1:0"))
          .isEqualTo(BedrockConstants.GeoPrefix.GLOBAL);
      assertThat(ModelIdentifier.resolveGeoPrefix("eu-west-1", "anthropic.claude-opus-4-7-20260101-v1:0"))
          .isEqualTo(BedrockConstants.GeoPrefix.GLOBAL);
    }

    @Test
    @DisplayName("maps region family to geo prefix")
    void regionMapping() {
      assertThat(ModelIdentifier.resolveGeoPrefix("us-east-1", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.US);
      assertThat(ModelIdentifier.resolveGeoPrefix("eu-central-1", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.EU);
      assertThat(ModelIdentifier.resolveGeoPrefix("ap-northeast-1", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.JP);
      assertThat(ModelIdentifier.resolveGeoPrefix("ap-southeast-2", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.AU);
      assertThat(ModelIdentifier.resolveGeoPrefix("ap-south-1", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.APAC);
    }

    @Test
    @DisplayName("defaults to us. when region is null or blank")
    void defaultsToUs() {
      assertThat(ModelIdentifier.resolveGeoPrefix(null, "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.US);
      assertThat(ModelIdentifier.resolveGeoPrefix("", "anthropic.claude-3"))
          .isEqualTo(BedrockConstants.GeoPrefix.US);
    }
  }

  @Nested
  @DisplayName("hasGeoPrefix")
  class HasGeoPrefix {

    @Test
    @DisplayName("returns true for geo-prefixed model IDs")
    void truePrefix() {
      assertThat(ModelIdentifier.hasGeoPrefix("us.anthropic.claude-3")).isTrue();
      assertThat(ModelIdentifier.hasGeoPrefix("eu.anthropic.claude-3")).isTrue();
      assertThat(ModelIdentifier.hasGeoPrefix("global.anthropic.claude-3")).isTrue();
      assertThat(ModelIdentifier.hasGeoPrefix("apac.anthropic.claude-3")).isTrue();
    }

    @Test
    @DisplayName("returns false for non-prefixed and null / blank")
    void falsePrefix() {
      assertThat(ModelIdentifier.hasGeoPrefix("anthropic.claude-3")).isFalse();
      assertThat(ModelIdentifier.hasGeoPrefix(null)).isFalse();
      assertThat(ModelIdentifier.hasGeoPrefix("")).isFalse();
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
