package org.mule.extension.bedrock.internal.connection;

import java.util.concurrent.CompletableFuture;
import org.mule.connectors.commons.template.connection.ConnectorConnection;
import org.mule.extension.bedrock.internal.error.BedrockErrorType;
import org.mule.extension.bedrock.internal.error.ErrorHandler;
import org.mule.extension.bedrock.internal.error.exception.BedrockException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelResponse;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListCustomModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagent.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockagent.model.BedrockAgentException;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentAliasRequest;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentAliasResponse;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentAliasRequest;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentAliasResponse;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentAliasesRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentAliasesResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagent.model.PrepareAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.PrepareAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockagent.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockagent.model.ValidationException;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;

public class BedrockConnection implements ConnectorConnection {

  private final BedrockRuntimeClient bedrockRuntimeClient;
  private final BedrockClient bedrockClient;
  private final BedrockAgentClient bedrockAgentClient;
  private final BedrockAgentRuntimeClient bedrockAgentRuntimeClient;
  private final IamClient iamClient;
  private final BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient;
  private final BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient;

  public BedrockConnection(BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder,
                           BedrockClientBuilder bedrockClientBuilder,
                           BedrockAgentClientBuilder bedrockAgentClientBuilder,
                           BedrockAgentRuntimeClientBuilder bedrockAgentRuntimeClientBuilder,
                           IamClientBuilder iamClientBuilder,
                           BedrockAgentRuntimeAsyncClientBuilder bedrockAgentRuntimeAsyncClientBuilder,
                           BedrockRuntimeAsyncClientBuilder bedrockRuntimeAsyncClientBuilder) {
    this.bedrockRuntimeClient = bedrockRuntimeClientBuilder.build();
    this.bedrockClient = bedrockClientBuilder.build();
    this.bedrockAgentClient = bedrockAgentClientBuilder.build();
    this.bedrockAgentRuntimeClient = bedrockAgentRuntimeClientBuilder.build();
    this.iamClient = iamClientBuilder.build();
    this.bedrockAgentRuntimeAsyncClient = bedrockAgentRuntimeAsyncClientBuilder.build();
    this.bedrockRuntimeAsyncClient = bedrockRuntimeAsyncClientBuilder.build();
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

  public BedrockAgentRuntimeClient getBedrockAgentRuntimeClient() {
    return bedrockAgentRuntimeClient;
  }

  public IamClient getIamClient() {
    return iamClient;
  }

  public BedrockAgentRuntimeAsyncClient getBedrockAgentRuntimeAsyncClient() {
    return bedrockAgentRuntimeAsyncClient;
  }

  public BedrockRuntimeAsyncClient getBedrockRuntimeAsyncClient() {
    return bedrockRuntimeAsyncClient;
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

  public GetCustomModelResponse getCustomModel(GetCustomModelRequest getCustomModelRequest) {
    return getBedrockClient().getCustomModel(getCustomModelRequest);
  }

  public ListCustomModelsResponse listCustomModels() {
    return getBedrockClient().listCustomModels(r -> {
    });
  }

  @Override
  public void disconnect() {

  }

  @Override
  public void validate() {
    // Validate connection by making a lightweight API call
    // This will throw an exception if credentials are invalid
    try {
      getBedrockClient().listFoundationModels(r -> {
      });
    } catch (software.amazon.awssdk.services.bedrock.model.BedrockException e) {
      // Handle Bedrock service exceptions (from bedrock service, not bedrockagent)
      // Check if it's a 403 (Forbidden) which indicates invalid credentials
      if (e.statusCode() == 403) {
        throw new BedrockException("Invalid credentials", BedrockErrorType.ACCESS_DENIED, e);
      }
      // For other Bedrock service errors, use SERVICE_ERROR
      throw new BedrockException("Bedrock service error", BedrockErrorType.SERVICE_ERROR, e);
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
      // Check if it's a 403 (Forbidden) which indicates invalid credentials
      if (e.statusCode() == 403) {
        throw new BedrockException("Invalid credentials", BedrockErrorType.ACCESS_DENIED, e);
      }
      throw ErrorHandler.handleSdkServiceException(e);
    } catch (SdkClientException e) {
      // Check if it's an authentication error (invalid credentials)
      // SdkClientException for auth errors typically contains "Unable to load credentials"
      // or similar messages, but network errors also use SdkClientException
      String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
      if (message.contains("unable to load credentials") ||
          (message.contains("invalid") && message.contains("credential"))) {
        throw new BedrockException("Invalid credentials", BedrockErrorType.ACCESS_DENIED, e);
      }
      // For other client errors (like network issues), use CLIENT_ERROR
      throw ErrorHandler.handleSdkClientException(e, "Connection validation");
    } catch (SdkException e) {
      throw ErrorHandler.handleSdkException(e);
    }
  }


  public ListAgentsResponse listAgents(ListAgentsRequest listAgentsRequest) {
    return getBedrockAgentClient().listAgents(listAgentsRequest);
  }

  public GetAgentResponse getAgent(GetAgentRequest getAgentRequest) {
    return getBedrockAgentClient().getAgent(getAgentRequest);
  }

  public DeleteAgentResponse deleteAgent(DeleteAgentRequest deleteAgentRequest) {
    return getBedrockAgentClient().deleteAgent(deleteAgentRequest);
  }

  public GetRoleResponse getRole(GetRoleRequest getRoleRequest) {
    return getIamClient().getRole(getRoleRequest);
  }

  public CreateRoleResponse createRole(CreateRoleRequest createRoleRequest) {
    return getIamClient().createRole(createRoleRequest);
  }

  public void putRolePolicy(PutRolePolicyRequest putRolePolicyRequest) {
    getIamClient().putRolePolicy(putRolePolicyRequest);
  }

  public CreateAgentResponse createAgent(CreateAgentRequest createAgentRequest) {
    return getBedrockAgentClient().createAgent(createAgentRequest);
  }

  public PrepareAgentResponse prepareAgent(PrepareAgentRequest prepareAgentRequest) {
    return getBedrockAgentClient().prepareAgent(prepareAgentRequest);
  }

  public CreateAgentAliasResponse createAgentAlias(CreateAgentAliasRequest createAgentAliasRequest) {
    return getBedrockAgentClient().createAgentAlias(createAgentAliasRequest);
  }

  public ListAgentAliasesResponse listAgentAliases(ListAgentAliasesRequest listAgentAliasesRequest) {
    return getBedrockAgentClient().listAgentAliases(listAgentAliasesRequest);
  }

  public DeleteAgentAliasResponse deleteAgentAlias(DeleteAgentAliasRequest deleteAgentAliasRequest) {
    return getBedrockAgentClient().deleteAgentAlias(deleteAgentAliasRequest);
  }

  public CompletableFuture<Void> invokeAgent(InvokeAgentRequest request, InvokeAgentResponseHandler handler) {
    return getBedrockAgentRuntimeAsyncClient().invokeAgent(request, handler);
  }

  public InvokeModelResponse invokeModel(InvokeModelRequest request) {
    return getBedrockRuntimeClient().invokeModel(request);
  }

  public CompletableFuture<Void> answerPromptStreaming(ConverseStreamRequest request, ConverseStreamResponseHandler handler) {
    return getBedrockRuntimeAsyncClient().converseStream(request, handler);
  }
}
