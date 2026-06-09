package com.mulesoft.connectors.bedrock.internal.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import org.mule.runtime.api.scheduler.Scheduler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsMultipleFilteringParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseLoggingParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsResponseParameters;
import com.mulesoft.connectors.bedrock.api.parameter.BedrockAgentsSessionParameters;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;
import com.mulesoft.connectors.bedrock.internal.error.ErrorHandler;
import com.mulesoft.connectors.bedrock.internal.helper.PromptPayloadHelper;
import com.mulesoft.connectors.bedrock.internal.util.StreamingRetryUtility;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.BedrockModelConfigurations;
import software.amazon.awssdk.services.bedrockagentruntime.model.FieldForReranking;
import software.amazon.awssdk.services.bedrockagentruntime.model.FilterAttribute;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseRetrievalConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseVectorSearchConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;
import software.amazon.awssdk.services.bedrockagentruntime.model.PromptCreationConfigurations;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalFilter;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;
import software.amazon.awssdk.services.bedrockagentruntime.model.VectorSearchBedrockRerankingConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class AgentServiceImpl extends BedrockServiceImpl implements AgentService {

  private static final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

  private static final String AGENT_NAMES = "agentNames";
  private static final String AGENT_ID = "agentId";
  private static final String AGENT_NAME = "agentName";
  private static final String AGENT_ARN = "agentArn";
  private static final String AGENT_STATUS = "agentStatus";
  private static final String AGENT_RESOURCE_ROLE_ARN = "agentResourceRoleArn";
  private static final String CLIENT_TOKEN = "clientToken";
  private static final String CREATED_AT = "createdAt";
  private static final String DESCRIPTION = "description";
  private static final String FOUNDATION_MODEL = "foundationModel";
  private static final String IDLE_SESSION_TTL_IN_SECONDS = "idleSessionTTLInSeconds";
  private static final String INSTRUCTION = "instruction";
  private static final String PROMPT_OVERRIDE_CONFIGURATION = "promptOverrideConfiguration";
  private static final String UPDATED_AT = "updatedAt";
  private static final String SESSION_ID = "sessionId";
  private static final String AGENT_ALIAS = "agentAlias";
  private static final String PROMPT = "prompt";
  private static final String PROCESSED_AT = "processedAt";
  private static final String CHUNKS = "chunks";
  private static final String SUMMARY = "summary";
  private static final String TOTAL_CHUNKS = "totalChunks";
  private static final String FULL_RESPONSE = "fullResponse";
  private static final String TYPE = "type";
  private static final String CHUNK = "chunk";
  private static final String TIMESTAMP = "timestamp";
  private static final String TEXT = "text";
  private static final String CITATIONS = "citations";
  private static final String GENERATED_RESPONSE_PART = "generatedResponsePart";
  private static final String RETRIEVED_REFERENCES = "retrievedReferences";
  private static final String CONTENT = "content";
  private static final String LOCATION = "location";
  private static final String METADATA = "metadata";
  private static final String ERROR_KEY = "error";
  private static final String ERROR_WRITING_EVENT_LOG = "Error writing error event: {}";
  private static final String END_OF_STREAM_SENTINEL = "__END_OF_STREAM__";

  private static final AtomicInteger eventCounter = new AtomicInteger(0);

  public AgentServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                                     BedrockParameters bedrockParameters) {
    long sessionStart = System.currentTimeMillis();
    logger.info("Agent operation [AGENT-define-prompt-template] session-start modelName={}",
                bedrockParameters != null ? bedrockParameters.getModelName() : null);
    try {
      String finalPromptTemplate = PromptPayloadHelper.definePromptTemplate(promptTemplate, instructions, dataset);
      String nativeRequest = PromptPayloadHelper.identifyPayload(finalPromptTemplate, bedrockParameters);
      String region = getConnection().getRegion();
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, region, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters != null ? bedrockParameters.getModelName() : null);
    } finally {
      logger.info("Agent operation [AGENT-define-prompt-template] session-end durationMs={}",
                  System.currentTimeMillis() - sessionStart);
    }

  }

  @Override
  public String listAgents() {
    long sessionStart = System.currentTimeMillis();
    logger.info("Agent operation [AGENT-list] session-start");
    try {
      ListAgentsRequest listAgentsRequest = ListAgentsRequest.builder().build();
      ListAgentsResponse listAgentsResponse = getConnection().listAgents(listAgentsRequest);
      List<AgentSummary> agentSummaries = listAgentsResponse.agentSummaries();
      List<String> agentNames = agentSummaries.stream()
          .map(AgentSummary::agentName)
          .collect(Collectors.toList());
      JSONArray jsonArray = new JSONArray(agentNames);

      // Create a JSONObject to store the JSONArray
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(AGENT_NAMES, jsonArray);

      // Convert the JSONObject to a JSON string
      return jsonObject.toString();
    } finally {
      logger.info("Agent operation [AGENT-list] session-end durationMs={}",
                  System.currentTimeMillis() - sessionStart);
    }
  }

  @Override
  public String getAgentById(String agentId) {
    long sessionStart = System.currentTimeMillis();
    logger.info("Agent operation [AGENT-get-by-id] session-start agentId={}", agentId);
    try {
      GetAgentRequest getAgentRequest = GetAgentRequest.builder()
          .agentId(agentId)
          .build();
      GetAgentResponse getAgentResponse = getConnection().getAgent(getAgentRequest);
      Agent agent = getAgentResponse.agent();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(AGENT_ID, agent.agentId());
      jsonObject.put(AGENT_NAME, agent.agentName());
      jsonObject.put(AGENT_ARN, agent.agentArn());
      jsonObject.put(AGENT_STATUS, agent.agentStatusAsString());
      jsonObject.put(AGENT_RESOURCE_ROLE_ARN, agent.agentResourceRoleArn());
      jsonObject.put(CLIENT_TOKEN, agent.clientToken());
      jsonObject.put(CREATED_AT, agent.createdAt());
      jsonObject.put(DESCRIPTION, agent.description());
      jsonObject.put(FOUNDATION_MODEL, agent.foundationModel());
      jsonObject.put(IDLE_SESSION_TTL_IN_SECONDS, agent.idleSessionTTLInSeconds());
      jsonObject.put(INSTRUCTION, agent.instruction());
      jsonObject.put(PROMPT_OVERRIDE_CONFIGURATION, agent.promptOverrideConfiguration());
      jsonObject.put(UPDATED_AT, agent.updatedAt());

      return jsonObject.toString();
    } finally {
      logger.info("Agent operation [AGENT-get-by-id] session-end agentId={} durationMs={}",
                  agentId, System.currentTimeMillis() - sessionStart);
    }
  }


  @Override
  public String chatWithAgent(String agentId, String agentAliasId, String prompt, boolean enableTrace, boolean latencyOptimized,
                              BedrockAgentsSessionParameters bedrockSessionParameters,
                              BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                              BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                              BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                              BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters) {
    Integer operationTimeout = bedrockAgentsResponseParameters.getRequestTimeout();
    TimeUnit operationTimeoutUnit = bedrockAgentsResponseParameters.getRequestTimeoutUnit();

    String sessionId = bedrockSessionParameters.getSessionId();
    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
    logger.debug("Using sessionId: {}", effectiveSessionId);

    long sessionStart = System.currentTimeMillis();
    logger.info("Agent operation [AGENT-chat] session-start sessionId={} agentId={} agentAliasId={}",
                effectiveSessionId, agentId, agentAliasId);

    // Create retry configuration from response parameters
    StreamingRetryUtility.RetryConfig retryConfig = new StreamingRetryUtility.RetryConfig(
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getMaxRetries() != null
                                                                                                  ? bedrockAgentsResponseParameters
                                                                                                      .getMaxRetries()
                                                                                                  : 3,
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getRetryBackoffMs() != null
                                                                                                  ? bedrockAgentsResponseParameters
                                                                                                      .getRetryBackoffMs()
                                                                                                  : 1000L,
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getEnableRetry());

    try {
      // Execute with retry logic for non-streaming operation
      return StreamingRetryUtility.executeWithRetry(() -> {
        return invokeAgent(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                           bedrockSessionParameters.getExcludePreviousThinkingSteps(),
                           bedrockSessionParameters.getPreviousConversationTurnsToInclude(),
                           buildKnowledgeBaseConfigs(bedrockAgentsFilteringParameters,
                                                     bedrockAgentsMultipleFilteringParameters),
                           operationTimeout, operationTimeoutUnit)
            .thenApply(response -> {
              logger.debug(response);
              return response;
            }).join();
      }, retryConfig);
    } finally {
      logger.info("Agent operation [AGENT-chat] session-end sessionId={} agentId={} durationMs={}",
                  effectiveSessionId, agentId, System.currentTimeMillis() - sessionStart);
    }
  }

  private static java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> buildKnowledgeBaseConfigs(
                                                                                                                BedrockAgentsFilteringParameters legacyParams,
                                                                                                                BedrockAgentsMultipleFilteringParameters multipleParams) {
    if (multipleParams != null && multipleParams.getKnowledgeBases() != null && !multipleParams.getKnowledgeBases().isEmpty()) {
      // If legacy single-KB fields are also provided, warn that they will be ignored
      if (legacyParams != null && (legacyParams.getKnowledgeBaseId() != null
          || legacyParams.getNumberOfResults() != null
          || legacyParams.getOverrideSearchType() != null
          || legacyParams.getRetrievalMetadataFilterType() != null
          || (legacyParams.getMetadataFilters() != null && !legacyParams.getMetadataFilters().isEmpty()))) {
        logger
            .warn("Multiple knowledge bases provided; legacy single-KB fields will be ignored.");
      }
      return multipleParams.getKnowledgeBases();
    }

    if (legacyParams == null) {
      return null;
    }

    // Fallback: if legacy single KB id is provided, map it to a per-KB config
    String id = legacyParams.getKnowledgeBaseId();
    if (id == null || id.isEmpty()) {
      return null;
    }

    return Collections
        .singletonList(new BedrockAgentsFilteringParameters.KnowledgeBaseConfig(
                                                                                id,
                                                                                legacyParams
                                                                                    .getNumberOfResults(),
                                                                                legacyParams
                                                                                    .getOverrideSearchType(),
                                                                                legacyParams
                                                                                    .getRetrievalMetadataFilterType(),
                                                                                legacyParams
                                                                                    .getMetadataFilters()));


  }

  private CompletableFuture<String> invokeAgent(String agentAliasId, String agentId,
                                                String prompt, boolean enableTrace,
                                                boolean latencyOptimized, String effectiveSessionId,
                                                boolean excludePreviousThinkingSteps, Integer previousConversationTurnsToInclude,
                                                java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                                Integer operationTimeout, TimeUnit operationTimeoutUnit) {
    long startTime = System.currentTimeMillis();
    InvokeAgentRequest request = buildInvokeAgentRequest(agentAliasId, agentId, prompt, enableTrace, latencyOptimized,
                                                         effectiveSessionId, excludePreviousThinkingSteps,
                                                         previousConversationTurnsToInclude, knowledgeBaseConfigs,
                                                         operationTimeout, operationTimeoutUnit);
    CompletableFuture<String> completionFuture = new CompletableFuture<>();
    List<JSONObject> chunks = Collections.synchronizedList(new ArrayList<>());
    InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
        .onChunk(chunk -> chunks.add(buildInvokeAgentChunkJson(chunk)))
        .build();
    InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
        .subscriber(visitor)
        .build();
    long effectiveTimeoutMs = (operationTimeout != null && operationTimeout > 0)
        ? toDuration(operationTimeout, operationTimeoutUnit).toMillis()
        : getConnection().getConnectionTimeoutMs();
    CompletableFuture<Void> invocationFuture = getConnection().invokeAgent(request, handler, effectiveTimeoutMs);
    invocationFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        completionFuture.completeExceptionally(throwable);
      } else {
        completionFuture.complete(buildInvokeAgentResult(effectiveSessionId, agentId, agentAliasId, prompt, chunks,
                                                         startTime)
            .toString());
      }
    });
    return completionFuture;
  }

  private InvokeAgentRequest buildInvokeAgentRequest(String agentAliasId, String agentId, String prompt,
                                                     boolean enableTrace, boolean latencyOptimized,
                                                     String effectiveSessionId, boolean excludePreviousThinkingSteps,
                                                     Integer previousConversationTurnsToInclude,
                                                     java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                                     Integer operationTimeout, TimeUnit operationTimeoutUnit) {
    InvokeAgentRequest.Builder requestBuilder = InvokeAgentRequest.builder()
        .agentId(agentId)
        .agentAliasId(agentAliasId)
        .sessionId(effectiveSessionId)
        .inputText(prompt)
        .enableTrace(enableTrace)
        .sessionState(buildSessionState(knowledgeBaseConfigs))
        .bedrockModelConfigurations(buildModelConfigurations(latencyOptimized))
        .promptCreationConfigurations(buildPromptConfigurations(excludePreviousThinkingSteps,
                                                                previousConversationTurnsToInclude));
    if (operationTimeout != null && operationTimeout > 0) {
      Duration timeout = toDuration(operationTimeout, operationTimeoutUnit);
      requestBuilder.overrideConfiguration(
                                           AwsRequestOverrideConfiguration.builder().apiCallTimeout(timeout).build());
    }
    return requestBuilder.build();
  }

  private JSONObject buildInvokeAgentChunkJson(PayloadPart chunk) {
    JSONObject chunkData = new JSONObject();
    chunkData.put(TYPE, CHUNK);
    chunkData.put(TIMESTAMP, Instant.now().toString());
    if (chunk.bytes() != null) {
      String text = new String(chunk.bytes().asByteArray(), StandardCharsets.UTF_8);
      chunkData.put(TEXT, text);
    }
    if (chunk.attribution() != null && chunk.attribution().citations() != null) {
      chunkData.put(CITATIONS, buildCitationsJson(chunk.attribution().citations()));
    }
    return chunkData;
  }

  private JSONArray buildCitationsJson(
                                       java.util.List<software.amazon.awssdk.services.bedrockagentruntime.model.Citation> citations) {
    JSONArray citationsArray = new JSONArray();
    citations.forEach(citation -> citationsArray.put(buildCitationJson(citation)));
    return citationsArray;
  }

  private JSONObject buildCitationJson(
                                       software.amazon.awssdk.services.bedrockagentruntime.model.Citation citation) {
    JSONObject citationData = new JSONObject();
    if (citation.generatedResponsePart() != null
        && citation.generatedResponsePart().textResponsePart() != null) {
      citationData.put(GENERATED_RESPONSE_PART,
                       citation.generatedResponsePart().textResponsePart().text());
    }
    if (citation.retrievedReferences() != null) {
      citationData.put(RETRIEVED_REFERENCES, buildRetrievedReferencesJson(citation.retrievedReferences()));
    }
    return citationData;
  }

  private JSONArray buildRetrievedReferencesJson(
                                                 java.util.List<software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference> refs) {
    JSONArray referencesArray = new JSONArray();
    refs.forEach(ref -> referencesArray.put(buildRetrievedReferenceJson(ref)));
    return referencesArray;
  }

  private JSONObject buildRetrievedReferenceJson(
                                                 software.amazon.awssdk.services.bedrockagentruntime.model.RetrievedReference ref) {
    JSONObject refData = new JSONObject();
    if (ref.content() != null && ref.content().text() != null) {
      refData.put(CONTENT, ref.content().text());
    }
    if (ref.location() != null) {
      refData.put(LOCATION, ref.location().toString());
    }
    if (ref.metadata() != null) {
      refData.put(METADATA, new JSONObject(ref.metadata()));
    }
    return refData;
  }

  private JSONObject buildInvokeAgentResult(String effectiveSessionId, String agentId, String agentAliasId,
                                            String prompt, List<JSONObject> chunks, long startTime) {
    JSONObject finalResult = new JSONObject();
    finalResult.put(SESSION_ID, effectiveSessionId);
    finalResult.put(AGENT_ID, agentId);
    finalResult.put(AGENT_ALIAS, agentAliasId);
    finalResult.put(PROMPT, prompt);
    finalResult.put(PROCESSED_AT, Instant.now().toString());
    finalResult.put(CHUNKS, new JSONArray(chunks));
    finalResult.put(SUMMARY, buildInvokeAgentSummary(chunks, startTime));
    return finalResult;
  }

  private JSONObject buildInvokeAgentSummary(List<JSONObject> chunks, long startTime) {
    JSONObject summary = new JSONObject();
    summary.put(TOTAL_CHUNKS, chunks.size());
    StringBuilder fullText = new StringBuilder();
    chunks.forEach(chunk -> {
      if (chunk.has(TEXT)) {
        fullText.append(chunk.getString(TEXT));
      }
    });
    summary.put(FULL_RESPONSE, fullText.toString());
    summary.put("total_duration_ms", System.currentTimeMillis() - startTime);
    return summary;
  }

  private static Duration toDuration(int amount, TimeUnit amountUnit) {
    if (amountUnit == null) {
      return Duration.ofSeconds(amount);
    }
    return Duration.ofMillis(amountUnit.toMillis(amount));
  }

  private Consumer<PromptCreationConfigurations.Builder> buildPromptConfigurations(boolean excludePreviousThinkingSteps,
                                                                                   Integer previousConversationTurnsToInclude) {
    return builder -> builder
        .excludePreviousThinkingSteps(excludePreviousThinkingSteps)
        .previousConversationTurnsToInclude(previousConversationTurnsToInclude);
  }

  private Consumer<BedrockModelConfigurations.Builder> buildModelConfigurations(boolean latencyOptimized) {
    return builder -> builder.performanceConfig(
                                                performanceConfig -> performanceConfig.latency(
                                                                                               latencyOptimized ? "optimized"
                                                                                                   : "standard"));

  }

  private static Consumer<SessionState.Builder> buildSessionState(
                                                                  java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> kbConfigs) {
    return sessionStateBuilder -> {
      if (kbConfigs == null || kbConfigs.isEmpty()) {
        return;
      }

      List<KnowledgeBaseConfiguration> sdkKbConfigs = kbConfigs.stream().map(kb -> {
        KnowledgeBaseVectorSearchConfiguration vectorCfg = buildVectorSearchConfiguration(kb.getNumberOfResults(),
                                                                                          kb.getOverrideSearchType(),
                                                                                          kb.getRetrievalMetadataFilterType(),
                                                                                          kb.getMetadataFilters(),
                                                                                          kb.getRerankingConfiguration());
        KnowledgeBaseConfiguration.Builder kbConfigBuilder = KnowledgeBaseConfiguration.builder()
            .knowledgeBaseId(kb.getKnowledgeBaseId());

        // Only add retrieval configuration if we have a vector search configuration
        if (vectorCfg != null) {
          KnowledgeBaseRetrievalConfiguration retrievalCfg = KnowledgeBaseRetrievalConfiguration.builder()
              .vectorSearchConfiguration(vectorCfg)
              .build();
          kbConfigBuilder.retrievalConfiguration(retrievalCfg);
        }

        return kbConfigBuilder.build();
      }).collect(Collectors.toList());

      if (!sdkKbConfigs.isEmpty()) {
        sessionStateBuilder.knowledgeBaseConfigurations(sdkKbConfigs);
      }
    };
  }


  private static KnowledgeBaseVectorSearchConfiguration buildVectorSearchConfiguration(Integer numberOfResults,
                                                                                       BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                                                                       BedrockAgentsFilteringParameters.RetrievalMetadataFilterType filterType,
                                                                                       Map<String, String> metadataFilters,
                                                                                       BedrockAgentsFilteringParameters.RerankingConfiguration rerankingConfig) {
    Map<String, String> nonEmptyFilters = getNonEmptyMetadataFilters(metadataFilters);
    if (!hasAnyVectorSearchConfig(numberOfResults, overrideSearchType, nonEmptyFilters, rerankingConfig)) {
      return null;
    }
    KnowledgeBaseVectorSearchConfiguration.Builder builder = KnowledgeBaseVectorSearchConfiguration.builder();
    applyFilterToBuilder(builder, nonEmptyFilters, filterType);
    applyNumberOfResults(builder, numberOfResults);
    applyOverrideSearchType(builder, overrideSearchType);
    applyRerankingConfig(builder, rerankingConfig);
    return builder.build();
  }

  private static Map<String, String> getNonEmptyMetadataFilters(Map<String, String> metadataFilters) {
    if (metadataFilters == null || metadataFilters.isEmpty()) {
      return null;
    }
    return metadataFilters.entrySet().stream()
        .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean hasAnyVectorSearchConfig(Integer numberOfResults,
                                                  BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                                  Map<String, String> nonEmptyFilters,
                                                  BedrockAgentsFilteringParameters.RerankingConfiguration rerankingConfig) {
    boolean hasNumberOfResults = numberOfResults != null && numberOfResults > 0;
    boolean hasOverrideSearchType = overrideSearchType != null;
    boolean hasFilters = nonEmptyFilters != null && !nonEmptyFilters.isEmpty();
    boolean hasRerankingConfig = rerankingConfig != null;
    return hasNumberOfResults || hasOverrideSearchType || hasFilters || hasRerankingConfig;
  }

  private static void applyFilterToBuilder(KnowledgeBaseVectorSearchConfiguration.Builder builder,
                                           Map<String, String> nonEmptyFilters,
                                           BedrockAgentsFilteringParameters.RetrievalMetadataFilterType filterType) {
    if (nonEmptyFilters == null || nonEmptyFilters.isEmpty()) {
      return;
    }
    RetrievalFilter filter = nonEmptyFilters.size() > 1
        ? buildCompositeRetrievalFilter(nonEmptyFilters, filterType)
        : buildSingleRetrievalFilter(nonEmptyFilters);
    builder.filter(filter);
  }

  private static RetrievalFilter buildSingleRetrievalFilter(Map<String, String> nonEmptyFilters) {
    Map.Entry<String, String> entry = nonEmptyFilters.entrySet().iterator().next();
    return RetrievalFilter.builder()
        .equalsValue(FilterAttribute.builder()
            .key(entry.getKey())
            .value(Document.fromString(entry.getValue()))
            .build())
        .build();
  }

  private static RetrievalFilter buildCompositeRetrievalFilter(Map<String, String> nonEmptyFilters,
                                                               BedrockAgentsFilteringParameters.RetrievalMetadataFilterType filterType) {
    List<RetrievalFilter> retrievalFilters = nonEmptyFilters.entrySet().stream()
        .map(entry -> RetrievalFilter.builder()
            .equalsValue(FilterAttribute.builder()
                .key(entry.getKey())
                .value(Document.fromString(entry.getValue()))
                .build())
            .build())
        .collect(Collectors.toList());
    return RetrievalFilter.builder()
        .applyMutation(filterBuilder -> {
          if (filterType == BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.AND_ALL) {
            filterBuilder.andAll(retrievalFilters);
          } else {
            filterBuilder.orAll(retrievalFilters);
          }
        })
        .build();
  }

  private static void applyNumberOfResults(KnowledgeBaseVectorSearchConfiguration.Builder builder, Integer numberOfResults) {
    if (numberOfResults != null && numberOfResults > 0) {
      builder.numberOfResults(numberOfResults);
    }
  }

  private static void applyOverrideSearchType(KnowledgeBaseVectorSearchConfiguration.Builder builder,
                                              BedrockAgentsFilteringParameters.SearchType overrideSearchType) {
    if (overrideSearchType != null) {
      builder.overrideSearchType(convertToSdkSearchType(overrideSearchType));
    }
  }

  private static void applyRerankingConfig(KnowledgeBaseVectorSearchConfiguration.Builder builder,
                                           BedrockAgentsFilteringParameters.RerankingConfiguration rerankingConfig) {
    if (rerankingConfig == null || rerankingConfig.getModelArn() == null
        || rerankingConfig.getModelArn().isEmpty()) {
      return;
    }
    builder.rerankingConfiguration(rerankingBuilder -> {
      String rerankingType = (rerankingConfig.getRerankingType() != null && !rerankingConfig.getRerankingType().isEmpty())
          ? rerankingConfig.getRerankingType()
          : "BEDROCK";
      rerankingBuilder.type(rerankingType);
      rerankingBuilder
          .bedrockRerankingConfiguration(bedrockBuilder -> configureBedrockReranking(bedrockBuilder, rerankingConfig));
    });
  }

  private static void configureBedrockReranking(
                                                VectorSearchBedrockRerankingConfiguration.Builder bedrockBuilder,
                                                BedrockAgentsFilteringParameters.RerankingConfiguration config) {
    bedrockBuilder.modelConfiguration(modelConfigBuilder -> {
      modelConfigBuilder.modelArn(config.getModelArn());
      if (config.getAdditionalModelRequestFields() != null && !config.getAdditionalModelRequestFields().isEmpty()) {
        Map<String, Document> additionalFields = config.getAdditionalModelRequestFields().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> Document.fromString(entry.getValue())));
        modelConfigBuilder.additionalModelRequestFields(additionalFields);
      }
    });
    applyMetadataRerankingConfiguration(bedrockBuilder, config);
    if (config.getNumberOfRerankedResults() != null) {
      bedrockBuilder.numberOfRerankedResults(config.getNumberOfRerankedResults());
    }
  }

  private static void applyMetadataRerankingConfiguration(
                                                          VectorSearchBedrockRerankingConfiguration.Builder bedrockBuilder,
                                                          BedrockAgentsFilteringParameters.RerankingConfiguration config) {
    if (config.getSelectionMode() == null) {
      return;
    }
    bedrockBuilder.metadataConfiguration(metadataConfigBuilder -> {
      metadataConfigBuilder.selectionMode(config.getSelectionMode().name());
      if (config.getSelectionMode() == BedrockAgentsFilteringParameters.RerankingSelectionMode.SELECTIVE) {
        metadataConfigBuilder.selectiveModeConfiguration(selectiveModeBuilder -> {
          if (config.getFieldsToExclude() != null && !config.getFieldsToExclude().isEmpty()) {
            List<FieldForReranking> fieldsToExclude = config.getFieldsToExclude().stream()
                .map(fieldName -> FieldForReranking.builder().fieldName(fieldName).build())
                .collect(Collectors.toList());
            selectiveModeBuilder.fieldsToExclude(fieldsToExclude);
          } else if (config.getFieldsToInclude() != null && !config.getFieldsToInclude().isEmpty()) {
            List<FieldForReranking> fieldsToInclude = config.getFieldsToInclude().stream()
                .map(fieldName -> FieldForReranking.builder().fieldName(fieldName).build())
                .collect(Collectors.toList());
            selectiveModeBuilder.fieldsToInclude(fieldsToInclude);
          }
        });
      }
    });
  }

  private static software.amazon.awssdk.services.bedrockagentruntime.model.SearchType convertToSdkSearchType(BedrockAgentsFilteringParameters.SearchType connectorSearchType) {
    if (connectorSearchType == null) {
      return null;
    }
    switch (connectorSearchType) {
      case HYBRID:
        return software.amazon.awssdk.services.bedrockagentruntime.model.SearchType.HYBRID;
      case SEMANTIC:
        return software.amazon.awssdk.services.bedrockagentruntime.model.SearchType.SEMANTIC;
      default:
        // Fail fast: return the error back by throwing an exception so callers can handle it
        throw new IllegalArgumentException("Unsupported SearchType: " + connectorSearchType);
    }
  }

  @Override
  public InputStream chatWithAgentSSEStream(String agentId, String agentAliasId,
                                            String prompt, boolean enableTrace,
                                            boolean latencyOptimized, BedrockAgentsSessionParameters bedrockSessionParameters,
                                            BedrockAgentsFilteringParameters bedrockAgentsFilteringParameters,
                                            BedrockAgentsMultipleFilteringParameters bedrockAgentsMultipleFilteringParameters,
                                            BedrockAgentsResponseParameters bedrockAgentsResponseParameters,
                                            BedrockAgentsResponseLoggingParameters bedrockAgentsResponseLoggingParameters) {

    Integer operationTimeout = bedrockAgentsResponseParameters.getRequestTimeout();
    TimeUnit operationTimeoutUnit = bedrockAgentsResponseParameters.getRequestTimeoutUnit();
    String sessionId = bedrockSessionParameters.getSessionId();
    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
    logger.debug("Using sessionId: {}", effectiveSessionId);
    logger.info("Agent operation [AGENT-chat-streaming-SSE] session-start sessionId={} agentId={} agentAliasId={}",
                effectiveSessionId, agentId, agentAliasId);

    // Create retry configuration from response parameters
    StreamingRetryUtility.RetryConfig retryConfig = new StreamingRetryUtility.RetryConfig(
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getMaxRetries() != null
                                                                                                  ? bedrockAgentsResponseParameters
                                                                                                      .getMaxRetries()
                                                                                                  : 3,
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getRetryBackoffMs() != null
                                                                                                  ? bedrockAgentsResponseParameters
                                                                                                      .getRetryBackoffMs()
                                                                                                  : 1000L,
                                                                                          bedrockAgentsResponseParameters
                                                                                              .getEnableRetry());

    String requestId = bedrockAgentsResponseLoggingParameters != null
        ? bedrockAgentsResponseLoggingParameters.getRequestId()
        : null;
    String correlationId = bedrockAgentsResponseLoggingParameters != null
        ? bedrockAgentsResponseLoggingParameters.getCorrelationId()
        : null;
    String userId = bedrockAgentsResponseLoggingParameters != null
        ? bedrockAgentsResponseLoggingParameters.getUserId()
        : null;

    return invokeAgentSSEStream(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                                bedrockSessionParameters.getExcludePreviousThinkingSteps(),
                                bedrockSessionParameters.getPreviousConversationTurnsToInclude(),
                                buildKnowledgeBaseConfigs(bedrockAgentsFilteringParameters,
                                                          bedrockAgentsMultipleFilteringParameters),
                                retryConfig,
                                requestId, correlationId, userId, operationTimeout, operationTimeoutUnit

    );

  }

  /**
   * Invokes Bedrock Agent and returns streaming SSE response as InputStream.
   *
   * This method is designed to work with Mule's binary streaming.
   **/
  private InputStream invokeAgentSSEStream(String agentAliasId, String agentId,
                                           String prompt, boolean enableTrace,
                                           boolean latencyOptimized, String effectiveSessionId,
                                           boolean excludePreviousThinkingSteps,
                                           Integer previousConversationTurnsToInclude,
                                           java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                           StreamingRetryUtility.RetryConfig retryConfig,
                                           String requestId, String correlationId, String userId,
                                           Integer operationTimeout, TimeUnit operationTimeoutUnit) {
    long requestStartTime = System.currentTimeMillis();
    logger
        .debug("SSE streaming request received - agentId: {}, agentAlias: {}, sessionId: {}, requestId: {}, correlationId: {}, promptLength: {}",
               agentId, agentAliasId, effectiveSessionId, requestId, correlationId, prompt != null ? prompt.length() : 0);

    PipedOutputStream outputStream = null;
    try {
      // Create piped streams for real-time streaming
      outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream);

      // Track if chunks have been received (for retry logic)
      AtomicBoolean chunksReceived = new AtomicBoolean(false);
      // Track if session-start has been sent (for consistency - always send before error if not already sent)
      AtomicBoolean sessionStartSent = new AtomicBoolean(false);
      // Track if client disconnected - used to stop processing new chunks and release thread
      AtomicBoolean clientDisconnected = new AtomicBoolean(false);

      // Bounded queue prevents unbounded memory growth if the writer thread is slow (e.g. slow client).
      // 1000 is generous for SSE events; offer() returns false if full, which we log and drop to avoid OOM.
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1000);

      final PipedOutputStream finalOut = outputStream;

      // Use Mule's managed IO scheduler for streaming operations instead of a manual ThreadPoolExecutor.
      // The IO scheduler is designed for blocking I/O tasks and is lifecycle-managed by the Mule runtime,
      // ensuring graceful shutdown on undeploy (no daemon thread abrupt termination, no classloader leaks).
      Scheduler streamingScheduler = getConfig().getStreamingScheduler();

      Future<?> writerFuture = submitToScheduler(
                                                 () -> runWriterTask(writeQueue, finalOut,
                                                                     clientDisconnected, agentId,
                                                                     effectiveSessionId, requestId),
                                                 streamingScheduler);

      // Start the streaming process asynchronously on the Mule IO scheduler
      // (NOT on ForkJoinPool.commonPool() which has ~CPU-cores threads and would starve under blocking retries)
      submitToScheduler(
                        () -> runProducerTask(agentAliasId, agentId, prompt, enableTrace, latencyOptimized,
                                              effectiveSessionId,
                                              excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                                              knowledgeBaseConfigs,
                                              finalOut, retryConfig, chunksReceived, sessionStartSent, requestId,
                                              correlationId, userId,
                                              operationTimeout, operationTimeoutUnit, writeQueue,
                                              clientDisconnected,
                                              requestStartTime, writerFuture),
                        streamingScheduler);
      return inputStream;

    } catch (IOException e) {
      // Return error as immediate SSE event
      logger.debug("Failed to initialize SSE streaming - agentId: {}, sessionId: {}, requestId: {}, error: {}, elapsedMs: {}",
                   agentId, effectiveSessionId, requestId, e.getMessage(), System.currentTimeMillis() - requestStartTime);
      String errorEvent = formatSSEEvent(ERROR_KEY, createErrorJson(e).toString());
      logger.error(errorEvent);
      if (outputStream != null) {
        closeQuietly(outputStream);
      }
      logger
          .info("Agent operation [AGENT-chat-streaming-SSE] session-end sessionId={} agentId={} durationMs={} status=init-failed",
                effectiveSessionId, agentId, System.currentTimeMillis() - requestStartTime);
      return new ByteArrayInputStream(errorEvent.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void closeQuietly(PipedOutputStream os) {
    try {
      if (os != null)
        os.close();
    } catch (IOException e) {
      logger.debug("Error closing stream", e);
    }
  }

  /**
   * Writer task: drains SSE events from the queue and writes them to the piped output stream. Runs until it receives the
   * {@link #END_OF_STREAM_SENTINEL}. On client disconnect (IOException), sets the clientDisconnected flag and drains the queue to
   * unblock producers.
   */
  private void runWriterTask(BlockingQueue<String> writeQueue, PipedOutputStream outputStream,
                             AtomicBoolean clientDisconnected, String agentId, String effectiveSessionId, String requestId) {
    logger.debug("Writer thread started - agentId: {}, sessionId: {}, requestId: {}", agentId,
                 effectiveSessionId, requestId);
    try {
      while (true) {
        String event = writeQueue.take();
        if (END_OF_STREAM_SENTINEL.equals(event)) {
          break;
        }
        outputStream.write(event.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      }
      logger.debug("Writer thread completed - agentId: {}, sessionId: {}, requestId: {}", agentId,
                   effectiveSessionId, requestId);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.debug("Writer thread interrupted - agentId: {}, sessionId: {}, requestId: {}", agentId, effectiveSessionId,
                   requestId);
    } catch (IOException e) {
      clientDisconnected.set(true);
      logger
          .error("Writer thread IOException (client disconnect) - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                 agentId, effectiveSessionId, requestId, e.getMessage());
      // Drain remaining queue items to prevent blocking producers
      int drained = 0;
      while (writeQueue.poll() != null) {
        drained++;
      }
      if (drained > 0) {
        logger.warn("Drained {} queued events after IOException - agentId: {}, sessionId: {}, requestId: {}",
                    drained, agentId, effectiveSessionId, requestId);
      }
    } finally {
      try {
        outputStream.close();
      } catch (IOException e) {
        logger.debug("Could not close output stream: {}", e.getMessage());
      }
    }
  }

  /**
   * Producer task: invokes Bedrock with retry logic, queues SSE events for the writer, and ensures the stream terminates cleanly.
   * On failure, sends a session-start (if not yet sent) and error event before the end-of-stream sentinel. Always waits for the
   * writer future to complete before returning.
   */
  private void runProducerTask(String agentAliasId, String agentId, String prompt, boolean enableTrace,
                               boolean latencyOptimized, String effectiveSessionId,
                               boolean excludePreviousThinkingSteps, Integer previousConversationTurnsToInclude,
                               java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                               PipedOutputStream outputStream, StreamingRetryUtility.RetryConfig retryConfig,
                               AtomicBoolean chunksReceived, AtomicBoolean sessionStartSent,
                               String requestId, String correlationId, String userId,
                               Integer operationTimeout, TimeUnit operationTimeoutUnit,
                               BlockingQueue<String> writeQueue, AtomicBoolean clientDisconnected,
                               long requestStartTime, Future<?> writerFuture) {
    try {
      streamBedrockResponseWithRetry(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                                     excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                                     knowledgeBaseConfigs,
                                     outputStream, retryConfig, chunksReceived, sessionStartSent, requestId, correlationId,
                                     userId,
                                     operationTimeout, operationTimeoutUnit, writeQueue, clientDisconnected);
      logger.debug("Streaming completed - agentId: {}, sessionId: {}, requestId: {}, elapsedMs: {}",
                   agentId, effectiveSessionId, requestId, System.currentTimeMillis() - requestStartTime);
    } catch (ModuleException e) {
      logger.error("Async streaming operation failed - agentId: {}, sessionId: {}, requestId: {}, error: {}, elapsedMs: {}",
                   agentId, effectiveSessionId, requestId, e.getMessage(), System.currentTimeMillis() - requestStartTime);
      try {
        queueStreamFailureEvents(e, agentAliasId, agentId, prompt, effectiveSessionId, requestId, correlationId, userId,
                                 sessionStartSent, writeQueue);
      } catch (IOException ioe) {
        logger.error("Failed to queue failure events - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                     agentId, effectiveSessionId, requestId, ioe.getMessage());
      }
      if (!forceQueueSentinel(writeQueue, agentId, effectiveSessionId, requestId)) {
        throw new ModuleException(
                                  "Failed to queue end-of-stream sentinel - stream will not terminate cleanly. agentId: "
                                      + agentId
                                      + ", sessionId: " + effectiveSessionId + ", requestId: " + requestId,
                                  BedrockErrorType.SERVICE_ERROR);
      }
    } finally {
      awaitWriterCompletion(writerFuture, agentId, effectiveSessionId, requestId);
      logger.info("Agent operation [AGENT-chat-streaming-SSE] session-end sessionId={} agentId={} durationMs={}",
                  effectiveSessionId, agentId, System.currentTimeMillis() - requestStartTime);
    }
  }

  /**
   * Queues session-start (if not yet sent) and error SSE events to the write queue on streaming failure. Ensures the client
   * receives a consistent event sequence even when the Bedrock call fails.
   */
  private void queueStreamFailureEvents(Exception e, String agentAliasId, String agentId, String prompt,
                                        String effectiveSessionId, String requestId, String correlationId, String userId,
                                        AtomicBoolean sessionStartSent, BlockingQueue<String> writeQueue)
      throws IOException {
    if (sessionStartSent.compareAndSet(false, true)) {
      writeSessionStartEvent(agentAliasId, agentId, prompt, effectiveSessionId, requestId, correlationId, userId,
                             -1, writeQueue);
    }

    JSONObject errorJson = createErrorJson(e);
    String errorEvent = formatSSEEvent(ERROR_KEY, errorJson.toString());
    if (!writeQueue.offer(errorEvent)) {
      logger.warn("Queue full, dropping error event - agentId: {}, sessionId: {}, requestId: {}",
                  agentId, effectiveSessionId, requestId);
    }
    logger.error("Streaming error - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                 agentId, effectiveSessionId, requestId, e.getMessage());
  }

  /**
   * Forces the end-of-stream sentinel onto the write queue. If the queue is full, drops one item to make room. The sentinel is
   * critical — without it the writer thread blocks forever on {@code writeQueue.take()}.
   *
   * @return {@code true} if the sentinel was successfully queued, {@code false} if it could not be queued even after draining
   */
  private boolean forceQueueSentinel(BlockingQueue<String> writeQueue, String agentId, String effectiveSessionId,
                                     String requestId) {
    if (writeQueue.offer(END_OF_STREAM_SENTINEL)) {
      return true;
    }
    writeQueue.poll();
    if (writeQueue.offer(END_OF_STREAM_SENTINEL)) {
      return true;
    }
    logger.error("Failed to queue end-of-stream sentinel after drain - agentId: {}, sessionId: {}, requestId: {}",
                 agentId, effectiveSessionId, requestId);
    return false;
  }

  /**
   * Waits for the writer future to complete within 5 seconds. Ensures the writer thread has flushed all data and closed the
   * output stream before the producer thread exits.
   */
  private void awaitWriterCompletion(Future<?> writerFuture, String agentId, String effectiveSessionId,
                                     String requestId) {
    try {
      writerFuture.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while waiting for writer thread - agentId: {}, sessionId: {}, requestId: {}",
                  agentId, effectiveSessionId, requestId);
    } catch (java.util.concurrent.TimeoutException te) {
      logger.warn("Writer thread did not complete within 5s - agentId: {}, sessionId: {}, requestId: {}",
                  agentId, effectiveSessionId, requestId);
    } catch (Exception writerException) {
      logger.error("Writer thread failed - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                   agentId, effectiveSessionId, requestId, writerException.getMessage());
    }
  }

  /**
   * Wrapper method that adds retry logic around streamBedrockResponse. Only retries if no chunks have been received yet. Added
   * writeQueue and clientDisconnected parameters to pass through to streamBedrockResponse
   */
  private void streamBedrockResponseWithRetry(String agentAliasId, String agentId, String prompt,
                                              boolean enableTrace, boolean latencyOptimized,
                                              String effectiveSessionId, boolean excludePreviousThinkingSteps,
                                              Integer previousConversationTurnsToInclude,
                                              java.util.List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                              PipedOutputStream outputStream, StreamingRetryUtility.RetryConfig retryConfig,
                                              AtomicBoolean chunksReceived,
                                              AtomicBoolean sessionStartSent,
                                              String requestId, String correlationId, String userId,
                                              Integer operationTimeout, TimeUnit operationTimeoutUnit,
                                              BlockingQueue<String> writeQueue, AtomicBoolean clientDisconnected) {
    StreamingRetryUtility.StreamingOperation operation = () -> {
      streamBedrockResponse(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                            excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                            knowledgeBaseConfigs, outputStream, chunksReceived, sessionStartSent, requestId,
                            correlationId, userId, operationTimeout, operationTimeoutUnit, writeQueue, clientDisconnected);
    };

    StreamingRetryUtility.RetryResult result =
        StreamingRetryUtility.executeWithRetry(operation, retryConfig, chunksReceived, agentId, effectiveSessionId, requestId);
    if (!result.isSuccess()) {
      // Create enhanced error message with retry information
      String errorMessage = StreamingRetryUtility.createRetryErrorMessage(
                                                                          result.getLastException().getMessage() != null
                                                                              ? result.getLastException().getMessage()
                                                                              : result.getLastException().getClass()
                                                                                  .getSimpleName(),
                                                                          result.getAttemptsMade(),
                                                                          retryConfig.getMaxRetries(),
                                                                          result.isChunksReceived());

      throw new ModuleException(errorMessage, BedrockErrorType.CLIENT_ERROR, result.getLastException());
    }
  }

  /**
   * invokeAgentSSEStream and passed through to avoid race conditions
   */
  private void streamBedrockResponse(String agentAliasId, String agentId, String prompt,
                                     boolean enableTrace, boolean latencyOptimized, String effectiveSessionId,
                                     boolean excludePreviousThinkingSteps, Integer previousConversationTurnsToInclude,
                                     List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                     PipedOutputStream outputStream, AtomicBoolean chunksReceived,
                                     AtomicBoolean sessionStartSent, String requestId,
                                     String correlationId, String userId,
                                     Integer operationTimeout, TimeUnit operationTimeoutUnit,
                                     BlockingQueue<String> writeQueue, AtomicBoolean clientDisconnected)
      throws ExecutionException, InterruptedException, IOException {

    long startTime = System.currentTimeMillis();

    // Track chunk timing and count
    AtomicInteger chunkCount = new AtomicInteger(0);
    AtomicLong lastChunkTime = new AtomicLong(startTime);
    AtomicLong timeToFirstChunk = new AtomicLong(-1);

    InvokeAgentRequest request = buildStreamingInvokeAgentRequest(agentAliasId, agentId, prompt, enableTrace,
                                                                  latencyOptimized, effectiveSessionId,
                                                                  excludePreviousThinkingSteps,
                                                                  previousConversationTurnsToInclude,
                                                                  knowledgeBaseConfigs, operationTimeout, operationTimeoutUnit);

    InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
        .onChunk(chunk -> handleStreamChunk(agentAliasId, agentId, prompt, effectiveSessionId, requestId, correlationId,
                                            userId, chunksReceived, sessionStartSent, startTime, writeQueue, chunk,
                                            chunkCount, lastChunkTime, timeToFirstChunk, clientDisconnected))
        .build();

    InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
        .subscriber(visitor)
        .onComplete(() -> handleStreamComplete(effectiveSessionId, agentId, agentAliasId, startTime, requestId,
                                               correlationId, userId, chunkCount, timeToFirstChunk, chunksReceived, writeQueue,
                                               clientDisconnected))
        .build();

    long effectiveTimeoutMs = (operationTimeout != null && operationTimeout > 0)
        ? toDuration(operationTimeout, operationTimeoutUnit).toMillis()
        : getConnection().getConnectionTimeoutMs();
    CompletableFuture<Void> invocationFuture = getConnection().invokeAgent(request, handler, effectiveTimeoutMs);

    // Use whenComplete callback to handle client disconnect instead of polling.
    // When client disconnects (writer IOException), this cancels the Bedrock invocation.
    invocationFuture.whenComplete((result, throwable) -> {
      if (throwable == null && clientDisconnected.get()) {
        logger.debug("Bedrock invocation completed but client already disconnected - agentId: {}, sessionId: {}, requestId: {}",
                     agentId, effectiveSessionId, requestId);
      }
    });

    try {
      // Block until Bedrock invocation completes (or fails/times out).
      // Client disconnect is handled by the writer thread setting clientDisconnected=true,
      // which causes handleStreamChunk to skip queuing and handleStreamComplete to send sentinel.
      invocationFuture.get();
    } catch (ExecutionException | InterruptedException e) {
      // If client disconnected during invocation, no need to propagate the error
      if (clientDisconnected.get()) {
        logger.debug("Client disconnected, suppressing Bedrock error - agentId: {}, sessionId: {}, requestId: {}",
                     agentId, effectiveSessionId, requestId);
        if (!forceQueueSentinel(writeQueue, agentId, effectiveSessionId, requestId)) {
          throw new ModuleException(
                                    "Failed to queue end-of-stream sentinel after client disconnect. agentId: " + agentId
                                        + ", sessionId: " + effectiveSessionId + ", requestId: " + requestId,
                                    BedrockErrorType.SERVICE_ERROR);
        }
        return;
      }
      throw e;
    }
  }

  private InvokeAgentRequest buildStreamingInvokeAgentRequest(String agentAliasId, String agentId, String prompt,
                                                              boolean enableTrace, boolean latencyOptimized,
                                                              String effectiveSessionId, boolean excludePreviousThinkingSteps,
                                                              Integer previousConversationTurnsToInclude,
                                                              List<BedrockAgentsFilteringParameters.KnowledgeBaseConfig> knowledgeBaseConfigs,
                                                              Integer operationTimeout, TimeUnit operationTimeoutUnit) {
    InvokeAgentRequest.Builder requestBuilder = InvokeAgentRequest.builder()
        .agentId(agentId)
        .agentAliasId(agentAliasId)
        .sessionId(effectiveSessionId)
        .inputText(prompt)
        .streamingConfigurations(builder -> builder.streamFinalResponse(true))
        .enableTrace(enableTrace)
        .sessionState(buildSessionState(knowledgeBaseConfigs))
        .bedrockModelConfigurations(buildModelConfigurations(latencyOptimized))
        .promptCreationConfigurations(buildPromptConfigurations(excludePreviousThinkingSteps,
                                                                previousConversationTurnsToInclude));
    if (operationTimeout != null && operationTimeout > 0) {
      Duration timeout = toDuration(operationTimeout, operationTimeoutUnit);
      requestBuilder.overrideConfiguration(
                                           AwsRequestOverrideConfiguration.builder().apiCallTimeout(timeout).build());
    }
    return requestBuilder.build();
  }

  private void handleStreamChunk(String agentAliasId, String agentId, String prompt, String effectiveSessionId,
                                 String requestId, String correlationId, String userId,
                                 AtomicBoolean chunksReceived, AtomicBoolean sessionStartSent, long startTime,
                                 BlockingQueue<String> writeQueue, PayloadPart chunk,
                                 AtomicInteger chunkCount, AtomicLong lastChunkTime, AtomicLong timeToFirstChunk,
                                 AtomicBoolean clientDisconnected) {
    // IMPORTANT: This callback runs on Netty's event loop thread.
    // We must NOT block here, so we queue the data for the writer thread.
    // Skip processing if client already disconnected - prevents queue growth
    if (clientDisconnected.get()) {
      return;
    }
    try {
      chunksReceived.set(true);
      // Track chunk timing
      long currentTime = System.currentTimeMillis();
      int currentChunkCount = chunkCount.incrementAndGet();
      long timeSinceLastChunk = currentTime - lastChunkTime.getAndSet(currentTime);

      if (sessionStartSent.compareAndSet(false, true)) {
        long firstChunkTime = currentTime - startTime;
        timeToFirstChunk.set(firstChunkTime);
        logger.debug("First chunk received - agentId: {}, sessionId: {}, requestId: {}, timeToFirstChunkMs: {}",
                     agentId, effectiveSessionId, requestId, firstChunkTime);
        writeSessionStartEvent(agentAliasId, agentId, prompt, effectiveSessionId, requestId, correlationId, userId,
                               firstChunkTime, writeQueue);
      } else if (timeSinceLastChunk > 1000) {
        logger.debug("Slow chunk gap - agentId: {}, sessionId: {}, requestId: {}, chunkNumber: {}, gapMs: {}",
                     agentId, effectiveSessionId, requestId, currentChunkCount, timeSinceLastChunk);
      }
      JSONObject chunkData = createChunkJson(chunk);
      String sseEvent = formatSSEEvent(CHUNK, chunkData.toString());
      boolean queued = writeQueue.offer(sseEvent);
      if (!queued) {
        logger.warn("Queue full, dropping chunk event - agentId: {}, sessionId: {}, requestId: {}, chunkNumber: {}",
                    agentId, effectiveSessionId, requestId, currentChunkCount);
      }
    } catch (IOException e) {
      writeChunkErrorEvent(e, writeQueue);
    }
  }

  private void writeSessionStartEvent(String agentAliasId, String agentId, String prompt, String effectiveSessionId,
                                      String requestId, String correlationId, String userId,
                                      long firstChunkTime, BlockingQueue<String> writeQueue)
      throws IOException {
    JSONObject startEvent = createSessionStartJson(agentAliasId, agentId, prompt, effectiveSessionId,
                                                   Instant.now().toString(), requestId, correlationId, userId, firstChunkTime);
    String sseStart = formatSSEEvent("session-start", startEvent.toString());
    if (!writeQueue.offer(sseStart)) {
      throw new IOException(
                            "Failed to queue session-start event - agentId: " + agentId
                                + ", sessionId: " + effectiveSessionId + ", requestId: " + requestId);
    }
  }

  private void writeChunkErrorEvent(IOException e, BlockingQueue<String> writeQueue) {
    String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
    if (!writeQueue.offer(errorEvent)) {
      logger.warn("Queue full, dropping chunk-error event");
    }
    logger.error("Error processing chunk: {}", e.getMessage());
  }

  private void handleStreamComplete(String effectiveSessionId, String agentId, String agentAliasId, long startTime,
                                    String requestId, String correlationId, String userId,
                                    AtomicInteger chunkCount, AtomicLong timeToFirstChunk,
                                    AtomicBoolean chunksReceived, BlockingQueue<String> writeQueue,
                                    AtomicBoolean clientDisconnected) {
    // IMPORTANT: This callback runs on Netty's event loop thread.
    // We must NOT block here, so we queue the data for the writer thread.
    // Skip completion event if client already disconnected
    if (clientDisconnected.get()) {
      logger.debug("Skipping completion - client disconnected - agentId: {}, sessionId: {}, requestId: {}",
                   agentId, effectiveSessionId, requestId);
      forceQueueSentinel(writeQueue, agentId, effectiveSessionId, requestId);
      return;
    }
    try {
      long totalDuration = System.currentTimeMillis() - startTime;
      int finalChunkCount = chunkCount.get();
      long timeToFirstChunkMs = timeToFirstChunk.get();

      JSONObject completionData = createCompletionJson(effectiveSessionId, agentId, agentAliasId, totalDuration,
                                                       requestId, correlationId, userId,
                                                       timeToFirstChunkMs >= 0 ? timeToFirstChunkMs : -1, finalChunkCount);
      String completionEvent = formatSSEEvent("session-complete", completionData.toString());
      if (!writeQueue.offer(completionEvent)) {
        logger.warn("Queue full, dropping completion event - agentId: {}, sessionId: {}, requestId: {}",
                    agentId, effectiveSessionId, requestId);
      }
    } catch (Exception e) {
      logger.error("Error creating completion event - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                   agentId, effectiveSessionId, requestId, e.getMessage());
      String errorEvent = formatSSEEvent("completion-error", createErrorJson(e).toString());
      if (!writeQueue.offer(errorEvent)) {
        logger.warn("Queue full, dropping completion-error event - agentId: {}, sessionId: {}, requestId: {}",
                    agentId, effectiveSessionId, requestId);
      }
    } finally {
      forceQueueSentinel(writeQueue, agentId, effectiveSessionId, requestId);
    }
  }

  private static JSONObject createCompletionJson(String sessionId, String agentId, String agentAlias, long duration,
                                                 String requestId, String correlationId, String userId, long timeToFirstChunkMs,
                                                 int totalChunks) {
    JSONObject completionData = new JSONObject();
    completionData.put(SESSION_ID, sessionId);
    completionData.put(AGENT_ID, agentId);
    completionData.put(AGENT_ALIAS, agentAlias);
    completionData.put("status", "completed");
    completionData.put("total_duration_ms", duration);
    completionData.put("timeToFirstChunkMs", timeToFirstChunkMs);
    completionData.put("totalChunks", totalChunks);
    completionData.put(TIMESTAMP, Instant.now().toString());
    if (requestId != null && !requestId.isEmpty()) {
      completionData.put("requestId", requestId);
    }
    if (correlationId != null && !correlationId.isEmpty()) {
      completionData.put("correlationId", correlationId);
    }
    if (userId != null && !userId.isEmpty()) {
      completionData.put("userId", userId);
    }
    return completionData;
  }

  private static JSONObject createErrorJson(Throwable error) {
    JSONObject errorData = new JSONObject();
    errorData.put(ERROR_KEY, error.getMessage());
    errorData.put("type", error.getClass().getSimpleName());
    errorData.put(TIMESTAMP, Instant.now().toString());
    return errorData;
  }

  private JSONObject createChunkJson(PayloadPart chunk) {
    JSONObject chunkData = new JSONObject();
    chunkData.put(TYPE, CHUNK);
    chunkData.put(TIMESTAMP, Instant.now().toString());

    try {
      if (chunk.bytes() != null) {
        String text = new String(chunk.bytes().asByteArray(), StandardCharsets.UTF_8);
        chunkData.put(TEXT, text);
      }

    } catch (Exception e) {
      chunkData.put(ERROR_KEY, "Error processing chunk: " + e.getMessage());
    }

    return chunkData;
  }

  private String formatSSEEvent(String eventType, String data) {
    int eventId = eventCounter.incrementAndGet();
    return String.format("id: %d%nevent: %s%ndata: %s%n%n", eventId, eventType, data);
  }

  private static JSONObject createSessionStartJson(String agentAlias, String agentId, String prompt,
                                                   String sessionId, String timestamp, String requestId, String correlationId,
                                                   String userId, long timeToFirstChunkMs) {
    JSONObject startData = new JSONObject();
    startData.put(SESSION_ID, sessionId);
    startData.put(AGENT_ID, agentId);
    startData.put(AGENT_ALIAS, agentAlias);
    startData.put(PROMPT, prompt);
    startData.put(PROCESSED_AT, timestamp);
    startData.put("status", "started");
    startData.put("timeToFirstChunkMs", timeToFirstChunkMs);
    if (requestId != null && !requestId.isEmpty()) {
      startData.put("requestId", requestId);
    }
    if (correlationId != null && !correlationId.isEmpty()) {
      startData.put("correlationId", correlationId);
    }
    if (userId != null && !userId.isEmpty()) {
      startData.put("userId", userId);
    }
    return startData;
  }

  /**
   * Submits a task to the Mule IO scheduler via {@link Scheduler#submit(Runnable)}. If the scheduler rejects the task (e.g. all
   * concurrent slots are occupied), the rejection propagates as a {@link ModuleException} so the caller can return a clean error
   * to the client. Running on the caller thread is not safe here because the writer and producer tasks have a producer-consumer
   * dependency that would deadlock if both ran on the same thread.
   */
  private static Future<?> submitToScheduler(Runnable task, Scheduler scheduler) {
    try {
      return scheduler.submit(task);
    } catch (RejectedExecutionException e) {
      throw new ModuleException(
                                "Streaming scheduler capacity exceeded - all concurrent slots are occupied. "
                                    + "Consider increasing the scheduler pool size or reducing concurrent streaming requests.",
                                BedrockErrorType.SERVICE_ERROR, e);
    }
  }
}
