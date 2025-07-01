package org.mule.extension.mulechain.helpers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.AwsbedrockParameters;
import org.mule.extension.mulechain.internal.agents.AwsbedrockAgentsParameters;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.core.SdkBytes;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentResponse;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentAliasRequest;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentAliasResponse;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentsResponse;
import software.amazon.awssdk.services.bedrockagent.model.PrepareAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.PrepareAgentResponse;
import software.amazon.awssdk.services.bedrockagent.model.AgentAlias;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentAliasesRequest;
import software.amazon.awssdk.services.bedrockagent.model.ListAgentAliasesResponse;
import software.amazon.awssdk.services.bedrockagent.model.AgentAliasSummary;
import software.amazon.awssdk.services.bedrockagent.model.AgentStatus;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentAliasRequest;
import software.amazon.awssdk.services.bedrockagent.model.CreateAgentAliasResponse;
import java.util.UUID;
import java.util.Optional;

public class AwsbedrockAgentsPayloadHelper {

  private static BedrockRuntimeClient createClient(AwsBasicCredentials awsCreds, Region region) {
    return BedrockRuntimeClient.builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
    .region(region)
    .build();
  }

  private static BedrockRuntimeClient createClientSession(AwsSessionCredentials awsCreds, Region region) {
    return BedrockRuntimeClient.builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
    .region(region)
    .build();
  }

  private static InvokeModelRequest createInvokeRequest(String modelId, String nativeRequest) {
    return InvokeModelRequest.builder()
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .modelId(modelId)
          .build();

  }

  private static String getAmazonTitanText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inputText", prompt);

    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("temperature", awsBedrockParameters.getTemperature());
    textGenerationConfig.put("topP", awsBedrockParameters.getTopP());
    textGenerationConfig.put("maxTokenCount", awsBedrockParameters.getMaxTokenCount());

    jsonRequest.put("textGenerationConfig", textGenerationConfig);

    return jsonRequest.toString();
}

private static String getAmazonNovaText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject textObject = new JSONObject();
    textObject.put("text", prompt);

    // Create the "content" array containing the "text" object
    JSONArray contentArray = new JSONArray();
    contentArray.put(textObject);

    // Create the "messages" array and add the "user" message
    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");
    userMessage.put("content", contentArray);

    JSONArray messagesArray = new JSONArray();
    messagesArray.put(userMessage);

    // Create the "inferenceConfig" object with optional parameters
    JSONObject inferenceConfig = new JSONObject();
    inferenceConfig.put("max_new_tokens", awsBedrockParameters.getMaxTokenCount());
    inferenceConfig.put("temperature", awsBedrockParameters.getTemperature());
    inferenceConfig.put("top_p", awsBedrockParameters.getTopP());
    inferenceConfig.put("top_k", awsBedrockParameters.getTopK());

    // Combine everything into the root JSON object
    JSONObject rootObject = new JSONObject();
    rootObject.put("messages", messagesArray);
    rootObject.put("inferenceConfig", inferenceConfig);

    return rootObject.toString();
}

private static String getStabilityTitanText(String prompt) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("text", prompt);

    jsonRequest.put("text_prompts", textGenerationConfig);

    return jsonRequest.toString();
}


private static String getAnthropicClaudeText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens_to_sample", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
}

private static String getMistralAIText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
}


  private static String getAI21Text(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("topP", awsBedrockParameters.getTopP());
    jsonRequest.put("maxTokens", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}

private static String getCohereText(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("p", awsBedrockParameters.getTopP());
    jsonRequest.put("k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}

private static String getLlamaText(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("max_gen_len", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}



  private static String identifyPayload(String prompt, AwsbedrockParameters awsBedrockParameters){
    if (awsBedrockParameters.getModelName().contains("amazon.titan-text")) {
        return getAmazonTitanText(prompt, awsBedrockParameters);
    } else if ( awsBedrockParameters.getModelName().contains("amazon.nova")){
        return getAmazonNovaText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("anthropic.claude")) {
        return getAnthropicClaudeText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("ai21.j2")) {
        return getAI21Text(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("mistral.mistral")) {
        return getMistralAIText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("cohere.command")) {
        return getCohereText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("meta.llama")) {
        return getLlamaText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("mistral.")) {
        return getLlamaText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("mistral.")) {
        return getStabilityTitanText(prompt);
    } else {
        return "Unsupported model";
    }

  }

  private static BedrockRuntimeClient InitiateClient(AwsbedrockConfiguration configuration, AwsbedrockParameters awsBedrockParameters){
        // Initialize the AWS credentials
        //AwsBasicCredentials awsCreds = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey());
        // Create Bedrock Client 
        //return createClient(awsCreds, AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()));
        if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
            AwsBasicCredentials awsCredsBasic = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey());
            return createClient(awsCredsBasic, AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()));
        } else {
            AwsSessionCredentials awsCredsSession = AwsSessionCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey(), configuration.getAwsSessionToken());
            return createClientSession(awsCredsSession, AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()));
        }

  }


  private static String invokeModel(String prompt, AwsbedrockConfiguration configuration, AwsbedrockParameters awsBedrockParameters) {

    // Create Bedrock Client 
    BedrockRuntimeClient client = InitiateClient(configuration, awsBedrockParameters);

    String nativeRequest = identifyPayload(prompt, awsBedrockParameters);

    try {
        // Encode and send the request to the Bedrock Runtime.
        InvokeModelRequest request = createInvokeRequest(awsBedrockParameters.getModelName(), nativeRequest);

        //System.out.println("Native request: " + nativeRequest);

        InvokeModelResponse response = client.invokeModel(request);
        

        // Decode the response body.
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());


        //System.out.println(responseBody);
        // Retrieve the generated text from the model's response.
        //String text = new JSONPointer("/completions/0/data/text").queryFrom(responseBody).toString();
        //System.out.println(text);

        return responseBody.toString();

    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", awsBedrockParameters.getModelName(), e.getMessage());
        throw new RuntimeException(e);
    }
}


private static BedrockClient createBedrockClient(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {

    BedrockClient bedrockClient = BedrockClient.builder()
    .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
    .build();

    return bedrockClient;
}

private static BedrockAgentClient createBedrockAgentClient(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {

    BedrockAgentClient bedrockAgentClient = BedrockAgentClient.builder()
    .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
    .build();

    return bedrockAgentClient;
}


private static BedrockAgentRuntimeClient createBedrockAgentRuntimeClient(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentRuntimeClient bedrockAgentRuntimeClient = BedrockAgentRuntimeClient.builder()
    .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
    .build();

    return bedrockAgentRuntimeClient;
}


private static BedrockAgentRuntimeAsyncClient createBedrockAgentRuntimeAsyncClient(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeClient = BedrockAgentRuntimeAsyncClient.builder()
    .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
    .build();

    return bedrockAgentRuntimeClient;
}



private static AwsCredentials createAwsBasicCredentials(AwsbedrockConfiguration configuration){

    if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
        return AwsBasicCredentials.create(
            configuration.getAwsAccessKeyId(), 
            configuration.getAwsSecretAccessKey()
        );
    } else {

        return AwsSessionCredentials.create(
            configuration.getAwsAccessKeyId(), 
            configuration.getAwsSecretAccessKey(), 
            configuration.getAwsSessionToken());
    }
}



private static IamClient createIamClient(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {
    return IamClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
        .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()))
        .build();
}


public static String ListAgents(AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    String listOfAgents = getAgentNames(bedrockAgent);
    return listOfAgents;
}

private static String getAgentNames(BedrockAgentClient bedrockagent) {
        // Build a ListAgentsRequest instance without any filter criteria
        ListAgentsRequest listAgentsRequest = ListAgentsRequest.builder().build();

        // Call the listAgents method of the BedrockAgentClient instance
        ListAgentsResponse listAgentsResponse = bedrockagent.listAgents(listAgentsRequest);


        // Retrieve the list of agent summaries from the ListAgentsResponse instance
        List<AgentSummary> agentSummaries = listAgentsResponse.agentSummaries();

        // Extract the list of agent names from the list of agent summaries
        List<String> agentNames = agentSummaries.stream()
            .map(AgentSummary::agentName) // specify the type of the elements returned by the map() method
            .collect(Collectors.toList());

        // Create a JSONArray to store the agent names
        JSONArray jsonArray = new JSONArray(agentNames);

        // Create a JSONObject to store the JSONArray
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("agentNames", jsonArray);

        // Convert the JSONObject to a JSON string
        String jsonString = jsonObject.toString();
        return jsonString;

}


private static Agent getAgentById(String agentId, BedrockAgentClient bedrockAgentClient) {
    // Build a GetAgentRequest instance with the agent ID
    GetAgentRequest getAgentRequest = GetAgentRequest.builder()
            .agentId(agentId)
            .build();

    // Call the getAgent method of the BedrockAgentClient instance
    GetAgentResponse getAgentResponse = bedrockAgentClient.getAgent(getAgentRequest);

    // Retrieve the agent from the GetAgentResponse instance
    Agent agent = getAgentResponse.agent();

    return agent;
}

public static String getAgentbyAgentId(String agentId, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    Agent agent = getAgentById(agentId, bedrockAgent);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("agentId", agent.agentId());
    jsonObject.put("agentName", agent.agentName());
    jsonObject.put("agentArn", agent.agentArn());
    jsonObject.put("agentStatus", agent.agentStatusAsString());
    jsonObject.put("agentResourceRoleArn", agent.agentResourceRoleArn());
    jsonObject.put("clientToken", agent.clientToken());
    jsonObject.put("createdAt", agent.createdAt().toString());
    jsonObject.put("description", agent.description());
    jsonObject.put("foundationModel", agent.foundationModel());
    jsonObject.put("idleSessionTTLInSeconds", agent.idleSessionTTLInSeconds());
    jsonObject.put("instruction", agent.instruction());
    jsonObject.put("promptOverrideConfiguration", agent.promptOverrideConfiguration());
    jsonObject.put("updatedAt", agent.updatedAt().toString());

    return jsonObject.toString();
}


private static Optional<Agent> getAgentByName(String agentName, BedrockAgentClient bedrockAgentClient) {
    // Build a ListAgentsRequest instance without any filter criteria
    ListAgentsRequest listAgentsRequest = ListAgentsRequest.builder()
            .build();

    // Call the listAgents method of the BedrockAgentClient instance
    ListAgentsResponse listAgentsResponse = bedrockAgentClient.listAgents(listAgentsRequest);

    // Retrieve the list of agent summaries from the ListAgentsResponse instance
    List<AgentSummary> agentSummaries = listAgentsResponse.agentSummaries();

    // Iterate through the list of agent summaries to find the one with the specified name
    for (AgentSummary agentSummary : agentSummaries) {
        if (agentSummary.agentName().equals(agentName)) {
            // Retrieve the agent ID from the agent summary
            String agentId = agentSummary.agentId();

            // Build a GetAgentRequest instance with the agent ID
            GetAgentRequest getAgentRequest = GetAgentRequest.builder()
                    .agentId(agentId)
                    .build();

            // Call the getAgent method of the BedrockAgentClient instance
            GetAgentResponse getAgentResponse = bedrockAgentClient.getAgent(getAgentRequest);

            // Retrieve the agent from the GetAgentResponse instance
            Agent agent = getAgentResponse.agent();

            // Return the agent as an Optional object
            return Optional.of(agent);
        }
    }

    // No agent with the specified name was found
    return Optional.empty();
}

public static String getAgentbyAgentName(String agentName, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    String agentInfo = "";
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    Optional<Agent> optionalAgent = getAgentByName(agentName, bedrockAgent);
    if (optionalAgent.isPresent()) {
        Agent agent = optionalAgent.get();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("agentId", agent.agentId());
        jsonObject.put("agentName", agent.agentName());
        jsonObject.put("agentArn", agent.agentArn());
        jsonObject.put("agentStatus", agent.agentStatusAsString());
        jsonObject.put("agentResourceRoleArn", agent.agentResourceRoleArn());
        jsonObject.put("clientToken", agent.clientToken());
        jsonObject.put("createdAt", agent.createdAt().toString());
        jsonObject.put("description", agent.description());
        jsonObject.put("foundationModel", agent.foundationModel());
        jsonObject.put("idleSessionTTLInSeconds", agent.idleSessionTTLInSeconds());
        jsonObject.put("instruction", agent.instruction());
        jsonObject.put("promptOverrideConfiguration", agent.promptOverrideConfiguration());
        jsonObject.put("updatedAt", agent.updatedAt().toString());
        return jsonObject.toString();
    } else {
        agentInfo = "No Agent found!";
        return agentInfo;
    }
}


public static String createAgentOperation(String name, String instruction, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameter) {
    String postfix = "muc";
    String RolePolicyName = "agent_permissions";

    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameter);

    Role agentRole = createAgentRole(postfix, RolePolicyName, configuration, awsBedrockParameter);

    Agent agent = createAgent(name, instruction, awsBedrockParameter.getModelName(),agentRole, bedrockAgent);


    PrepareAgentResponse agentDetails = prepareAgent(agent.agentId(), bedrockAgent);

    //AgentAlias AgentAlias = createAgentAlias(name, agent.agentId(),bedrockAgent);

    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("agentId", agentDetails.agentId());
    jsonRequest.put("agentVersion", agentDetails.agentVersion());
    jsonRequest.put("agentStatus", agentDetails.agentStatusAsString());
    jsonRequest.put("preparedAt", agentDetails.preparedAt().toString());
    jsonRequest.put("agentArn", agent.agentArn());
    jsonRequest.put("agentName", agent.agentName());
    jsonRequest.put("agentResourceRoleArn", agent.agentResourceRoleArn());
    jsonRequest.put("clientToken", agent.clientToken());
    jsonRequest.put("createdAt", agent.createdAt().toString());
    jsonRequest.put("description", agent.description());
    jsonRequest.put("foundationModel", agent.foundationModel());
    jsonRequest.put("idleSessionTTLInSeconds", agent.idleSessionTTLInSeconds());
    jsonRequest.put("instruction", agent.instruction());
    jsonRequest.put("promptOverrideConfiguration", agent.promptOverrideConfiguration());
    jsonRequest.put("updatedAt", agent.updatedAt().toString());
    return jsonRequest.toString();

    //return agentDetails.toString();
}




public static String createAgentAlias(String name, String agentId, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameter){

    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameter);
    AgentAlias agentAlias = createAgentAlias(name, agentId,bedrockAgent);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("agentAliasId", agentAlias.agentAliasId());
    jsonObject.put("agentAliasName", agentAlias.agentAliasName());
    jsonObject.put("agentAliasArn", agentAlias.agentAliasArn());
    jsonObject.put("clientToken", agentAlias.clientToken());
    jsonObject.put("createdAt", agentAlias.createdAt().toString());
    jsonObject.put("updatedAt", agentAlias.updatedAt().toString());

    return jsonObject.toString();
}



private static Role createAgentRole(String postfix, String RolePolicyName, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {
    String roleName = "AmazonBedrockExecutionRoleForAgents_" + postfix;
    String modelArn = "arn:aws:bedrock:" + awsBedrockParameters.getRegion() + "::foundation-model/" + awsBedrockParameters.getModelName() + "*";
    String ROLE_POLICY_NAME = RolePolicyName;

    System.out.println("Creating an execution role for the agent...");

    // Create an IAM client
    IamClient iamClient = createIamClient(configuration, awsBedrockParameters);
    // Check if the role exists
    Role agentRole = null;
    try {
        GetRoleResponse getRoleResponse = iamClient.getRole(GetRoleRequest.builder().roleName(roleName).build());
        agentRole = getRoleResponse.role();
        System.out.println("Role already exists: " + agentRole.arn());
    } catch (NoSuchEntityException e) {
        // Role does not exist, create it
        try {
            CreateRoleResponse createRoleResponse = iamClient.createRole(CreateRoleRequest.builder()
                    .roleName(roleName)
                    .assumeRolePolicyDocument("{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Principal\": {\"Service\": \"bedrock.amazonaws.com\"},\"Action\": \"sts:AssumeRole\"}]}")
                    .build());

            
            System.out.println(modelArn);
            //String policyDocument = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Sid\": \"Agents for Amazon Bedrock permissions\",\"Effect\": \"Allow\",\"Action\": [\"bedrock:ListFoundationModels\",\"bedrock:GetFoundationModel\",\"bedrock:TagResource\",\"bedrock:UntagResource\",\"bedrock:ListTagsForResource\",\"bedrock:CreateAgent\",\"bedrock:UpdateAgent\",\"bedrock:GetAgent\",\"bedrock:ListAgents\",\"bedrock:DeleteAgent\",\"bedrock:CreateAgentActionGroup\",\"bedrock:UpdateAgentActionGroup\",\"bedrock:GetAgentActionGroup\",\"bedrock:ListAgentActionGroups\",\"bedrock:DeleteAgentActionGroup\",\"bedrock:GetAgentVersion\",\"bedrock:ListAgentVersions\",\"bedrock:DeleteAgentVersion\",\"bedrock:CreateAgentAlias\",\"bedrock:UpdateAgentAlias\",\"bedrock:GetAgentAlias\",\"bedrock:ListAgentAliases\",\"bedrock:DeleteAgentAlias\",\"bedrock:AssociateAgentKnowledgeBase\",\"bedrock:DisassociateAgentKnowledgeBase\",\"bedrock:GetKnowledgeBase\",\"bedrock:ListKnowledgeBases\",\"bedrock:PrepareAgent\",\"bedrock:InvokeAgent\",\"bedrock:InvokeModel\"],\"Resource\": \"" + modelArn + "\"}]}";
            String policyDocument = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Action\": [\"bedrock:ListFoundationModels\",\"bedrock:GetFoundationModel\",\"bedrock:TagResource\",\"bedrock:UntagResource\",\"bedrock:ListTagsForResource\",\"bedrock:CreateAgent\",\"bedrock:UpdateAgent\",\"bedrock:GetAgent\",\"bedrock:ListAgents\",\"bedrock:DeleteAgent\",\"bedrock:CreateAgentActionGroup\",\"bedrock:UpdateAgentActionGroup\",\"bedrock:GetAgentActionGroup\",\"bedrock:ListAgentActionGroups\",\"bedrock:DeleteAgentActionGroup\",\"bedrock:GetAgentVersion\",\"bedrock:ListAgentVersions\",\"bedrock:DeleteAgentVersion\",\"bedrock:CreateAgentAlias\",\"bedrock:UpdateAgentAlias\",\"bedrock:GetAgentAlias\",\"bedrock:ListAgentAliases\",\"bedrock:DeleteAgentAlias\",\"bedrock:AssociateAgentKnowledgeBase\",\"bedrock:DisassociateAgentKnowledgeBase\",\"bedrock:GetKnowledgeBase\",\"bedrock:ListKnowledgeBases\",\"bedrock:PrepareAgent\",\"bedrock:InvokeAgent\",\"bedrock:InvokeModel\"],\"Resource\": \"*\"}]}";
            System.out.println(policyDocument);
            iamClient.putRolePolicy(PutRolePolicyRequest.builder()
                    .roleName(roleName)
                    .policyName(ROLE_POLICY_NAME)
                    //.policyDocument("{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Action\": \"bedrock:InvokeModel\",\"Resource\": \"" + modelArn + "\"}]}")
                    .policyDocument(policyDocument)
                    .build());

            agentRole = Role.builder()
                    .roleName(roleName)
                    .arn(createRoleResponse.role().arn())
                    .build();
        } catch (IamException ex) {
            System.out.println("Couldn't create role " + roleName + ". Here's why: " + ex.getMessage());
            throw ex;
        }
    }
    return agentRole;
}

private static Agent createAgent(String name, String instruction, String modelId, Role agentRole, BedrockAgentClient bedrockAgentClient) {
    System.out.println("Creating the agent...");

    //String instruction = "You are a friendly chat bot. You have access to a function called that returns information about the current date and time. When responding with date or time, please make sure to add the timezone UTC.";
    CreateAgentResponse createAgentResponse = bedrockAgentClient.createAgent(CreateAgentRequest.builder()
            .agentName(name)
            .foundationModel(modelId)
            .instruction(instruction)
            .agentResourceRoleArn(agentRole.arn())
            .build());

    waitForAgentStatus(createAgentResponse.agent().agentId(), AgentStatus.NOT_PREPARED.toString(), bedrockAgentClient);

    return createAgentResponse.agent();
}


private static void waitForAgentStatus(String agentId, String status, BedrockAgentClient bedrockAgentClient) {
    while (true) {
        GetAgentResponse response = bedrockAgentClient.getAgent(GetAgentRequest.builder()
                .agentId(agentId)
                .build());
        
        //System.out.println("Status: " + status);
        //System.out.println("response.agent.agentStatus: " + response.agent().agentStatus().toString());

        if (response.agent().agentStatus().toString().equals(status)) {

            break;
        }

        try {
            System.out.println("Waiting for agent get prepared...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}



private static PrepareAgentResponse prepareAgent(String agentId, BedrockAgentClient bedrockAgentClient) {
    System.out.println("Preparing the agent...");

    //String agentId = agent.agentId();
    PrepareAgentResponse preparedAgentDetails = bedrockAgentClient.prepareAgent(PrepareAgentRequest.builder()
            .agentId(agentId)
            .build());
    waitForAgentStatus(agentId, "PREPARED", bedrockAgentClient);

    return preparedAgentDetails;
}


private static AgentAlias createAgentAlias(String alias, String agentId, BedrockAgentClient bedrockAgentClient) {
    System.out.println("Creating an agent alias...");    
    System.out.println("agentId: " + agentId);
    CreateAgentAliasRequest request = CreateAgentAliasRequest.builder()
            .agentId(agentId)
            .agentAliasName(alias)
            .build();   


    CreateAgentAliasResponse response = bedrockAgentClient.createAgentAlias(request);

    return response.agentAlias();
}


public static String listAllAgentAliases(String agentId, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliases(agentId, bedrockAgent);

    JSONArray jsonArray = new JSONArray();
    for (AgentAliasSummary agentAliasSummary : agentAliasSummaries) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("agentAliasId", agentAliasSummary.agentAliasId());
        jsonObject.put("agentAliasName", agentAliasSummary.agentAliasName());
        jsonObject.put("createdAt", agentAliasSummary.createdAt().toString());
        jsonObject.put("updatedAt", agentAliasSummary.updatedAt().toString());
        jsonArray.put(jsonObject);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("agentAliasSummaries", jsonArray);

    return jsonObject.toString();
}

private static List<AgentAliasSummary> listAgentAliases(String agentId, BedrockAgentClient bedrockAgentClient) {
    // Build a ListAgentAliasesRequest instance with the agent ID
    ListAgentAliasesRequest listAgentAliasesRequest = ListAgentAliasesRequest.builder()
            .agentId(agentId)
            .build();

    // Call the listAgentAliases method of the BedrockAgentClient instance
    ListAgentAliasesResponse listAgentAliasesResponse = bedrockAgentClient.listAgentAliases(listAgentAliasesRequest);

    // Retrieve the list of agent alias summaries from the ListAgentAliasesResponse instance
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliasesResponse.agentAliasSummaries();

    return agentAliasSummaries;
}

public static String deleteAgentAliasesByAgentId(String agentId, String agentAliasName, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    DeleteAgentAliasResponse response = deleteAgentAliasByName(agentId, agentAliasName, bedrockAgent);
   
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("agentId", response.agentId());
    jsonObject.put("agentAliasId", response.agentAliasId());
    jsonObject.put("agentAliasStatus", response.agentAliasStatus().toString());
    jsonObject.put("agentStatus", response.agentAliasStatusAsString());

    return jsonObject.toString();
}


private static DeleteAgentAliasResponse deleteAgentAliasByName(String agentId, String agentAliasName, BedrockAgentClient bedrockAgentClient) {
    DeleteAgentAliasResponse response=null;
    
    // Build a ListAgentAliasesRequest instance with the agent ID
    ListAgentAliasesRequest listAgentAliasesRequest = ListAgentAliasesRequest.builder()
            .agentId(agentId)
            .build();

    // Call the listAgentAliases method of the BedrockAgentClient instance
    ListAgentAliasesResponse listAgentAliasesResponse = bedrockAgentClient.listAgentAliases(listAgentAliasesRequest);

    // Retrieve the list of agent alias summaries from the ListAgentAliasesResponse instance
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliasesResponse.agentAliasSummaries();

    // Iterate through the list of agent alias summaries to find the one with the specified name
    Optional<AgentAliasSummary> agentAliasSummary = agentAliasSummaries.stream()
            .filter(alias -> alias.agentAliasName().equals(agentAliasName))
            .findFirst();

    // If the agent alias was found, delete it
    if (agentAliasSummary.isPresent()) {
        String agentAliasId = agentAliasSummary.get().agentAliasId();

        // Build a DeleteAgentAliasRequest instance with the agent ID and agent alias ID
        DeleteAgentAliasRequest deleteAgentAliasRequest = DeleteAgentAliasRequest.builder()
                .agentId(agentId)
                .agentAliasId(agentAliasId)
                .build();

        // Call the deleteAgentAlias method of the BedrockAgentClient instance
        DeleteAgentAliasResponse deleteAgentAliasResponse = bedrockAgentClient.deleteAgentAlias(deleteAgentAliasRequest);

        System.out.println("Agent alias with name " + agentAliasName + " deleted successfully.");
        response = deleteAgentAliasResponse;
    } else {
        System.out.println("No agent alias with name " + agentAliasName + " found.");
    }
    return response;
}


public static String deleteAgentByAgentId(String agentId, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    DeleteAgentResponse response = deleteAgentById(agentId, bedrockAgent);
    
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("agentId", response.agentId());
    jsonObject.put("agentStatus", response.agentStatusAsString());

    return jsonObject.toString();
}


private static DeleteAgentResponse deleteAgentById(String agentId, BedrockAgentClient bedrockAgentClient) {
    // Build a DeleteAgentRequest instance with the agent ID
    DeleteAgentRequest deleteAgentRequest = DeleteAgentRequest.builder()
            .agentId(agentId)
            .build();

    // Call the deleteAgent method of the BedrockAgentClient instance
    DeleteAgentResponse deleteAgentResponse = bedrockAgentClient.deleteAgent(deleteAgentRequest);

    // Print a message indicating that the agent was deleted successfully
    return deleteAgentResponse;
}



/*

private void chatWithAgent(AgentAlias agentAlias, BedrockAgentRuntimeClient bedrockAgentRuntimeClient) {
    System.out.println(String.join("", Collections.nCopies(88, "-")));
    System.out.println("The agent is ready to chat.");
    System.out.println("Try asking for the date or time. Type 'exit' to quit.");

    // Create a unique session ID for the conversation
    String sessionId = UUID.randomUUID().toString();

    Scanner scanner = new Scanner(System.in);
    while (true) {
        System.out.print("Prompt: ");
        String prompt = scanner.nextLine();

        if (prompt.equals("exit")) {
            break;
        }

        String response = invokeAgent(agentAlias, agentId, prompt, sessionId, bedrockAgentRuntimeClient);

        System.out.println("Agent: " + response);
    }
}

*/



public static String chatWithAgent(String agentAlias, String agentId, String sessionId, String prompt, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters){
    BedrockAgentRuntimeAsyncClient bedrockAgent = createBedrockAgentRuntimeAsyncClient(configuration, awsBedrockParameters);

    if (sessionId != null && !sessionId.isEmpty()) {
        System.out.println("Using provided sessionId: " + sessionId);
    } else {
        sessionId = UUID.randomUUID().toString();
        System.out.println("Generated new sessionId: " + sessionId);
    }
    
    //String sessionId = UUID.randomUUID().toString();
    //CompletableFuture<String> completableFuture=null;
    String response = "";
    try {
        //completableFuture = invokeAgent(agentAlias, agentId, prompt, sessionId, bedrockAgent);
        /*invokeAgent(agentAlias, agentId, prompt, sessionId, bedrockAgent).thenAccept(response -> {
            System.out.println(response);
        });*/

        response = invokeAgent(agentAlias, agentId, prompt, sessionId, bedrockAgent).get();
        System.out.println(response);

    } catch (InterruptedException | ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println(e.getMessage());
    }

    return response.substring(4);
}

private static CompletableFuture<String> invokeAgent(String agentAlias, String agentId, String prompt, String sessionId, BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient) throws InterruptedException, ExecutionException {
    InvokeAgentRequest request = InvokeAgentRequest.builder()
            .agentId(agentId)
            .agentAliasId(agentAlias)
            .sessionId(sessionId)
            .inputText(prompt)
            .build();

    CompletableFuture<String> completionFuture = new CompletableFuture<>();

    InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
            .onChunk(chunk -> {
                SdkBytes bytes = chunk.bytes();
                String text = new String(bytes.asByteArray(), StandardCharsets.UTF_8);
                completionFuture.complete(completionFuture.getNow(null) + text);
            })
            .build();

    InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
            .subscriber(visitor)
            .build();

    bedrockAgentRuntimeAsyncClient.invokeAgent(request, handler).get();



    
    return completionFuture;
}



}