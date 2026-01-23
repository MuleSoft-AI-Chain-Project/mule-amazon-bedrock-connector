package org.mule.extension.bedrock.internal.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockAgentsFilteringParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsParameters;
import org.mule.extension.bedrock.api.params.BedrockAgentsSessionParameters;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.ErrorHandler;
import org.mule.extension.bedrock.internal.helper.PromptPayloadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.BedrockModelConfigurations;
import software.amazon.awssdk.services.bedrockagentruntime.model.FilterAttribute;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.KnowledgeBaseVectorSearchConfiguration;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;
import software.amazon.awssdk.services.bedrockagentruntime.model.PromptCreationConfigurations;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrievalFilter;
import software.amazon.awssdk.services.bedrockagentruntime.model.SessionState;
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


  private static final AtomicInteger eventCounter = new AtomicInteger(0);

  public AgentServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String definePromptTemplate(String promptTemplate, String instructions, String dataset,
                                     BedrockParameters bedrockParameters) {
    try {
      String finalPromptTemplate = PromptPayloadHelper.definePromptTemplate(promptTemplate, instructions, dataset);
      String nativeRequest = PromptPayloadHelper.identifyPayload(finalPromptTemplate, bedrockParameters);
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters.getModelName());
    }

  }

  @Override
  public String listAgents(BedrockAgentsParameters bedrockAgentsParameters) {
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
  public String getAgentById(String agentId, BedrockAgentsParameters bedrockAgentsParameters) {
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
                              BedrockAgentsParameters bedrockAgentsParameters) {
    String sessionId = bedrockSessionParameters.getSessionId();
    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
    logger.info("Using sessionId: {}", effectiveSessionId);
    return invokeAgent(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                       bedrockSessionParameters.getExcludePreviousThinkingSteps(),
                       bedrockSessionParameters.getPreviousConversationTurnsToInclude(),
                       bedrockAgentsFilteringParameters.getKnowledgeBaseId(),
                       bedrockAgentsFilteringParameters.getNumberOfResults(),
                       bedrockAgentsFilteringParameters.getOverrideSearchType(),
                       bedrockAgentsFilteringParameters.getRetrievalMetadataFilterType(),
                       bedrockAgentsFilteringParameters.getMetadataFilters())
        .thenApply(response -> {
          logger.debug(response);
          return response;
        }).join();
  }

  private CompletableFuture<String> invokeAgent(String agentAliasId, String agentId,
                                                String prompt, boolean enableTrace,
                                                boolean latencyOptimized, String effectiveSessionId,
                                                boolean excludePreviousThinkingSteps, Integer previousConversationTurnsToInclude,
                                                String knowledgeBaseId, Integer numberOfResults,
                                                BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                                BedrockAgentsFilteringParameters.RetrievalMetadataFilterType retrievalMetadataFilterType,
                                                Map<String, String> metadataFilters) {
    long startTime = System.currentTimeMillis();

    InvokeAgentRequest request = InvokeAgentRequest.builder()
        .agentId(agentId)
        .agentAliasId(agentAliasId)
        .sessionId(effectiveSessionId)
        .inputText(prompt)
        .enableTrace(enableTrace)
        .sessionState(buildSessionState(knowledgeBaseId, numberOfResults, overrideSearchType, retrievalMetadataFilterType,
                                        metadataFilters))
        .bedrockModelConfigurations(buildModelConfigurations(latencyOptimized))
        .promptCreationConfigurations(buildPromptConfigurations(excludePreviousThinkingSteps, previousConversationTurnsToInclude))
        .build();
    CompletableFuture<String> completionFuture = new CompletableFuture<>();
    List<JSONObject> chunks = Collections.synchronizedList(new ArrayList<>());
    InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
        .onChunk(chunk -> {
          JSONObject chunkData = new JSONObject();
          chunkData.put(TYPE, CHUNK);
          chunkData.put(TIMESTAMP, Instant.now().toString());

          if (chunk.bytes() != null) {
            String text = new String(chunk.bytes().asByteArray(), StandardCharsets.UTF_8);
            chunkData.put(TEXT, text);
          }

          // Add attribution/citations if present
          if (chunk.attribution() != null && chunk.attribution().citations() != null) {
            JSONArray citationsArray = new JSONArray();
            chunk.attribution().citations().forEach(citation -> {
              JSONObject citationData = new JSONObject();

              if (citation.generatedResponsePart() != null
                  && citation.generatedResponsePart().textResponsePart() != null) {
                citationData.put(GENERATED_RESPONSE_PART,
                                 citation.generatedResponsePart().textResponsePart().text());
              }

              if (citation.retrievedReferences() != null) {
                JSONArray referencesArray = new JSONArray();
                citation.retrievedReferences().forEach(ref -> {
                  JSONObject refData = new JSONObject();
                  if (ref.content() != null && ref.content().text() != null) {
                    refData.put(CONTENT, ref.content().text());
                  }
                  if (ref.location() != null) {
                    refData.put(LOCATION, ref.location().toString());
                  }
                  if (ref.metadata() != null) {
                    JSONObject metadataObject = new JSONObject(ref.metadata());
                    refData.put(METADATA, metadataObject);
                  }
                  referencesArray.put(refData);
                });
                citationData.put(RETRIEVED_REFERENCES, referencesArray);
              }
              citationsArray.put(citationData);
            });
            chunkData.put(CITATIONS, citationsArray);
          }

          chunks.add(chunkData);
        })
        .build();
    InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
        .subscriber(visitor)
        .build();
    CompletableFuture<Void> invocationFuture = getConnection().invokeAgent(request, handler);
    invocationFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        completionFuture.completeExceptionally(throwable);
      } else {
        JSONObject finalResult = new JSONObject();
        finalResult.put(SESSION_ID, effectiveSessionId);
        finalResult.put(AGENT_ID, agentId);
        finalResult.put(AGENT_ALIAS, agentAliasId);
        finalResult.put(PROMPT, prompt);
        finalResult.put(PROCESSED_AT, Instant.now().toString());
        finalResult.put(CHUNKS, new JSONArray(chunks));

        // Add summary statistics
        JSONObject summary = new JSONObject();
        summary.put(TOTAL_CHUNKS, chunks.size());

        // Concatenate all chunk text for full response
        StringBuilder fullText = new StringBuilder();
        chunks.forEach(chunk -> {
          if (chunk.has(TEXT)) {
            fullText.append(chunk.getString(TEXT));
          }
        });
        summary.put(FULL_RESPONSE, fullText.toString());

        long endTime = System.currentTimeMillis();
        summary.put("total_duration_ms", endTime - startTime);

        finalResult.put(SUMMARY, summary);

        String finalJson = finalResult.toString();
        completionFuture.complete(finalJson);
      }
    });


    return completionFuture;

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

  private Consumer<SessionState.Builder> buildSessionState(String knowledgeBaseId, Integer numberOfResults,
                                                           BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                                           BedrockAgentsFilteringParameters.RetrievalMetadataFilterType retrievalMetadataFilterType,
                                                           Map<String, String> metadataFilters) {
    return sessionStateBuilder -> {
      KnowledgeBaseVectorSearchConfiguration vectorSearchConfig =
          buildVectorSearchConfiguration(numberOfResults, overrideSearchType, retrievalMetadataFilterType, metadataFilters);

      if (vectorSearchConfig != null && knowledgeBaseId != null) {
        sessionStateBuilder.knowledgeBaseConfigurations(
                                                        kbConfigBuilder -> kbConfigBuilder.knowledgeBaseId(knowledgeBaseId)
                                                            .retrievalConfiguration(
                                                                                    retrievalConfigBuilder -> retrievalConfigBuilder
                                                                                        .vectorSearchConfiguration(vectorSearchConfig)));
      }
    };

  }

  private KnowledgeBaseVectorSearchConfiguration buildVectorSearchConfiguration(Integer numberOfResults,
                                                                                BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                                                                BedrockAgentsFilteringParameters.RetrievalMetadataFilterType filterType,
                                                                                Map<String, String> metadataFilters) {
    if (metadataFilters == null || metadataFilters.isEmpty()) {
      return null;
    }
    Map<String, String> nonEmptyFilters = metadataFilters.entrySet().stream()
        .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    if (nonEmptyFilters.isEmpty()) {
      return null;
    }
    Consumer<KnowledgeBaseVectorSearchConfiguration.Builder> applyOptionalNumberOfResults =
        b -> {
          if (numberOfResults != null && numberOfResults.intValue() > 0)
            b.numberOfResults(numberOfResults);
        };
    Consumer<KnowledgeBaseVectorSearchConfiguration.Builder> applyOptionalOverrideSearchType =
        b -> {
          if (overrideSearchType != null)
            // Use the conversion function to pass the SDK's SearchType
            b.overrideSearchType(convertToSdkSearchType(overrideSearchType));
        };
    if (nonEmptyFilters.size() > 1) {
      List<RetrievalFilter> retrievalFilters = nonEmptyFilters.entrySet().stream()
          .map(entry -> RetrievalFilter.builder()
              .equalsValue(FilterAttribute.builder()
                  .key(entry.getKey())
                  .value(Document.fromString(entry.getValue()))
                  .build())
              .build())
          .collect(Collectors.toList());

      RetrievalFilter compositeFilter = RetrievalFilter.builder()
          .applyMutation(builder -> {
            if (filterType == BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.AND_ALL) {
              builder.andAll(retrievalFilters);
            } else if (filterType == BedrockAgentsFilteringParameters.RetrievalMetadataFilterType.OR_ALL) {
              builder.orAll(retrievalFilters);
            }
          })
          .build();

      return KnowledgeBaseVectorSearchConfiguration.builder()
          .filter(compositeFilter)
          .applyMutation(applyOptionalNumberOfResults)
          .applyMutation(applyOptionalOverrideSearchType)
          .build();
    } else {
      String key = nonEmptyFilters.entrySet().iterator().next().getKey();
      return KnowledgeBaseVectorSearchConfiguration.builder()
          .filter(retrievalFilter -> retrievalFilter.equalsValue(FilterAttribute.builder()
              .key(key)
              .value(Document.fromString(nonEmptyFilters.get(key)))
              .build()).build())
          .applyMutation(applyOptionalNumberOfResults)
          .applyMutation(applyOptionalOverrideSearchType)
          .build();
    }

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
                                            BedrockAgentsParameters bedrockAgentsParameters) {
    String sessionId = bedrockSessionParameters.getSessionId();
    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
    logger.info("Using sessionId: {}", effectiveSessionId);
    return invokeAgentSSEStream(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                                bedrockSessionParameters.getExcludePreviousThinkingSteps(),
                                bedrockSessionParameters.getPreviousConversationTurnsToInclude(),
                                bedrockAgentsFilteringParameters.getKnowledgeBaseId(),
                                bedrockAgentsFilteringParameters.getNumberOfResults(),
                                bedrockAgentsFilteringParameters.getOverrideSearchType(),
                                bedrockAgentsFilteringParameters.getRetrievalMetadataFilterType(),
                                bedrockAgentsFilteringParameters.getMetadataFilters());

  }

  private InputStream invokeAgentSSEStream(String agentAliasId, String agentId,
                                           String prompt, boolean enableTrace,
                                           boolean latencyOptimized, String effectiveSessionId,
                                           boolean excludePreviousThinkingSteps,
                                           Integer previousConversationTurnsToInclude,
                                           String knowledgeBaseId, Integer numberOfResults,
                                           BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                           BedrockAgentsFilteringParameters.RetrievalMetadataFilterType retrievalMetadataFilterType,
                                           Map<String, String> metadataFilters) {

    try {
      // Create piped streams for real-time streaming
      PipedOutputStream outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream);

      // Start the streaming process asynchronously
      CompletableFuture.runAsync(() -> {
        try {
          streamBedrockResponse(agentAliasId, agentId, prompt, enableTrace, latencyOptimized, effectiveSessionId,
                                excludePreviousThinkingSteps, previousConversationTurnsToInclude,
                                knowledgeBaseId, numberOfResults, overrideSearchType, retrievalMetadataFilterType,
                                metadataFilters,
                                outputStream);
        } catch (Exception e) {
          try {
            // Send error as SSE event
            String errorEvent = formatSSEEvent("error", createErrorJson(e).toString());
            outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
            logger.error(errorEvent);
          } catch (IOException ioException) {
            // Log error but can't do much more
            logger.error("Error writing error event: {}", ioException.getMessage());
          }
        }
      });

      return inputStream;

    } catch (IOException e) {
      // Return error as immediate SSE event
      String errorEvent = formatSSEEvent("error", createErrorJson(e).toString());
      logger.error(errorEvent);
      return new ByteArrayInputStream(errorEvent.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void streamBedrockResponse(String agentAliasId, String agentId, String prompt,
                                     boolean enableTrace, boolean latencyOptimized,
                                     String effectiveSessionId, boolean excludePreviousThinkingSteps,
                                     Integer previousConversationTurnsToInclude,
                                     String knowledgeBaseId, Integer numberOfResults,
                                     BedrockAgentsFilteringParameters.SearchType overrideSearchType,
                                     BedrockAgentsFilteringParameters.RetrievalMetadataFilterType retrievalMetadataFilterType,
                                     Map<String, String> metadataFilters, PipedOutputStream outputStream)
      throws ExecutionException, InterruptedException, IOException {
    long startTime = System.currentTimeMillis();

    // Send initial event
    JSONObject startEvent = createSessionStartJson(agentAliasId, agentId, prompt, effectiveSessionId, Instant.now().toString());
    String sseStart = formatSSEEvent("session-start", startEvent.toString());
    outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
    outputStream.flush();
    logger.info(sseStart);

    InvokeAgentRequest request = InvokeAgentRequest.builder()
        .agentId(agentId)
        .agentAliasId(agentAliasId)
        .sessionId(effectiveSessionId)
        .inputText(prompt)
        .streamingConfigurations(builder -> builder.streamFinalResponse(true))
        .enableTrace(enableTrace)
        .sessionState(buildSessionState(knowledgeBaseId, numberOfResults, overrideSearchType, retrievalMetadataFilterType,
                                        metadataFilters))
        .bedrockModelConfigurations(buildModelConfigurations(latencyOptimized))
        .promptCreationConfigurations(buildPromptConfigurations(excludePreviousThinkingSteps, previousConversationTurnsToInclude))
        .build();

    InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
        .onChunk(chunk -> {
          try {
            JSONObject chunkData = createChunkJson(chunk);
            String sseEvent = formatSSEEvent("chunk", chunkData.toString());
            outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            logger.debug(sseEvent);
          } catch (IOException e) {
            try {
              String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
              outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
              logger.error(errorEvent);
            } catch (IOException ioException) {
              // Can't write error, stream is likely closed
              logger.error("Error writing error event: {}", ioException.getMessage());
            }
          }
        }).build();

    InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
        .subscriber(visitor)
        .onComplete(() -> {
          try {
            // Send completion event
            long endTime = System.currentTimeMillis();
            JSONObject completionData = createCompletionJson(effectiveSessionId, agentId, agentAliasId, endTime - startTime);
            String completionEvent = formatSSEEvent("session-complete", completionData.toString());
            outputStream.write(completionEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            logger.info(completionEvent);
          } catch (IOException e) {
            try {
              String errorEvent = formatSSEEvent("completion-error", createErrorJson(e).toString());
              outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
              logger.error(errorEvent);
            } catch (IOException ioException) {
              // Can't write error, stream is likely closed
              logger.error("Error writing error event: {}", ioException.getMessage());
            }
          } finally {
            try {
              outputStream.close();
            } catch (IOException ioException) {
              // Log error but can't do much more
              logger.error("Error writing error event: {}", ioException.getMessage());
            }
          }
        })
        .build();

    CompletableFuture<Void> invocationFuture = getConnection().invokeAgent(request, handler);
    invocationFuture.get();
  }

  private JSONObject createCompletionJson(String effectiveSessionId, String agentId,
                                          String agentAliasId, long duration) {
    JSONObject completionData = new JSONObject();
    completionData.put(SESSION_ID, effectiveSessionId);
    completionData.put(AGENT_ID, agentId);
    completionData.put(AGENT_ALIAS, agentAliasId);
    completionData.put("status", "completed");
    completionData.put("total_duration_ms", duration);
    completionData.put(TIMESTAMP, Instant.now().toString());
    return completionData;

  }

  private Object createErrorJson(Throwable error) {
    JSONObject errorData = new JSONObject();
    errorData.put("error", error.getMessage());
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
      chunkData.put("error", "Error processing chunk: " + e.getMessage());
    }

    return chunkData;
  }

  private String formatSSEEvent(String eventType, String data) {
    int eventId = eventCounter.incrementAndGet();
    return String.format("id: %d%nevent: %s%ndata: %s%n%n", eventId, eventType, data);
  }

  private JSONObject createSessionStartJson(String agentAliasId, String agentId,
                                            String prompt, String effectiveSessionId,
                                            String timestamp) {
    JSONObject startData = new JSONObject();
    startData.put(SESSION_ID, effectiveSessionId);
    startData.put(AGENT_ID, agentId);
    startData.put(AGENT_ALIAS, agentAliasId);
    startData.put(PROMPT, prompt);
    startData.put(PROCESSED_AT, timestamp);
    startData.put("status", "started");
    return startData;
  }
}
