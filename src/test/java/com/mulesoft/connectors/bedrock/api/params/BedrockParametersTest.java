package com.mulesoft.connectors.bedrock.api.params;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
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
  @DisplayName("BedrockAgentsResponseParameters getMaxRetries getRetryBackoffMs return defaults when null")
  void responseParamsNullRetryDefaults() throws Exception {
    BedrockAgentsResponseParameters p = new BedrockAgentsResponseParameters();
    IntegrationTestParamHelper.setField(p, "maxRetries", null);
    IntegrationTestParamHelper.setField(p, "retryBackoffMs", null);
    assertThat(p.getMaxRetries()).isEqualTo(3);
    assertThat(p.getRetryBackoffMs()).isEqualTo(1000L);
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
}
