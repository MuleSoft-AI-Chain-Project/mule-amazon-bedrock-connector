package org.mule.extension.mulechain.helpers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.agents.AwsbedrockAgentsParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagent.model.Agent;

import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentRequest;
import software.amazon.awssdk.services.bedrockagent.model.DeleteAgentResponse;
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

import javax.net.ssl.TrustManagerFactory;

public class AwsbedrockAgentsPayloadHelper {

    private static final Logger logger = LoggerFactory.getLogger(AwsbedrockAgentsPayloadHelper.class);

    private static BedrockAgentClient createBedrockAgentClient(
            AwsbedrockConfiguration configuration,
            AwsbedrockAgentsParameters awsBedrockParameters) {

        BedrockAgentClientBuilder bedrockAgentClientBuilder = BedrockAgentClient.builder()
                .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()))
                .fipsEnabled(configuration.getFipsModeEnabled())
                .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)));

        if (configuration.getProxyConfig() != null) {
            ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();

            software.amazon.awssdk.http.apache.ProxyConfiguration proxyConfig = software.amazon.awssdk.http.apache.ProxyConfiguration.builder()
                    .endpoint(URI.create(String.format("%s://%s:%d",
                            configuration.getProxyConfig().getScheme(),
                            configuration.getProxyConfig().getHost(),
                            configuration.getProxyConfig().getPort())))
                    .username(configuration.getProxyConfig().getUsername())
                    .password(configuration.getProxyConfig().getPassword())
                    .nonProxyHosts(configuration.getProxyConfig().getNonProxyHosts())
                    .build();

            httpClientBuilder.proxyConfiguration(proxyConfig);

            // Configure truststore if available
            if (configuration.getProxyConfig().getTrustStorePath() != null) {
                TlsTrustManagersProvider tlsTrustManagersProvider = createTlsKeyManagersProvider(
                            configuration.getProxyConfig().getTrustStorePath(),
                            configuration.getProxyConfig().getTrustStorePassword(),
                            configuration.getProxyConfig().getTrustStoreType().name()
                );

                httpClientBuilder.tlsTrustManagersProvider(tlsTrustManagersProvider);
            }

            bedrockAgentClientBuilder.httpClient(httpClientBuilder.build());
        }

        return bedrockAgentClientBuilder.build();
    }

    private static BedrockAgentRuntimeAsyncClient createBedrockAgentRuntimeAsyncClient(
            AwsbedrockConfiguration configuration,
            AwsbedrockAgentsParameters awsBedrockParameters) {

        BedrockAgentRuntimeAsyncClientBuilder clientBuilder = BedrockAgentRuntimeAsyncClient.builder()
                .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()))
                .fipsEnabled(configuration.getFipsModeEnabled())
                .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)));

        // Configure HTTP client if proxy or truststore is needed
        if (configuration.getProxyConfig() != null) {
            NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();

            software.amazon.awssdk.http.nio.netty.ProxyConfiguration proxyConfig =
                    software.amazon.awssdk.http.nio.netty.ProxyConfiguration.builder()
                            .host(configuration.getProxyConfig().getHost())
                            .port(configuration.getProxyConfig().getPort())
                            .username(configuration.getProxyConfig().getUsername())
                            .password(configuration.getProxyConfig().getPassword())
                            .nonProxyHosts(configuration.getProxyConfig().getNonProxyHosts())
                            .build();

            httpClientBuilder.proxyConfiguration(proxyConfig);

            // Configure truststore if available
            if (configuration.getProxyConfig().getTrustStorePath() != null) {
                TlsTrustManagersProvider tlsTrustManagersProvider = createTlsKeyManagersProvider(
                        configuration.getProxyConfig().getTrustStorePath(),
                        configuration.getProxyConfig().getTrustStorePassword(),
                        configuration.getProxyConfig().getTrustStoreType().name()
                );

                httpClientBuilder.tlsTrustManagersProvider(tlsTrustManagersProvider);
            }

            clientBuilder.httpClient(httpClientBuilder.build());
        }

        return clientBuilder.build();
    }

    private static TlsTrustManagersProvider createTlsKeyManagersProvider(String trustStorePath, String trustStorePassword, String trustStoreType) {
        try {
            // Load the truststore (supports JKS, PKCS12, etc.)
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, trustStorePassword != null ? trustStorePassword.toCharArray() : null);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return () -> trustManagerFactory.getTrustManagers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure JKS truststore", e);
        }
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

    logger.info("Creating an execution role for the agent...");

    // Create an IAM client
    IamClient iamClient = createIamClient(configuration, awsBedrockParameters);
    // Check if the role exists
    Role agentRole = null;
    try {
        GetRoleResponse getRoleResponse = iamClient.getRole(GetRoleRequest.builder().roleName(roleName).build());
        agentRole = getRoleResponse.role();
        logger.info("Role already exists: {}", agentRole.arn());
    } catch (NoSuchEntityException e) {
        // Role does not exist, create it
        try {
            CreateRoleResponse createRoleResponse = iamClient.createRole(CreateRoleRequest.builder()
                    .roleName(roleName)
                    .assumeRolePolicyDocument("{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Principal\": {\"Service\": \"bedrock.amazonaws.com\"},\"Action\": \"sts:AssumeRole\"}]}")
                    .build());

            
            logger.info("Model ARN: {}", modelArn);
            //String policyDocument = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Sid\": \"Agents for Amazon Bedrock permissions\",\"Effect\": \"Allow\",\"Action\": [\"bedrock:ListFoundationModels\",\"bedrock:GetFoundationModel\",\"bedrock:TagResource\",\"bedrock:UntagResource\",\"bedrock:ListTagsForResource\",\"bedrock:CreateAgent\",\"bedrock:UpdateAgent\",\"bedrock:GetAgent\",\"bedrock:ListAgents\",\"bedrock:DeleteAgent\",\"bedrock:CreateAgentActionGroup\",\"bedrock:UpdateAgentActionGroup\",\"bedrock:GetAgentActionGroup\",\"bedrock:ListAgentActionGroups\",\"bedrock:DeleteAgentActionGroup\",\"bedrock:GetAgentVersion\",\"bedrock:ListAgentVersions\",\"bedrock:DeleteAgentVersion\",\"bedrock:CreateAgentAlias\",\"bedrock:UpdateAgentAlias\",\"bedrock:GetAgentAlias\",\"bedrock:ListAgentAliases\",\"bedrock:DeleteAgentAlias\",\"bedrock:AssociateAgentKnowledgeBase\",\"bedrock:DisassociateAgentKnowledgeBase\",\"bedrock:GetKnowledgeBase\",\"bedrock:ListKnowledgeBases\",\"bedrock:PrepareAgent\",\"bedrock:InvokeAgent\",\"bedrock:InvokeModel\"],\"Resource\": \"" + modelArn + "\"}]}";
            String policyDocument = "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Action\": [\"bedrock:ListFoundationModels\",\"bedrock:GetFoundationModel\",\"bedrock:TagResource\",\"bedrock:UntagResource\",\"bedrock:ListTagsForResource\",\"bedrock:CreateAgent\",\"bedrock:UpdateAgent\",\"bedrock:GetAgent\",\"bedrock:ListAgents\",\"bedrock:DeleteAgent\",\"bedrock:CreateAgentActionGroup\",\"bedrock:UpdateAgentActionGroup\",\"bedrock:GetAgentActionGroup\",\"bedrock:ListAgentActionGroups\",\"bedrock:DeleteAgentActionGroup\",\"bedrock:GetAgentVersion\",\"bedrock:ListAgentVersions\",\"bedrock:DeleteAgentVersion\",\"bedrock:CreateAgentAlias\",\"bedrock:UpdateAgentAlias\",\"bedrock:GetAgentAlias\",\"bedrock:ListAgentAliases\",\"bedrock:DeleteAgentAlias\",\"bedrock:AssociateAgentKnowledgeBase\",\"bedrock:DisassociateAgentKnowledgeBase\",\"bedrock:GetKnowledgeBase\",\"bedrock:ListKnowledgeBases\",\"bedrock:PrepareAgent\",\"bedrock:InvokeAgent\",\"bedrock:InvokeModel\"],\"Resource\": \"*\"}]}";
            logger.info("Policy Document: {}", policyDocument);
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
            logger.error("Couldn't create role {}. Here's why: {}", roleName, ex.getMessage(), ex);
            throw ex;
        }
    }
    return agentRole;
}

private static Agent createAgent(String name, String instruction, String modelId, Role agentRole, BedrockAgentClient bedrockAgentClient) {
    logger.info("Creating the agent...");

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

        if (response.agent().agentStatus().toString().equals(status)) {

            break;
        }

        try {
            logger.info("Waiting for agent get prepared...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}



private static PrepareAgentResponse prepareAgent(String agentId, BedrockAgentClient bedrockAgentClient) {
    logger.info("Preparing the agent...");

    //String agentId = agent.agentId();
    PrepareAgentResponse preparedAgentDetails = bedrockAgentClient.prepareAgent(PrepareAgentRequest.builder()
            .agentId(agentId)
            .build());
    waitForAgentStatus(agentId, "PREPARED", bedrockAgentClient);

    return preparedAgentDetails;
}


private static AgentAlias createAgentAlias(String alias, String agentId, BedrockAgentClient bedrockAgentClient) {
    logger.info("Creating an agent alias for agentId: {}", agentId);
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

        logger.info("Agent alias with name " + agentAliasName + " deleted successfully.");
        response = deleteAgentAliasResponse;
    } else {
        logger.info("No agent alias with name " + agentAliasName + " found.");
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

    public static String chatWithAgent(String agentAlias, String agentId, String sessionId, String prompt, AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {

        BedrockAgentRuntimeAsyncClient bedrockAgent = createBedrockAgentRuntimeAsyncClient(configuration, awsBedrockParameters);

        String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : UUID.randomUUID().toString();
        logger.info("Using sessionId: {}", effectiveSessionId);

        try {
            return invokeAgent(agentAlias, agentId, prompt, effectiveSessionId, bedrockAgent)
                    .thenApply(response -> {
                        logger.debug(response);
                        return response;
                    })
                    .exceptionally(e -> {
                        logger.error("Error during agent invocation: {}", e.getMessage(), e);
                        throw new CompletionException("Failed to chat with agent", e);
                    })
                    .join();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static CompletableFuture<String> invokeAgent(String agentAlias, String agentId, String prompt, String sessionId, BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient) throws InterruptedException, ExecutionException {
        InvokeAgentRequest request = InvokeAgentRequest.builder()
                .agentId(agentId)
                .agentAliasId(agentAlias)
                .sessionId(sessionId)
                .inputText(prompt)
                .enableTrace(false)
                .build();

        CompletableFuture<String> completionFuture = new CompletableFuture<>();

        // Thread-safe collection to store different chunks
        List<JSONObject> chunks = Collections.synchronizedList(new ArrayList<>());

        InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
                .onChunk(chunk -> {
                    try {
                        JSONObject chunkData = new JSONObject();
                        chunkData.put("type", "chunk");
                        chunkData.put("timestamp", System.currentTimeMillis());

                        if (chunk.bytes() != null) {
                            String text = new String(chunk.bytes().asByteArray(), StandardCharsets.UTF_8);
                            chunkData.put("text", text);
                        }

                        // Add attribution/citations if present
                        if (chunk.attribution() != null && chunk.attribution().citations() != null) {
                            JSONArray citationsArray = new JSONArray();
                            chunk.attribution().citations().forEach(citation -> {
                                JSONObject citationData = new JSONObject();

                                if (citation.generatedResponsePart() != null && citation.generatedResponsePart().textResponsePart() != null) {
                                    citationData.put("generatedResponsePart", citation.generatedResponsePart().textResponsePart().text());
                                }

                                if (citation.retrievedReferences() != null) {
                                    JSONArray referencesArray = new JSONArray();
                                    citation.retrievedReferences().forEach(ref -> {
                                        JSONObject refData = new JSONObject();
                                        if (ref.content() != null && ref.content().text() != null) {
                                            refData.put("content", ref.content().text());
                                        }
                                        if (ref.location() != null) {
                                            refData.put("location", ref.location().toString());
                                        }
                                        if (ref.metadata() != null) {
                                            JSONObject metadataObject = new JSONObject(ref.metadata());
                                            refData.put("metadata", metadataObject);
                                        }
                                        referencesArray.put(refData);
                                    });
                                    citationData.put("retrievedReferences", referencesArray);
                                }
                                citationsArray.put(citationData);
                            });
                            chunkData.put("citations", citationsArray);
                        }

                        chunks.add(chunkData);
                    } catch (Exception e) {
                        logger.error("Error processing chunk: {}", e.getMessage(), e);
                    }
                })
                .build();

        InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
                .subscriber(visitor)
                .build();

        CompletableFuture<Void> invocationFuture = bedrockAgentRuntimeAsyncClient.invokeAgent(request, handler);

        invocationFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                completionFuture.completeExceptionally(throwable);
            } else {
                try {
                    JSONObject finalResult = new JSONObject();
                    finalResult.put("sessionId", sessionId);
                    finalResult.put("agentId", agentId);
                    finalResult.put("agentAlias", agentAlias);
                    finalResult.put("prompt", prompt);
                    finalResult.put("processedAt", System.currentTimeMillis());
                    finalResult.put("chunks", new JSONArray(chunks));

                    // Add summary statistics
                    JSONObject summary = new JSONObject();
                    summary.put("totalChunks", chunks.size());

                    // Concatenate all chunk text for full response
                    StringBuilder fullText = new StringBuilder();
                    chunks.forEach(chunk -> {
                        if (chunk.has("text")) {
                            fullText.append(chunk.getString("text"));
                        }
                    });
                    summary.put("fullResponse", fullText.toString());

                    finalResult.put("summary", summary);

                    String finalJson = finalResult.toString(4);
                    completionFuture.complete(finalJson);
                } catch (Exception e) {
                    logger.error("Error creating final JSON: {}", e.getMessage(), e);
                    completionFuture.completeExceptionally(e);
                }
            }
        });

        return completionFuture;
    }
}