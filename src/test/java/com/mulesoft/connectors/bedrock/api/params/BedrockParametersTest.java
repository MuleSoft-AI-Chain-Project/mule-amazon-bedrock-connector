package com.mulesoft.connectors.bedrock.api.params;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockAgentsResponseLoggingParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockImageParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbeddingDocument;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParamsModelDetails;
import com.mulesoft.connectors.bedrock.internal.support.IntegrationTestParamHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockParameters getters")
class BedrockParametersTest {

  @Test
  @DisplayName("BedrockParameters getters return set values")
  void bedrockParametersGetters() {
    BedrockParameters p = IntegrationTestParamHelper.bedrockParams("amazon.nova-lite-v1:0", 0.5f, 100);
    assertThat(p.getModelName()).isEqualTo("amazon.nova-lite-v1:0");
    assertThat(p.getTemperature()).isEqualTo(0.5f);
    assertThat(p.getMaxTokenCount()).isEqualTo(100);
  }

  @Test
  @DisplayName("BedrockAgentsSessionParameters getters return set values")
  void sessionParamsGetters() {
    BedrockAgentsSessionParameters p = IntegrationTestParamHelper.sessionParams("sess-1", true, 2);
    assertThat(p.getSessionId()).isEqualTo("sess-1");
    assertThat(p.getExcludePreviousThinkingSteps()).isTrue();
    assertThat(p.getPreviousConversationTurnsToInclude()).isEqualTo(2);
  }

  @Test
  @DisplayName("BedrockAgentsResponseParameters getters return set values")
  void responseParamsGetters() {
    BedrockAgentsResponseParameters p =
        IntegrationTestParamHelper.responseParams(30, TimeUnit.SECONDS, true, 3, 1000L);
    assertThat(p.getRequestTimeout()).isEqualTo(30);
    assertThat(p.getRequestTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
    assertThat(p.getEnableRetry()).isTrue();
    assertThat(p.getMaxRetries()).isEqualTo(3);
    assertThat(p.getRetryBackoffMs()).isEqualTo(1000L);
  }

  @Test
  @DisplayName("BedrockParameters getTopP getTopK getGuardrail getAwsAccountId")
  void bedrockParamsOptionalGetters() throws Exception {
    BedrockParameters p = new BedrockParameters();
    IntegrationTestParamHelper.setField(p, "topP", 0.9f);
    IntegrationTestParamHelper.setField(p, "topK", 40);
    IntegrationTestParamHelper.setField(p, "guardrailIdentifier", "g-1");
    IntegrationTestParamHelper.setField(p, "guardrailVersion", "1.0");
    IntegrationTestParamHelper.setField(p, "awsAccountId", "123456789");
    assertThat(p.getTopP()).isEqualTo(0.9f);
    assertThat(p.getTopK()).isEqualTo(40);
    assertThat(p.getGuardrailIdentifier()).isEqualTo("g-1");
    assertThat(p.getGuardrailVersion()).isEqualTo("1.0");
    assertThat(p.getAwsAccountId()).isEqualTo("123456789");
  }

  @Test
  @DisplayName("BedrockAgentsResponseParameters getMaxRetries getRetryBackoffMs return null when fields are null")
  void responseParamsNullRetryDefaults() throws Exception {
    BedrockAgentsResponseParameters p = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p, "maxRetries", null);
    IntegrationTestParamHelper.setField(p, "retryBackoffMs", null);
    assertThat(p.getMaxRetries()).isNull();
    assertThat(p.getRetryBackoffMs()).isNull();
  }

  @Test
  @DisplayName("BedrockImageParameters getters return set values")
  void imageParamsGetters() throws Exception {
    BedrockImageParameters p = new BedrockImageParameters();
    IntegrationTestParamHelper.setField(p, "modelName", "stability.stable-diffusion-xl-v1");
    IntegrationTestParamHelper.setField(p, "numOfImages", 2);
    IntegrationTestParamHelper.setField(p, "height", 512);
    IntegrationTestParamHelper.setField(p, "width", 768);
    IntegrationTestParamHelper.setField(p, "cfgScale", 7.5f);
    IntegrationTestParamHelper.setField(p, "seed", 42);
    assertThat(p.getModelName()).isEqualTo("stability.stable-diffusion-xl-v1");
    assertThat(p.getNumOfImages()).isEqualTo(2);
    assertThat(p.getHeight()).isEqualTo(512);
    assertThat(p.getWidth()).isEqualTo(768);
    assertThat(p.getCfgScale()).isEqualTo(7.5f);
    assertThat(p.getSeed()).isEqualTo(42);
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters getters return set values")
  void filteringParamsGetters() throws Exception {
    BedrockAgentsFilteringParameters p = new BedrockAgentsFilteringParameters();
    IntegrationTestParamHelper.setField(p, "knowledgeBaseId", "kb-1");
    IntegrationTestParamHelper.setField(p, "numberOfResults", 10);
    IntegrationTestParamHelper.setField(p, "overrideSearchType",
                                        BedrockAgentsFilteringParameters.SearchType.HYBRID);
    IntegrationTestParamHelper.setField(p, "retrievalMetadataFilterType",
                                        BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.AND_ALL);
    IntegrationTestParamHelper.setField(p, "metadataFilters",
                                        java.util.Collections.singletonMap("k", "v"));
    assertThat(p.getKnowledgeBaseId()).isEqualTo("kb-1");
    assertThat(p.getNumberOfResults()).isEqualTo(10);
    assertThat(p.getOverrideSearchType()).isEqualTo(BedrockAgentsFilteringParameters.SearchType.HYBRID);
    assertThat(p.getRetrievalMetadataFilterType())
        .isEqualTo(BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.AND_ALL);
    assertThat(p.getMetadataFilters()).containsEntry("k", "v");
  }

  @Test
  @DisplayName("BedrockAgentsResponseLoggingParameters getters return set values")
  void responseLoggingParamsGetters() throws Exception {
    BedrockAgentsResponseLoggingParameters p = new BedrockAgentsResponseLoggingParameters();
    IntegrationTestParamHelper.setField(p, "requestId", "req-1");
    IntegrationTestParamHelper.setField(p, "correlationId", "corr-1");
    IntegrationTestParamHelper.setField(p, "userId", "user-1");
    assertThat(p.getRequestId()).isEqualTo("req-1");
    assertThat(p.getCorrelationId()).isEqualTo("corr-1");
    assertThat(p.getUserId()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("BedrockParametersEmbedding getters return set values")
  void embeddingParamsGetters() throws Exception {
    BedrockParametersEmbedding p = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p, "modelName", "amazon.titan-embed-text-v1");
    IntegrationTestParamHelper.setField(p, "dimension", 256);
    IntegrationTestParamHelper.setField(p, "normalize", true);
    assertThat(p.getModelName()).isEqualTo("amazon.titan-embed-text-v1");
    assertThat(p.getDimension()).isEqualTo(256);
    assertThat(p.getNormalize()).isTrue();
  }

  @Test
  @DisplayName("BedrockParametersEmbeddingDocument getters return set values")
  void embeddingDocParamsGetters() throws Exception {
    BedrockParametersEmbeddingDocument p = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p, "modelName", "amazon.titan-embed-text-v2:0");
    IntegrationTestParamHelper.setField(p, "optionType", "FULL");
    assertThat(p.getModelName()).isEqualTo("amazon.titan-embed-text-v2:0");
    assertThat(p.getOptionType()).isEqualTo("FULL");
  }

  @Test
  @DisplayName("BedrockParamsModelDetails getters return set values")
  void modelDetailsParamsGetters() throws Exception {
    BedrockParamsModelDetails p = new BedrockParamsModelDetails();
    IntegrationTestParamHelper.setField(p, "modelName", "amazon.nova-lite-v1:0");
    assertThat(p.getModelName()).isEqualTo("amazon.nova-lite-v1:0");
  }

  @Test
  @DisplayName("BedrockAgentsMultipleFilteringParameters getKnowledgeBases returns set value")
  void multipleFilteringParamsGetters() throws Exception {
    BedrockAgentsMultipleFilteringParameters p = new BedrockAgentsMultipleFilteringParameters();
    BedrockAgentsFilteringParameters.KnowledgeBaseConfig kb =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig("kb-1", 5,
                                                                 BedrockAgentsFilteringParameters.SearchType.SEMANTIC, null,
                                                                 null);
    IntegrationTestParamHelper.setField(p, "knowledgeBases",
                                        java.util.Collections.singletonList(kb));
    assertThat(p.getKnowledgeBases()).hasSize(1);
    assertThat(p.getKnowledgeBases().get(0).getKnowledgeBaseId()).isEqualTo("kb-1");
  }

  @Test
  @DisplayName("BedrockParameters equals returns true for same values")
  void bedrockParametersEqualsTrue() throws Exception {
    BedrockParameters p1 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p1, "modelName", "amazon.nova-lite-v1:0");
    IntegrationTestParamHelper.setField(p1, "temperature", 0.7f);
    IntegrationTestParamHelper.setField(p1, "topP", 0.9f);
    IntegrationTestParamHelper.setField(p1, "topK", 40);
    IntegrationTestParamHelper.setField(p1, "maxTokenCount", 100);

    BedrockParameters p2 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p2, "modelName", "amazon.nova-lite-v1:0");
    IntegrationTestParamHelper.setField(p2, "temperature", 0.7f);
    IntegrationTestParamHelper.setField(p2, "topP", 0.9f);
    IntegrationTestParamHelper.setField(p2, "topK", 40);
    IntegrationTestParamHelper.setField(p2, "maxTokenCount", 100);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockParameters equals returns false for different values")
  void bedrockParametersEqualsFalse() throws Exception {
    BedrockParameters p1 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p1, "modelName", "amazon.nova-lite-v1:0");

    BedrockParameters p2 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p2, "modelName", "amazon.nova-pro-v1:0");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockParameters equals returns false for non-BedrockParameters object")
  void bedrockParametersEqualsNonBedrockParams() throws Exception {
    BedrockParameters p = new BedrockParameters();
    assertThat(p).isNotEqualTo("not a BedrockParameters");
    assertThat(p).isNotEqualTo(null);
  }

  @Test
  @DisplayName("BedrockParameters equals returns true for same object")
  void bedrockParametersEqualsSameObject() throws Exception {
    BedrockParameters p = new BedrockParameters();
    assertThat(p).isEqualTo(p);
  }

  @Test
  @DisplayName("BedrockParameters equals checks guardrail fields")
  void bedrockParametersEqualsGuardrails() throws Exception {
    BedrockParameters p1 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p1, "guardrailIdentifier", "g-1");
    IntegrationTestParamHelper.setField(p1, "guardrailVersion", "1.0");

    BedrockParameters p2 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p2, "guardrailIdentifier", "g-1");
    IntegrationTestParamHelper.setField(p2, "guardrailVersion", "1.0");

    assertThat(p1).isEqualTo(p2);

    BedrockParameters p3 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p3, "guardrailIdentifier", "g-2");
    IntegrationTestParamHelper.setField(p3, "guardrailVersion", "1.0");

    assertThat(p1).isNotEqualTo(p3);
  }

  @Test
  @DisplayName("BedrockParameters equals checks awsAccountId")
  void bedrockParametersEqualsAwsAccountId() throws Exception {
    BedrockParameters p1 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p1, "awsAccountId", "123456789");

    BedrockParameters p2 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p2, "awsAccountId", "123456789");

    assertThat(p1).isEqualTo(p2);

    BedrockParameters p3 = new BedrockParameters();
    IntegrationTestParamHelper.setField(p3, "awsAccountId", "987654321");

    assertThat(p1).isNotEqualTo(p3);
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals returns true for same values")
  void embeddingParamsEquals() throws Exception {
    BedrockParametersEmbedding p1 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p1, "modelName", "amazon.titan-embed-text-v1");
    IntegrationTestParamHelper.setField(p1, "dimension", 256);

    BedrockParametersEmbedding p2 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p2, "modelName", "amazon.titan-embed-text-v1");
    IntegrationTestParamHelper.setField(p2, "dimension", 256);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals returns false for different values")
  void embeddingParamsNotEquals() throws Exception {
    BedrockParametersEmbedding p1 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p1, "modelName", "amazon.titan-embed-text-v1");

    BedrockParametersEmbedding p2 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p2, "modelName", "amazon.titan-embed-text-v2:0");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters RerankingConfiguration getters")
  void rerankingConfigGetters() throws Exception {
    BedrockAgentsFilteringParameters.RerankingConfiguration config =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();

    IntegrationTestParamHelper.setField(config, "rerankingType", "BEDROCK");
    IntegrationTestParamHelper.setField(config, "modelArn", "arn:aws:bedrock:us-east-1::model");
    IntegrationTestParamHelper.setField(config, "numberOfRerankedResults", 5);
    IntegrationTestParamHelper.setField(config, "selectionMode",
                                        BedrockAgentsFilteringParameters.RerankingSelectionMode.SELECTIVE);
    IntegrationTestParamHelper.setField(config, "fieldsToInclude", java.util.Arrays.asList("field1", "field2"));
    IntegrationTestParamHelper.setField(config, "fieldsToExclude", java.util.Arrays.asList("field3"));
    IntegrationTestParamHelper.setField(config, "additionalModelRequestFields",
                                        java.util.Collections.singletonMap("key", "value"));

    assertThat(config.getRerankingType()).isEqualTo("BEDROCK");
    assertThat(config.getModelArn()).isEqualTo("arn:aws:bedrock:us-east-1::model");
    assertThat(config.getNumberOfRerankedResults()).isEqualTo(5);
    assertThat(config.getSelectionMode())
        .isEqualTo(BedrockAgentsFilteringParameters.RerankingSelectionMode.SELECTIVE);
    assertThat(config.getFieldsToInclude()).containsExactly("field1", "field2");
    assertThat(config.getFieldsToExclude()).containsExactly("field3");
    assertThat(config.getAdditionalModelRequestFields()).containsEntry("key", "value");
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters RerankingConfiguration setters")
  void rerankingConfigSetters() {
    BedrockAgentsFilteringParameters.RerankingConfiguration config =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();

    config.setRerankingType("CUSTOM");
    config.setModelArn("arn:custom");
    config.setNumberOfRerankedResults(10);
    config.setSelectionMode(BedrockAgentsFilteringParameters.RerankingSelectionMode.ALL);
    config.setFieldsToInclude(java.util.Arrays.asList("f1"));
    config.setFieldsToExclude(java.util.Arrays.asList("f2"));
    config.setAdditionalModelRequestFields(java.util.Collections.singletonMap("k", "v"));

    assertThat(config.getRerankingType()).isEqualTo("CUSTOM");
    assertThat(config.getModelArn()).isEqualTo("arn:custom");
    assertThat(config.getNumberOfRerankedResults()).isEqualTo(10);
    assertThat(config.getSelectionMode())
        .isEqualTo(BedrockAgentsFilteringParameters.RerankingSelectionMode.ALL);
    assertThat(config.getFieldsToInclude()).containsExactly("f1");
    assertThat(config.getFieldsToExclude()).containsExactly("f2");
    assertThat(config.getAdditionalModelRequestFields()).containsEntry("k", "v");
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters KnowledgeBaseConfig getters with constructor")
  void knowledgeBaseConfigConstructor() {
    java.util.Map<String, String> filters = java.util.Collections.singletonMap("key", "val");
    BedrockAgentsFilteringParameters.KnowledgeBaseConfig config =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig(
                                                                 "kb-123", 5, BedrockAgentsFilteringParameters.SearchType.HYBRID,
                                                                 BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.OR_ALL,
                                                                 filters);

    assertThat(config.getKnowledgeBaseId()).isEqualTo("kb-123");
    assertThat(config.getNumberOfResults()).isEqualTo(5);
    assertThat(config.getOverrideSearchType()).isEqualTo(BedrockAgentsFilteringParameters.SearchType.HYBRID);
    assertThat(config.getRetrievalMetadataFilterType())
        .isEqualTo(BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.OR_ALL);
    assertThat(config.getMetadataFilters()).containsEntry("key", "val");
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters KnowledgeBaseConfig setRerankingConfiguration")
  void knowledgeBaseConfigSetReranking() {
    BedrockAgentsFilteringParameters.KnowledgeBaseConfig config =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig();

    BedrockAgentsFilteringParameters.RerankingConfiguration rerank =
        new BedrockAgentsFilteringParameters.RerankingConfiguration();
    rerank.setModelArn("arn:aws:bedrock:us-east-1::model");

    config.setRerankingConfiguration(rerank);

    assertThat(config.getRerankingConfiguration()).isNotNull();
    assertThat(config.getRerankingConfiguration().getModelArn()).isEqualTo("arn:aws:bedrock:us-east-1::model");
  }

  @Test
  @DisplayName("BedrockAgentsFilteringParameters KnowledgeBaseConfig default constructor")
  void knowledgeBaseConfigDefaultConstructor() {
    BedrockAgentsFilteringParameters.KnowledgeBaseConfig config =
        new BedrockAgentsFilteringParameters.KnowledgeBaseConfig();

    assertThat(config.getKnowledgeBaseId()).isNull();
    assertThat(config.getNumberOfResults()).isNull();
    assertThat(config.getOverrideSearchType()).isNull();
    assertThat(config.getRetrievalMetadataFilterType()).isNull();
    assertThat(config.getMetadataFilters()).isNull();
    assertThat(config.getRerankingConfiguration()).isNull();
  }

  @Test
  @DisplayName("BedrockAgentsResponseLoggingParameters equals and hashCode")
  void responseLoggingParamsEqualsHashCode() throws Exception {
    BedrockAgentsResponseLoggingParameters p1 = new BedrockAgentsResponseLoggingParameters();
    IntegrationTestParamHelper.setField(p1, "requestId", "req-1");
    IntegrationTestParamHelper.setField(p1, "correlationId", "corr-1");
    IntegrationTestParamHelper.setField(p1, "userId", "user-1");

    BedrockAgentsResponseLoggingParameters p2 = new BedrockAgentsResponseLoggingParameters();
    IntegrationTestParamHelper.setField(p2, "requestId", "req-1");
    IntegrationTestParamHelper.setField(p2, "correlationId", "corr-1");
    IntegrationTestParamHelper.setField(p2, "userId", "user-1");

    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockParametersEmbeddingDocument dimension and normalize getters")
  void embeddingDocDimensionNormalize() throws Exception {
    BedrockParametersEmbeddingDocument p = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p, "dimension", 512);
    IntegrationTestParamHelper.setField(p, "normalize", true);

    assertThat(p.getDimension()).isEqualTo(512);
    assertThat(p.getNormalize()).isTrue();
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals with non-object returns false")
  void embeddingParamsEqualsNonObject() throws Exception {
    BedrockParametersEmbedding p = new BedrockParametersEmbedding();
    assertThat(p).isNotEqualTo("not-embedding-params");
    assertThat(p).isNotEqualTo(null);
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals with same object returns true")
  void embeddingParamsEqualsSameObject() throws Exception {
    BedrockParametersEmbedding p = new BedrockParametersEmbedding();
    assertThat(p).isEqualTo(p);
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals with different normalize returns false")
  void embeddingParamsEqualsDifferentNormalize() throws Exception {
    BedrockParametersEmbedding p1 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p1, "normalize", true);

    BedrockParametersEmbedding p2 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p2, "normalize", false);

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockParametersEmbedding equals with different dimension returns false")
  void embeddingParamsEqualsDifferentDimension() throws Exception {
    BedrockParametersEmbedding p1 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p1, "dimension", 512);

    BedrockParametersEmbedding p2 = new BedrockParametersEmbedding();
    IntegrationTestParamHelper.setField(p2, "dimension", 1024);

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockParametersEmbeddingDocument equals returns true for same values")
  void embeddingDocParamsEquals() throws Exception {
    BedrockParametersEmbeddingDocument p1 = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p1, "modelName", "amazon.titan-embed-text-v2:0");
    IntegrationTestParamHelper.setField(p1, "dimension", 256);

    BedrockParametersEmbeddingDocument p2 = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p2, "modelName", "amazon.titan-embed-text-v2:0");
    IntegrationTestParamHelper.setField(p2, "dimension", 256);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockParametersEmbeddingDocument equals returns false for different values")
  void embeddingDocParamsNotEquals() throws Exception {
    BedrockParametersEmbeddingDocument p1 = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p1, "modelName", "model1");

    BedrockParametersEmbeddingDocument p2 = new BedrockParametersEmbeddingDocument();
    IntegrationTestParamHelper.setField(p2, "modelName", "model2");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockParametersEmbeddingDocument equals with non-object returns false")
  void embeddingDocParamsEqualsNonObject() throws Exception {
    BedrockParametersEmbeddingDocument p = new BedrockParametersEmbeddingDocument();
    assertThat(p).isNotEqualTo("string");
    assertThat(p).isNotEqualTo(null);
  }

  @Test
  @DisplayName("BedrockImageParameters equals returns true for same values")
  void imageParamsEquals() throws Exception {
    BedrockImageParameters p1 = new BedrockImageParameters();
    IntegrationTestParamHelper.setField(p1, "modelName", "stability.stable-diffusion-xl-v1");
    IntegrationTestParamHelper.setField(p1, "numOfImages", 2);
    IntegrationTestParamHelper.setField(p1, "height", 512);
    IntegrationTestParamHelper.setField(p1, "width", 768);

    BedrockImageParameters p2 = new BedrockImageParameters();
    IntegrationTestParamHelper.setField(p2, "modelName", "stability.stable-diffusion-xl-v1");
    IntegrationTestParamHelper.setField(p2, "numOfImages", 2);
    IntegrationTestParamHelper.setField(p2, "height", 512);
    IntegrationTestParamHelper.setField(p2, "width", 768);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockImageParameters equals returns false for different values")
  void imageParamsNotEquals() throws Exception {
    BedrockImageParameters p1 = new BedrockImageParameters();
    IntegrationTestParamHelper.setField(p1, "modelName", "model1");

    BedrockImageParameters p2 = new BedrockImageParameters();
    IntegrationTestParamHelper.setField(p2, "modelName", "model2");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockImageParameters equals with non-object returns false")
  void imageParamsEqualsNonObject() throws Exception {
    BedrockImageParameters p = new BedrockImageParameters();
    assertThat(p).isNotEqualTo("string");
    assertThat(p).isNotEqualTo(null);
  }

  @Test
  @DisplayName("BedrockAgentsSessionParameters equals returns true for same values")
  void sessionParamsEquals() throws Exception {
    BedrockAgentsSessionParameters p1 = new BedrockAgentsSessionParameters();
    IntegrationTestParamHelper.setField(p1, "sessionId", "sess-1");

    BedrockAgentsSessionParameters p2 = new BedrockAgentsSessionParameters();
    IntegrationTestParamHelper.setField(p2, "sessionId", "sess-1");

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockAgentsSessionParameters equals returns false for different values")
  void sessionParamsNotEquals() throws Exception {
    BedrockAgentsSessionParameters p1 = new BedrockAgentsSessionParameters();
    IntegrationTestParamHelper.setField(p1, "sessionId", "sess-1");

    BedrockAgentsSessionParameters p2 = new BedrockAgentsSessionParameters();
    IntegrationTestParamHelper.setField(p2, "sessionId", "sess-2");

    assertThat(p1).isNotEqualTo(p2);
  }

  @Test
  @DisplayName("BedrockAgentsResponseParameters equals returns true for same values")
  void responseParamsEquals() throws Exception {
    BedrockAgentsResponseParameters p1 = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p1, "requestTimeout", 30);
    IntegrationTestParamHelper.setField(p1, "enableRetry", true);

    BedrockAgentsResponseParameters p2 = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p2, "requestTimeout", 30);
    IntegrationTestParamHelper.setField(p2, "enableRetry", true);

    assertThat(p1).isEqualTo(p2);
    assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
  }

  @Test
  @DisplayName("BedrockAgentsResponseParameters equals returns false for different values")
  void responseParamsNotEquals() throws Exception {
    BedrockAgentsResponseParameters p1 = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p1, "requestTimeout", 30);

    BedrockAgentsResponseParameters p2 = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p2, "requestTimeout", 60);

    assertThat(p1).isNotEqualTo(p2);
  }
}
