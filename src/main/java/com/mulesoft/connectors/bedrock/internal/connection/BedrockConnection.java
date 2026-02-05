package com.mulesoft.connectors.bedrock.internal.connection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import org.mule.connectors.commons.template.connection.ConnectorConnection;
import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;
import com.mulesoft.connectors.bedrock.internal.error.ErrorHandler;
import org.mule.runtime.extension.api.exception.ModuleException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagent.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockagent.model.BedrockAgentException;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagent.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockagent.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockagent.model.ValidationException;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

public class BedrockConnection implements ConnectorConnection {

  private static final String AGENT_RUNTIME_ASYNC_CLIENT_TYPE = "BedrockAgentRuntimeAsync";
  private static final String RUNTIME_ASYNC_CLIENT_TYPE = "BedrockRuntimeAsync";
  private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

  private final String region;
  private final BedrockRuntimeClient bedrockRuntimeClient;
  private final BedrockClient bedrockClient;
  private final BedrockAgentClient bedrockAgentClient;
  private final BedrockAgentRuntimeClient bedrockAgentRuntimeClient;
  private final IamClient iamClient;
  private final int connectionTimeoutMs;
  private final ConcurrentHashMap<String, BedrockAgentRuntimeAsyncClient> agentRuntimeAsyncClients =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, BedrockRuntimeAsyncClient> runtimeAsyncClients =
      new ConcurrentHashMap<>();
  private final LongFunction<BedrockAgentRuntimeAsyncClient> agentRuntimeAsyncClientFactory;
  private final LongFunction<BedrockRuntimeAsyncClient> runtimeAsyncClientFactory;

  public BedrockConnection(String region,
                           BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder,
                           BedrockClientBuilder bedrockClientBuilder,
                           BedrockAgentClientBuilder bedrockAgentClientBuilder,
                           BedrockAgentRuntimeClientBuilder bedrockAgentRuntimeClientBuilder,
                           IamClientBuilder iamClientBuilder,
                           int connectionTimeoutMs,
                           LongFunction<BedrockAgentRuntimeAsyncClient> agentRuntimeAsyncClientFactory,
                           LongFunction<BedrockRuntimeAsyncClient> runtimeAsyncClientFactory) {
    this.region = region;
    this.bedrockRuntimeClient = bedrockRuntimeClientBuilder.build();
    this.bedrockClient = bedrockClientBuilder.build();
    this.bedrockAgentClient = bedrockAgentClientBuilder.build();
    this.bedrockAgentRuntimeClient = bedrockAgentRuntimeClientBuilder.build();
    this.iamClient = iamClientBuilder.build();
    this.connectionTimeoutMs = connectionTimeoutMs;
    this.agentRuntimeAsyncClientFactory = agentRuntimeAsyncClientFactory;
    this.runtimeAsyncClientFactory = runtimeAsyncClientFactory;
  }

  /**
   * Returns the connection-level timeout in milliseconds (used for SdkAsyncHttpClient and as fallback when no operation-level
   * timeout is provided).
   */
  public int getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  /**
   * Builds a cache key that includes client type and effective timeout so clients with different timeouts are cached separately
   * (whether timeout is from operation-level or connection-level).
   */
  private static String buildCacheKey(String clientType, long effectiveTimeoutMs) {
    return clientType + ":" + effectiveTimeoutMs;
  }

  /**
   * If cache key is already in the cache, returns that instance. Otherwise creates the client once via {@code clientSupplier},
   * stores it in the cache, and returns the new instance.
   */
  private static <T> T getOrCreateClient(java.util.Map<String, T> clients, String cacheKey,
                                         Supplier<T> clientSupplier) {
    return (T) clients.computeIfAbsent(cacheKey, k -> clientSupplier.get());
  }

  /**
   * Returns a cached or newly created BedrockAgentRuntimeAsyncClient for the given effective timeout (ms). Effective timeout
   * should be operation-level when available, otherwise connection-level.
   */
  public BedrockAgentRuntimeAsyncClient getOrCreateBedrockAgentRuntimeAsyncClient(long effectiveTimeoutMs) {
    String cacheKey = buildCacheKey(AGENT_RUNTIME_ASYNC_CLIENT_TYPE, effectiveTimeoutMs);
    return getOrCreateClient(agentRuntimeAsyncClients, cacheKey,
                             () -> agentRuntimeAsyncClientFactory.apply(effectiveTimeoutMs));
  }

  public BedrockRuntimeAsyncClient getBedrockRuntimeAsyncClient() {
    return getOrCreateBedrockRuntimeAsyncClient(connectionTimeoutMs);
  }

  /**
   * Returns a cached or newly created BedrockRuntimeAsyncClient for the given effective timeout (ms). Effective timeout should be
   * operation-level when available, otherwise connection-level.
   */
  public BedrockRuntimeAsyncClient getOrCreateBedrockRuntimeAsyncClient(long effectiveTimeoutMs) {
    String cacheKey = buildCacheKey(RUNTIME_ASYNC_CLIENT_TYPE, effectiveTimeoutMs);
    return getOrCreateClient(runtimeAsyncClients, cacheKey,
                             () -> runtimeAsyncClientFactory.apply(effectiveTimeoutMs));
  }

  public String getRegion() {
    return region;
  }

  public BedrockRuntimeClient getBedrockRuntimeClient() {
    return bedrockRuntimeClient;
  }

  public BedrockClient getBedrockClient() {
    return bedrockClient;
  }

  public BedrockAgentClient getBedrockAgentClient() {
    return bedrockAgentClient;
  }

  public IamClient getIamClient() {
    return iamClient;
  }

  public InvokeModelResponse answerPrompt(InvokeModelRequest invokeModelRequest) {
    return getBedrockRuntimeClient().invokeModel(invokeModelRequest);
  }

  public GetFoundationModelResponse getFoundationModel(GetFoundationModelRequest foundationModelRequest) {
    return getBedrockClient().getFoundationModel(foundationModelRequest);
  }

  public ListFoundationModelsResponse listFoundationalModels() {
    return getBedrockClient().listFoundationModels(r -> {
    });
  }

  @Override
  public void disconnect() {
    // Default method
    // no need of any implementation

  }

  @Override
  public void validate() {
    try {
      getBedrockClient().listFoundationModels(r -> {
      });
    } catch (software.amazon.awssdk.services.bedrock.model.BedrockException e) {
      handleBedrockException(e);
    } catch (AccessDeniedException e) {
      throw ErrorHandler.handleAccessDeniedException(e);
    } catch (ValidationException e) {
      throw ErrorHandler.handleValidationException(e);
    } catch (ResourceNotFoundException e) {
      throw ErrorHandler.handleResourceNotFoundException(e);
    } catch (ThrottlingException e) {
      throw ErrorHandler.handleThrottlingException(e);
    } catch (BedrockAgentException e) {
      throw ErrorHandler.handleBedrockAgentException(e);
    } catch (SdkServiceException e) {
      handleSdkServiceExceptionOnValidate(e);
    } catch (SdkClientException e) {
      handleSdkClientExceptionOnValidate(e);
    } catch (SdkException e) {
      throw ErrorHandler.handleSdkException(e);
    }
  }

  private void handleBedrockException(software.amazon.awssdk.services.bedrock.model.BedrockException e) {
    if (e.statusCode() == 403) {
      throw new ModuleException(INVALID_CREDENTIALS_MESSAGE, BedrockErrorType.ACCESS_DENIED, e);
    }
    throw new ModuleException("Bedrock service error", BedrockErrorType.SERVICE_ERROR, e);
  }

  private void handleSdkServiceExceptionOnValidate(SdkServiceException e) {
    if (e.statusCode() == 403) {
      throw new ModuleException(INVALID_CREDENTIALS_MESSAGE, BedrockErrorType.ACCESS_DENIED, e);
    }
    throw ErrorHandler.handleSdkServiceException(e);
  }

  private void handleSdkClientExceptionOnValidate(SdkClientException e) {
    if (isCredentialRelatedClientError(e)) {
      throw new ModuleException(INVALID_CREDENTIALS_MESSAGE, BedrockErrorType.ACCESS_DENIED, e);
    }
    throw ErrorHandler.handleSdkClientException(e, "Connection validation");
  }

  private static boolean isCredentialRelatedClientError(SdkClientException e) {
    String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
    return message.contains("unable to load credentials")
        || (message.contains("invalid") && message.contains("credential"));
  }


  public ListAgentsResponse listAgents(ListAgentsRequest listAgentsRequest) {
    return getBedrockAgentClient().listAgents(listAgentsRequest);
  }

  public GetAgentResponse getAgent(GetAgentRequest getAgentRequest) {
    return getBedrockAgentClient().getAgent(getAgentRequest);
  }

  public CompletableFuture<Void> invokeAgent(InvokeAgentRequest request, InvokeAgentResponseHandler handler) {
    return invokeAgent(request, handler, connectionTimeoutMs);
  }

  /**
   * Invokes the agent using a client cached for the given effective timeout (operation-level if provided, otherwise
   * connection-level).
   */
  public CompletableFuture<Void> invokeAgent(InvokeAgentRequest request, InvokeAgentResponseHandler handler,
                                             long effectiveTimeoutMs) {
    return getOrCreateBedrockAgentRuntimeAsyncClient(effectiveTimeoutMs).invokeAgent(request, handler);
  }

  public InvokeModelResponse invokeModel(InvokeModelRequest request) {
    return getBedrockRuntimeClient().invokeModel(request);
  }

  public CompletableFuture<Void> answerPromptStreaming(ConverseStreamRequest request, ConverseStreamResponseHandler handler) {
    return getBedrockRuntimeAsyncClient().converseStream(request, handler);
  }
}
