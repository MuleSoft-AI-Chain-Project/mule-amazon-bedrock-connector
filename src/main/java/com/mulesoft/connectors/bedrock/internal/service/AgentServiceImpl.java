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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

  /**
   * Maximum number of threads in the streaming pool. Each SSE streaming request uses up to 2 threads (one for the outer async
   * orchestration, one for the queue-draining writer). This cap prevents runaway thread creation under high concurrency while
   * still allowing substantial parallelism. Excess tasks are rejected with {@link ThreadPoolExecutor.CallerRunsPolicy} so that
   * backpressure is applied to the calling thread instead of silently dropping work.
   */
  private static final int STREAMING_MAX_POOL_SIZE = 200;

  /**
   * Dedicated, bounded thread pool for streaming operations with retry support.
   *
   * IMPORTANT: We use a dedicated pool instead of ForkJoinPool.commonPool() because: 1. Retry logic can block threads for
   * extended periods (multiple attempts + backoff delays) 2. ForkJoinPool.commonPool() has limited threads (~CPU cores - 1) 3. If
   * all ForkJoinPool threads are blocked in retry loops, new requests cannot start 4. This causes thread starvation, leading to
   * connection pool timeouts for new requests
   *
   * The pool is bounded to {@link #STREAMING_MAX_POOL_SIZE} threads to prevent OS thread exhaustion. Idle threads are reaped
   * after 60 seconds. A {@link ThreadPoolExecutor.CallerRunsPolicy} rejection policy applies backpressure by running excess tasks
   * on the submitting thread rather than throwing or silently discarding.
   */
  private static final ExecutorService STREAMING_EXECUTOR = createStreamingExecutor();

  private static ExecutorService createStreamingExecutor() {
    AtomicInteger threadCounter = new AtomicInteger(0);
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
                                                         0, // core pool size: no permanent threads, scale to zero when idle
                                                         STREAMING_MAX_POOL_SIZE,
                                                         60L, TimeUnit.SECONDS, // idle threads reaped after 60s
                                                         new SynchronousQueue<>(), // direct hand-off: forces new thread creation
                                                                                   // up to max
                                                         r -> {
                                                           Thread t =
                                                               new Thread(r,
                                                                          "bedrock-streaming-" + threadCounter.incrementAndGet());
                                                           t.setDaemon(true); // Don't prevent JVM shutdown
                                                           return t;
                                                         },
                                                         new ThreadPoolExecutor.CallerRunsPolicy() // backpressure: caller
                                                                                                   // executes the task if pool is
                                                                                                   // full
    );
    // Register a shutdown hook for orderly cleanup when the JVM exits
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.debug("Shutting down bedrock streaming executor");
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.warn("Streaming executor did not terminate within 5s, forcing shutdown");
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }, "bedrock-streaming-shutdown"));
    return executor;
  }

  public AgentServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                                     BedrockParameters bedrockParameters) {
    try {
      String finalPromptTemplate = PromptPayloadHelper.definePromptTemplate(promptTemplate, instructions, dataset);
      String nativeRequest = PromptPayloadHelper.identifyPayload(finalPromptTemplate, bedrockParameters);
      String region = getConnection().getRegion();
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, region, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters.getModelName());
    }

  }

  @Override
  public String listAgents() {
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
  }

  @Override
  public String getAgentById(String agentId) {
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
    logger.info("Using sessionId: {}", effectiveSessionId);

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
    logger.info("Using sessionId: {}", effectiveSessionId);

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

      // This prevents the race condition where error handler writes directly to pipe after it's closed
      BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();

      final PipedOutputStream finalOut = outputStream;

      // This ensures the writer thread is always running and can handle error events queued from catch block
      logger.debug("Starting writer thread - agentId: {}, sessionId: {}, requestId: {}", agentId, effectiveSessionId, requestId);
      CompletableFuture<Void> writerFuture = CompletableFuture.runAsync(() -> {
        logger.debug("Writer thread STARTED and waiting for events - agentId: {}, sessionId: {}, requestId: {}", agentId,
                     effectiveSessionId, requestId);
        try {
          while (true) {
            int queueSizeBefore = writeQueue.size();
            logger
                .debug("Writer thread blocking on writeQueue.take() - agentId: {}, sessionId: {}, requestId: {}, currentQueueSize: {}",
                       agentId, effectiveSessionId, requestId, queueSizeBefore);
            String event = writeQueue.take(); // Blocks until an event is available
            int queueSizeAfter = writeQueue.size();
            logger
                .debug("Writer thread received event from queue - agentId: {}, sessionId: {}, requestId: {}, queueSizeAfter: {}",
                       agentId, effectiveSessionId, requestId, queueSizeAfter);
            if (queueSizeBefore > 25) {
              logger
                  .debug("Writer thread consumed event from queue - agentId: {}, sessionId: {}, requestId: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                         agentId, effectiveSessionId, requestId, queueSizeBefore, queueSizeAfter);
            }
            if (END_OF_STREAM_SENTINEL.equals(event)) {
              logger.debug("Writer thread received end-of-stream signal - agentId: {}, sessionId: {}, requestId: {}",
                           agentId, effectiveSessionId, requestId);
              break; // End of stream
            }
            logger.debug("Writer thread attempting to write event - agentId: {}, sessionId: {}, requestId: {}, eventLength: {}",
                         agentId, effectiveSessionId, requestId, event.length());
            finalOut.write(event.getBytes(StandardCharsets.UTF_8));
            finalOut.flush();
            logger.debug("Writer thread successfully wrote event - agentId: {}, sessionId: {}, requestId: {}",
                         agentId, effectiveSessionId, requestId);
          }
          logger.debug("Writer thread completed normally - agentId: {}, sessionId: {}, requestId: {}", agentId,
                       effectiveSessionId,
                       requestId);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          logger.debug("Writer thread interrupted - agentId: {}, sessionId: {}, requestId: {}", agentId, effectiveSessionId,
                       requestId);
        } catch (IOException e) {
          clientDisconnected.set(true);
          logger
              .error("Writer thread IOException during write - agentId: {}, sessionId: {}, requestId: {}, correlationId: {}, error: {}, errorType: {}. Stopping chunk processing.",
                     agentId, effectiveSessionId, requestId, correlationId, e.getMessage(), e.getClass().getSimpleName());
          logger.error("Full IOException stack trace:", e);
          // Drain remaining queue items to prevent memory leak, but don't write them
          int drained = 0;
          while (!writeQueue.isEmpty()) {
            String drainedEvent = writeQueue.poll();
            if (drainedEvent != null && !END_OF_STREAM_SENTINEL.equals(drainedEvent)) {
              drained++;
              logger.error("Drained unwritten event (lost): {}", drainedEvent.substring(0, Math.min(100, drainedEvent.length())));
            }
          }
          if (drained > 0) {
            logger
                .error("CRITICAL: Drained {} queued events after IOException - these events did NOT reach the client - agentId: {}, sessionId: {}, requestId: {}",
                       drained, agentId, effectiveSessionId, requestId);
          }
        } finally {
          try {
            finalOut.close();
            logger.debug("Writer thread closed output stream - agentId: {}, sessionId: {}, requestId: {}", agentId,
                         effectiveSessionId, requestId);
          } catch (IOException e) {
            logger.debug("Could not close output stream: {}", e.getMessage());
          }
        }
      }, STREAMING_EXECUTOR);

      // Start the streaming process asynchronously on the bounded STREAMING_EXECUTOR
      // (NOT on ForkJoinPool.commonPool() which has ~CPU-cores threads and would starve under blocking retries)
      CompletableFuture.runAsync(() -> {
        logger.debug("Starting async streaming operation - agentId: {}, sessionId: {}, requestId: {}", agentId,
                     effectiveSessionId, requestId);
        try {
          streamBedrockResponseWithRetry(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                                         excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                                         knowledgeBaseConfigs,
                                         finalOut, retryConfig, chunksReceived, sessionStartSent, requestId, correlationId,
                                         userId,
                                         operationTimeout, operationTimeoutUnit, writeQueue, clientDisconnected);
          logger
              .debug("Async streaming operation completed successfully - agentId: {}, sessionId: {}, requestId: {}, totalElapsedMs: {}",
                     agentId, effectiveSessionId, requestId, System.currentTimeMillis() - requestStartTime);
          // streamBedrockResponse will have queued all chunk/completion events, but not the sentinel
          // Note: handleStreamComplete already queues END_OF_STREAM_SENTINEL, so we don't need to do it here
          logger
              .debug("Success path - END_OF_STREAM_SENTINEL should have been queued by handleStreamComplete - agentId: {}, sessionId: {}, requestId: {}",
                     agentId, effectiveSessionId, requestId);
        } catch (Exception e) {
          logger.debug("Async streaming operation failed - agentId: {}, sessionId: {}, requestId: {}, error: {}, elapsedMs: {}",
                       agentId, effectiveSessionId, requestId, e.getMessage(), System.currentTimeMillis() - requestStartTime);

          // This prevents "Pipe closed" IOException when writer thread has already closed the pipe
          try {
            // Send session-start event before error if not already sent (for consistency)
            if (sessionStartSent.compareAndSet(false, true)) {
              JSONObject startEvent =
                  createSessionStartJson(agentAliasId, agentId, prompt, effectiveSessionId, Instant.now().toString(),
                                         requestId, correlationId, userId, -1);
              String sseStart = formatSSEEvent("session-start", startEvent.toString());
              boolean queued = writeQueue.offer(sseStart);
              logger.info("Queued session-start event: {} - agentId: {}, sessionId: {}, requestId: {}, queueSize: {}",
                          queued, agentId, effectiveSessionId, requestId, writeQueue.size());
              logger.info(sseStart);
            }

            // streamBedrockResponseWithRetry already enhances the error message with retry information
            // The exception message already contains retry details if retries were attempted
            // Send error as SSE event (createErrorJson will use the exception's message which is already enhanced)
            JSONObject errorJson = createErrorJson(e);
            String errorEvent = formatSSEEvent(ERROR_KEY, errorJson.toString());
            boolean queued = writeQueue.offer(errorEvent);
            logger.error("Queued error event: {} - agentId: {}, sessionId: {}, requestId: {}, queueSize: {}",
                         queued, agentId, effectiveSessionId, requestId, writeQueue.size());
            logger.error(errorEvent);
          } catch (Exception queueException) {
            logger
                .error("Failed to queue error event - agentId: {}, sessionId: {}, requestId: {}, correlationId: {}, error: {}",
                       agentId, effectiveSessionId, requestId, correlationId, queueException.getMessage());
          } finally {
            boolean queued = writeQueue.offer(END_OF_STREAM_SENTINEL);
            logger.debug("Queued END_OF_STREAM_SENTINEL: {} - agentId: {}, sessionId: {}, requestId: {}, finalQueueSize: {}",
                         queued, agentId, effectiveSessionId, requestId, writeQueue.size());
          }
        } finally {
          try {
            logger.debug("Waiting for writer thread to complete - agentId: {}, sessionId: {}, requestId: {}, queueSize: {}",
                         agentId, effectiveSessionId, requestId, writeQueue.size());
            writerFuture.get(5, TimeUnit.SECONDS); // Wait up to 5 seconds for writer to complete
            logger.debug("Writer thread completed successfully - agentId: {}, sessionId: {}, requestId: {}",
                         agentId, effectiveSessionId, requestId);
          } catch (java.util.concurrent.TimeoutException te) {
            logger
                .error("TIMEOUT: Writer thread did not complete within 5 seconds - agentId: {}, sessionId: {}, requestId: {}, remainingQueueSize: {}",
                       agentId, effectiveSessionId, requestId, writeQueue.size());
          } catch (Exception writerException) {
            logger.error("Writer thread failed - agentId: {}, sessionId: {}, requestId: {}, error: {}, errorType: {}",
                         agentId, effectiveSessionId, requestId, writerException.getMessage(),
                         writerException.getClass().getSimpleName());
            logger.error("Writer thread exception stack trace:", writerException);
          }
        }
      }, STREAMING_EXECUTOR);
      logger
          .debug("SSE streaming request initialized, returning InputStream - agentId: {}, sessionId: {}, requestId: {}, elapsedMs: {}",
                 agentId, effectiveSessionId, requestId, System.currentTimeMillis() - requestStartTime);
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
                                              BlockingQueue<String> writeQueue, AtomicBoolean clientDisconnected)
      throws ExecutionException, InterruptedException, IOException {
    long retryStartTime = System.currentTimeMillis();
    logger
        .debug("Starting Bedrock streaming with retry - agentId: {}, agentAlias: {}, sessionId: {}, requestId: {}, retryEnabled: {}, maxRetries: {}",
               agentId, agentAliasId, effectiveSessionId, requestId, retryConfig.isEnabled(), retryConfig.getMaxRetries());


    StreamingRetryUtility.StreamingOperation operation = () -> {
      streamBedrockResponse(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                            excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                            knowledgeBaseConfigs, outputStream, chunksReceived, sessionStartSent, requestId,
                            correlationId, userId, operationTimeout, operationTimeoutUnit, writeQueue, clientDisconnected);
    };



    StreamingRetryUtility.RetryResult result =
        StreamingRetryUtility.executeWithRetry(operation, retryConfig, chunksReceived, agentId, effectiveSessionId, requestId);
    logger
        .debug("Bedrock streaming operation completed - agentId: {}, sessionId: {}, requestId: {}, success: {}, attemptsMade: {}, elapsedMs: {}",
               agentId, effectiveSessionId, requestId, result.isSuccess(), result.getAttemptsMade(),
               System.currentTimeMillis() - retryStartTime);
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

      // Re-throw the original exception with enhanced message
      // If it's an ExecutionException, InterruptedException, or IOException, preserve the type
      Exception lastException = result.getLastException();
      if (lastException instanceof ExecutionException) {
        throw new ExecutionException(errorMessage, lastException.getCause());
      } else if (lastException instanceof InterruptedException) {
        InterruptedException ie = new InterruptedException(errorMessage);
        ie.initCause(lastException);
        throw ie;
      } else if (lastException instanceof IOException) {
        throw new IOException(errorMessage, lastException);
      } else {
        // For other exceptions, wrap in ModuleException
        throw new ModuleException(errorMessage, BedrockErrorType.CLIENT_ERROR, lastException);
      }
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
    logger
        .debug("Building Bedrock streaming request - agentId: {}, agentAlias: {}, sessionId: {}, requestId: {}, enableTrace: {}, latencyOptimized: {}",
               agentId, agentAliasId, effectiveSessionId, requestId, enableTrace, latencyOptimized);

    // Track chunk timing and count
    AtomicInteger chunkCount = new AtomicInteger(0);
    AtomicLong lastChunkTime = new AtomicLong(startTime);
    AtomicLong timeToFirstChunk = new AtomicLong(-1); // -1 means not yet received



    InvokeAgentRequest request = buildStreamingInvokeAgentRequest(agentAliasId, agentId, prompt, enableTrace,
                                                                  latencyOptimized, effectiveSessionId,
                                                                  excludePreviousThinkingSteps,
                                                                  previousConversationTurnsToInclude,
                                                                  knowledgeBaseConfigs, operationTimeout, operationTimeoutUnit);
    logger
        .debug("Bedrock streaming request built, invoking Bedrock API - agentId: {}, agentAlias: {}, sessionId: {}, requestId: {}",
               agentId, agentAliasId, effectiveSessionId, requestId);

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

    logger.debug("Bedrock API invocation started (async) - agentId: {}, sessionId: {}, requestId: {}, elapsedMs: {}",
                 agentId, effectiveSessionId, requestId, System.currentTimeMillis() - startTime);
    try {
      while (!invocationFuture.isDone()) {
        // Check if client disconnected during streaming
        if (clientDisconnected.get()) {
          logger.debug("Client disconnected during Bedrock invocation - agentId: {}, sessionId: {}, requestId: {}",
                       agentId, effectiveSessionId, requestId);
          writeQueue.offer(END_OF_STREAM_SENTINEL);
          logger.debug("Early exit due to client disconnect - agentId: {}, sessionId: {}, requestId: {}, elapsedMs: {}",
                       agentId, effectiveSessionId, requestId, System.currentTimeMillis() - startTime);
          // Don't wait for Bedrock invocation - let it complete in background
          // This releases the thread immediately for other requests
          return;
        }

        // Wait with short timeout before checking again (500ms polling interval)
        try {
          invocationFuture.get(500, TimeUnit.MILLISECONDS);
          break; // Completed successfully
        } catch (java.util.concurrent.TimeoutException te) {
          // Expected - continue polling
        }
      }

      logger
          .debug("Bedrock invocation completed, streaming complete - agentId: {}, sessionId: {}, requestId: {}, totalElapsedMs: {}",
                 agentId, effectiveSessionId, requestId, System.currentTimeMillis() - startTime);

    } catch (Exception e) {
      logger
          .debug("Bedrock SDK error - agentId: {}, sessionId: {}, requestId: {}, correlationId: {}, errorType: {}, error: {}, elapsedMs: {}",
                 agentId, effectiveSessionId, requestId, correlationId, e.getClass().getSimpleName(), e.getMessage(),
                 System.currentTimeMillis() - startTime);
      // The outer catch block in invokeAgentSSEStream will queue error events first, then the sentinel.
      // If we queue sentinel here, writer thread exits before error events are queued.
      logger
          .debug("Throwing exception to outer handler - agentId: {}, sessionId: {}, requestId: {} - outer handler will queue error events",
                 agentId, effectiveSessionId, requestId);
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
      logger.debug("Skipping chunk - client disconnected - agentId: {}, sessionId: {}, requestId: {}",
                   agentId, effectiveSessionId, requestId);
      return;
    }
    try {
      // Mark that chunks have been received (for retry logic)
      boolean isFirstChunk = !chunksReceived.get();
      chunksReceived.set(true);
      // Track chunk timing
      long currentTime = System.currentTimeMillis();
      int currentChunkCount = chunkCount.incrementAndGet();
      long timeSinceLastChunk = currentTime - lastChunkTime.getAndSet(currentTime);

      if (sessionStartSent.compareAndSet(false, true)) {
        long firstChunkTime = currentTime - startTime;
        timeToFirstChunk.set(firstChunkTime); // Store for completion summary
        logger
            .debug("First chunk received, sending session-start event - agentId: {}, sessionId: {}, requestId: {}, timeToFirstChunkMs: {}",
                   agentId, effectiveSessionId, requestId, firstChunkTime);
        writeSessionStartEvent(agentAliasId, agentId, prompt, effectiveSessionId, requestId, correlationId, userId,
                               firstChunkTime, writeQueue);
      }
      if (isFirstChunk) {

        // Use the stored timeToFirstChunk value for consistency (measured when chunk first arrived)
        long storedTimeToFirstChunk = timeToFirstChunk.get() >= 0 ? timeToFirstChunk.get() : (currentTime - startTime);
        logger.debug("First chunk queued - agentId: {}, sessionId: {}, requestId: {}, timeToFirstChunkMs: {}",
                     agentId, effectiveSessionId, requestId, storedTimeToFirstChunk);
      } else if (timeSinceLastChunk > 1000) {
        // Log if gap between chunks is > 1 second (might indicate slow streaming)
        logger.debug("Chunk timing - agentId: {}, sessionId: {}, requestId: {}, chunkNumber: {}, timeSinceLastChunkMs: {}",
                     agentId, effectiveSessionId, requestId, currentChunkCount, timeSinceLastChunk);
      }
      JSONObject chunkData = createChunkJson(chunk);
      String sseEvent = formatSSEEvent(CHUNK, chunkData.toString());
      // Log the formatted SSE chunk event at DEBUG level
      logger.debug(sseEvent);
      int queueSizeBefore = writeQueue.size();
      boolean queued = writeQueue.offer(sseEvent); // Non-blocking add to queue
      int queueSizeAfter = writeQueue.size();
      if (queueSizeAfter > 25) {
        logger
            .debug("Queued chunk event - agentId: {}, sessionId: {}, requestId: {}, queued: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                   agentId, effectiveSessionId, requestId, queued, queueSizeBefore, queueSizeAfter);
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
    int queueSizeBefore = writeQueue.size();
    boolean queued = writeQueue.offer(sseStart); // Non-blocking add to queue
    int queueSizeAfter = writeQueue.size();
    // Only log if queue size indicates concerning backpressure (queueSizeAfter > 25)
    if (queueSizeAfter > 25) {
      logger
          .debug("Queued session-start event - agentId: {}, sessionId: {}, requestId: {}, queued: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                 agentId, effectiveSessionId, requestId, queued, queueSizeBefore, queueSizeAfter);
    }
    logger.info(sseStart);
  }

  private void writeChunkErrorEvent(IOException e, BlockingQueue<String> writeQueue) {
    String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
    writeQueue.offer(errorEvent);
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
      logger.debug("Skipping completion event - client disconnected - agentId: {}, sessionId: {}, requestId: {}",
                   agentId, effectiveSessionId, requestId);
      // Still signal end of stream to allow writer thread to exit cleanly
      writeQueue.offer(END_OF_STREAM_SENTINEL);
      return;
    }
    try {
      long endTime = System.currentTimeMillis();
      long totalDuration = endTime - startTime;
      int finalChunkCount = chunkCount.get();
      long timeToFirstChunkMs = timeToFirstChunk.get();
      logger
          .debug("Bedrock streaming completed - agentId: {}, sessionId: {}, requestId: {}, totalDurationMs: {}, timeToFirstChunkMs: {}, totalChunks: {}, chunksReceived: {}",
                 agentId, effectiveSessionId, requestId, totalDuration, timeToFirstChunkMs >= 0 ? timeToFirstChunkMs : -1,
                 finalChunkCount, chunksReceived.get());

      JSONObject completionData = createCompletionJson(effectiveSessionId, agentId, agentAliasId, totalDuration,
                                                       requestId, correlationId, userId,
                                                       timeToFirstChunkMs >= 0 ? timeToFirstChunkMs : -1, finalChunkCount);
      String completionEvent = formatSSEEvent("session-complete", completionData.toString());
      int queueSizeBefore = writeQueue.size();
      writeQueue.offer(completionEvent); // Non-blocking add to queue
      int queueSizeAfter = writeQueue.size();
      // Only log if queue size indicates concerning backpressure (queueSizeAfter > 25)
      if (queueSizeAfter > 25) {
        logger
            .debug("Queued completion event - agentId: {}, sessionId: {}, requestId: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                   agentId, effectiveSessionId, requestId, queueSizeBefore, queueSizeAfter);
      }
      logger.info(completionEvent);
    } catch (Exception e) {
      logger.debug("Error creating completion event - agentId: {}, sessionId: {}, requestId: {}, error: {}",
                   agentId, effectiveSessionId, requestId, e.getMessage());
      String errorEvent = formatSSEEvent("completion-error", createErrorJson(e).toString());
      int queueSizeBefore = writeQueue.size();
      writeQueue.offer(errorEvent);
      int queueSizeAfter = writeQueue.size();
      // Only log if queue size indicates concerning backpressure (queueSizeAfter > 25)
      if (queueSizeAfter > 25) {
        logger
            .debug("Queued completion-error event - agentId: {}, sessionId: {}, requestId: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                   agentId, effectiveSessionId, requestId, queueSizeBefore, queueSizeAfter);
      }
      logger.error("Error creating completion event: {}", e.getMessage());

    } finally {
      // Signal end of stream to the writer thread
      int queueSizeBefore = writeQueue.size();
      writeQueue.offer(END_OF_STREAM_SENTINEL);
      int queueSizeAfter = writeQueue.size();
      if (queueSizeAfter > 25) {
        logger
            .debug("Queued end-of-stream signal - agentId: {}, sessionId: {}, requestId: {}, queueSizeBefore: {}, queueSizeAfter: {}",
                   agentId, effectiveSessionId, requestId, queueSizeBefore, queueSizeAfter);
      }
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
}
