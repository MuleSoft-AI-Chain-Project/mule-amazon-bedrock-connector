package org.mule.extension.mulechain.helpers;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.net.ssl.TrustManagerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.agents.AwsbedrockAgentsParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagent.model.Agent;
import software.amazon.awssdk.services.bedrockagent.model.AgentAlias;
import software.amazon.awssdk.services.bedrockagent.model.AgentAliasSummary;
import software.amazon.awssdk.services.bedrockagent.model.AgentStatus;
import software.amazon.awssdk.services.bedrockagent.model.AgentSummary;
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
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.PayloadPart;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;

public class AwsbedrockAgentsPayloadHelper {

  private static final Logger logger = LoggerFactory.getLogger(AwsbedrockAgentsPayloadHelper.class);

  // JSON Keys
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
  private static final String AGENT_ALIAS_ID = "agentAliasId";
  private static final String AGENT_ALIAS_NAME = "agentAliasName";
  private static final String AGENT_ALIAS_ARN = "agentAliasArn";
  private static final String AGENT_ALIAS_SUMMARIES = "agentAliasSummaries";
  private static final String AGENT_ALIAS_STATUS = "agentAliasStatus";
  private static final String AGENT_VERSION = "agentVersion";
  private static final String PREPARED_AT = "preparedAt";
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

  // Messages
  private static final String NO_AGENT_FOUND = "No Agent found!";

  // IAM and Agent Configuration
  private static final String AGENT_EXECUTION_ROLE_PREFIX = "AmazonBedrockExecutionRoleForAgents_";
  private static final String AGENT_ROLE_POLICY_NAME = "agent_permissions";
  private static final String AGENT_POSTFIX = "muc";
  private static final long AGENT_STATUS_CHECK_INTERVAL_MS = 2000;

  private static final AtomicInteger eventCounter = new AtomicInteger(0);

  private static BedrockAgentClient createBedrockAgentClient(
                                                             AwsbedrockConfiguration configuration,
                                                             AwsbedrockAgentsParameters awsBedrockParameters) {

    BedrockAgentClientBuilder bedrockAgentClientBuilder = BedrockAgentClient.builder()
        .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()))
        .fipsEnabled(configuration.getFipsModeEnabled())
        .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)));

    if (configuration.getProxyConfig() != null) {
      ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();

      software.amazon.awssdk.http.apache.ProxyConfiguration proxyConfig = software.amazon.awssdk.http.apache.ProxyConfiguration
          .builder()
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
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStorePath(),
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStorePassword(),
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStoreType().name());

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
          software.amazon.awssdk.http.nio.netty.ProxyConfiguration
              .builder()
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
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStorePath(),
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStorePassword(),
                                                                                         configuration.getProxyConfig()
                                                                                             .getTrustStoreType().name());

        httpClientBuilder.tlsTrustManagersProvider(tlsTrustManagersProvider);
      }

      clientBuilder.httpClient(httpClientBuilder.build());
    }

    return clientBuilder.build();
  }

  private static TlsTrustManagersProvider createTlsKeyManagersProvider(String trustStorePath,
                                                                       String trustStorePassword, String trustStoreType) {
    try {
      // Load the truststore (supports JKS, PKCS12, etc.)
      KeyStore trustStore = KeyStore.getInstance(trustStoreType);
      try (FileInputStream fis = new FileInputStream(trustStorePath)) {
        trustStore.load(fis, trustStorePassword != null ? trustStorePassword.toCharArray() : null);
      }

      TrustManagerFactory trustManagerFactory = TrustManagerFactory
          .getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);

      return () -> trustManagerFactory.getTrustManagers();
    } catch (Exception e) {
      throw new RuntimeException("Failed to configure JKS truststore", e);
    }
  }

  private static AwsCredentials createAwsBasicCredentials(AwsbedrockConfiguration configuration) {

    if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
      return AwsBasicCredentials.create(
                                        configuration.getAwsAccessKeyId(),
                                        configuration.getAwsSecretAccessKey());
    } else {

      return AwsSessionCredentials.create(
                                          configuration.getAwsAccessKeyId(),
                                          configuration.getAwsSecretAccessKey(),
                                          configuration.getAwsSessionToken());
    }
  }

  private static IamClient createIamClient(AwsbedrockConfiguration configuration,
                                           AwsbedrockAgentsParameters awsBedrockParameters) {
    return IamClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(createAwsBasicCredentials(configuration)))
        .region(AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion()))
        .build();
  }

  public static String ListAgents(AwsbedrockConfiguration configuration,
                                  AwsbedrockAgentsParameters awsBedrockParameters) {
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
    jsonObject.put(AGENT_NAMES, jsonArray);

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

  public static String getAgentbyAgentId(String agentId, AwsbedrockConfiguration configuration,
                                         AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    Agent agent = getAgentById(agentId, bedrockAgent);
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

  private static Optional<Agent> getAgentByName(String agentName, BedrockAgentClient bedrockAgentClient) {
    // Build a ListAgentsRequest instance without any filter criteria
    ListAgentsRequest listAgentsRequest = ListAgentsRequest.builder()
        .build();

    // Call the listAgents method of the BedrockAgentClient instance
    ListAgentsResponse listAgentsResponse = bedrockAgentClient.listAgents(listAgentsRequest);

    // Retrieve the list of agent summaries from the ListAgentsResponse instance
    List<AgentSummary> agentSummaries = listAgentsResponse.agentSummaries();

    // Iterate through the list of agent summaries to find the one with the
    // specified name
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

  public static String getAgentbyAgentName(String agentName, AwsbedrockConfiguration configuration,
                                           AwsbedrockAgentsParameters awsBedrockParameters) {
    String agentInfo = "";
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    Optional<Agent> optionalAgent = getAgentByName(agentName, bedrockAgent);
    if (optionalAgent.isPresent()) {
      Agent agent = optionalAgent.get();
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
    } else {
      agentInfo = NO_AGENT_FOUND;
      return agentInfo;
    }
  }

  public static String createAgentOperation(String name, String instruction, AwsbedrockConfiguration configuration,
                                            AwsbedrockAgentsParameters awsBedrockParameter) {
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameter);

    Role agentRole = createAgentRole(AGENT_POSTFIX, AGENT_ROLE_POLICY_NAME, configuration, awsBedrockParameter);

    Agent agent = createAgent(name, instruction, awsBedrockParameter.getModelName(), agentRole, bedrockAgent);

    PrepareAgentResponse agentDetails = prepareAgent(agent.agentId(), bedrockAgent);

    // AgentAlias AgentAlias = createAgentAlias(name, agent.agentId(),bedrockAgent);

    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put(AGENT_ID, agentDetails.agentId());
    jsonRequest.put(AGENT_VERSION, agentDetails.agentVersion());
    jsonRequest.put(AGENT_STATUS, agentDetails.agentStatusAsString());
    jsonRequest.put(PREPARED_AT, agentDetails.preparedAt());
    jsonRequest.put(AGENT_ARN, agent.agentArn());
    jsonRequest.put(AGENT_NAME, agent.agentName());
    jsonRequest.put(AGENT_RESOURCE_ROLE_ARN, agent.agentResourceRoleArn());
    jsonRequest.put(CLIENT_TOKEN, agent.clientToken());
    jsonRequest.put(CREATED_AT, agent.createdAt());
    jsonRequest.put(DESCRIPTION, agent.description());
    jsonRequest.put(FOUNDATION_MODEL, agent.foundationModel());
    jsonRequest.put(IDLE_SESSION_TTL_IN_SECONDS, agent.idleSessionTTLInSeconds());
    jsonRequest.put(INSTRUCTION, agent.instruction());
    jsonRequest.put(PROMPT_OVERRIDE_CONFIGURATION, agent.promptOverrideConfiguration());
    jsonRequest.put(UPDATED_AT, agent.updatedAt());
    return jsonRequest.toString();

    // return agentDetails.toString();
  }

  public static String createAgentAlias(String name, String agentId, AwsbedrockConfiguration configuration,
                                        AwsbedrockAgentsParameters awsBedrockParameter) {

    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameter);
    AgentAlias agentAlias = createAgentAlias(name, agentId, bedrockAgent);
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(AGENT_ALIAS_ID, agentAlias.agentAliasId());
    jsonObject.put(AGENT_ALIAS_NAME, agentAlias.agentAliasName());
    jsonObject.put(AGENT_ALIAS_ARN, agentAlias.agentAliasArn());
    jsonObject.put(CLIENT_TOKEN, agentAlias.clientToken());
    jsonObject.put(CREATED_AT, agentAlias.createdAt());
    jsonObject.put(UPDATED_AT, agentAlias.updatedAt());

    return jsonObject.toString();
  }

  private static Role createAgentRole(String postfix, String RolePolicyName, AwsbedrockConfiguration configuration,
                                      AwsbedrockAgentsParameters awsBedrockParameters) {
    String roleName = AGENT_EXECUTION_ROLE_PREFIX + postfix;
    String modelArn = "arn:aws:bedrock:" + awsBedrockParameters.getRegion() + "::foundation-model/"
        + awsBedrockParameters.getModelName() + "*";
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
            .assumeRolePolicyDocument(
                                      "{\"Version\": \"2012-10-17\",\"Statement\": [{\"Effect\": \"Allow\",\"Principal\": {\"Service\": \"bedrock.amazonaws.com\"},\"Action\": \"sts:AssumeRole\"}]}")
            .build());

        logger.info("Model ARN: {}", modelArn);
        String policyDocument = "{\n"
            + "    \"Version\": \"2012-10-17\",\n"
            + "    \"Statement\": [\n"
            + "        {\n"
            + "            \"Effect\": \"Allow\",\n"
            + "            \"Action\": [\n"
            + "                \"bedrock:ListFoundationModels\",\n"
            + "                \"bedrock:GetFoundationModel\",\n"
            + "                \"bedrock:TagResource\",\n"
            + "                \"bedrock:UntagResource\",\n"
            + "                \"bedrock:ListTagsForResource\",\n"
            + "                \"bedrock:CreateAgent\",\n"
            + "                \"bedrock:UpdateAgent\",\n"
            + "                \"bedrock:GetAgent\",\n"
            + "                \"bedrock:ListAgents\",\n"
            + "                \"bedrock:DeleteAgent\",\n"
            + "                \"bedrock:CreateAgentActionGroup\",\n"
            + "                \"bedrock:UpdateAgentActionGroup\",\n"
            + "                \"bedrock:GetAgentActionGroup\",\n"
            + "                \"bedrock:ListAgentActionGroups\",\n"
            + "                \"bedrock:DeleteAgentActionGroup\",\n"
            + "                \"bedrock:GetAgentVersion\",\n"
            + "                \"bedrock:ListAgentVersions\",\n"
            + "                \"bedrock:DeleteAgentVersion\",\n"
            + "                \"bedrock:CreateAgentAlias\",\n"
            + "                \"bedrock:UpdateAgentAlias\",\n"
            + "                \"bedrock:GetAgentAlias\",\n"
            + "                \"bedrock:ListAgentAliases\",\n"
            + "                \"bedrock:DeleteAgentAlias\",\n"
            + "                \"bedrock:AssociateAgentKnowledgeBase\",\n"
            + "                \"bedrock:DisassociateAgentKnowledgeBase\",\n"
            + "                \"bedrock:GetKnowledgeBase\",\n"
            + "                \"bedrock:ListKnowledgeBases\",\n"
            + "                \"bedrock:PrepareAgent\",\n"
            + "                \"bedrock:InvokeAgent\",\n"
            + "                \"bedrock:InvokeModel\"\n"
            + "            ],\n"
            + "            \"Resource\": \"*\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";
        logger.info("Policy Document: {}", policyDocument);
        iamClient.putRolePolicy(PutRolePolicyRequest.builder()
            .roleName(roleName)
            .policyName(ROLE_POLICY_NAME)
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
    } catch (IamException ex) {
      logger.error("Couldn't get role {}. Here's why: {}", roleName, ex.getMessage(), ex);
      throw ex;
    }
    return agentRole;
  }

  private static Agent createAgent(String name, String instruction, String modelId, Role agentRole,
                                   BedrockAgentClient bedrockAgentClient) {
    logger.info("Creating the agent...");

    // String instruction = "You are a friendly chat bot. You have access to a
    // function called that returns information about the current date and time.
    // When responding with date or time, please make sure to add the timezone
    // UTC.";
    CreateAgentResponse createAgentResponse = bedrockAgentClient.createAgent(CreateAgentRequest.builder()
        .agentName(name)
        .foundationModel(modelId)
        .instruction(instruction)
        .agentResourceRoleArn(agentRole.arn())
        .build());

    waitForAgentStatus(createAgentResponse.agent().agentId(), AgentStatus.NOT_PREPARED.toString(),
                       bedrockAgentClient);

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
        Thread.sleep(AGENT_STATUS_CHECK_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static PrepareAgentResponse prepareAgent(String agentId, BedrockAgentClient bedrockAgentClient) {
    logger.info("Preparing the agent...");

    // String agentId = agent.agentId();
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

  public static String listAllAgentAliases(String agentId, AwsbedrockConfiguration configuration,
                                           AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliases(agentId, bedrockAgent);

    JSONArray jsonArray = new JSONArray();
    for (AgentAliasSummary agentAliasSummary : agentAliasSummaries) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(AGENT_ALIAS_ID, agentAliasSummary.agentAliasId());
      jsonObject.put(AGENT_ALIAS_NAME, agentAliasSummary.agentAliasName());
      jsonObject.put(CREATED_AT, agentAliasSummary.createdAt());
      jsonObject.put(UPDATED_AT, agentAliasSummary.updatedAt());
      jsonArray.put(jsonObject);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(AGENT_ALIAS_SUMMARIES, jsonArray);

    return jsonObject.toString();
  }

  private static List<AgentAliasSummary> listAgentAliases(String agentId, BedrockAgentClient bedrockAgentClient) {
    // Build a ListAgentAliasesRequest instance with the agent ID
    ListAgentAliasesRequest listAgentAliasesRequest = ListAgentAliasesRequest.builder()
        .agentId(agentId)
        .build();

    // Call the listAgentAliases method of the BedrockAgentClient instance
    ListAgentAliasesResponse listAgentAliasesResponse = bedrockAgentClient
        .listAgentAliases(listAgentAliasesRequest);

    // Retrieve the list of agent alias summaries from the ListAgentAliasesResponse
    // instance
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliasesResponse.agentAliasSummaries();

    return agentAliasSummaries;
  }

  public static String deleteAgentAliasesByAgentId(String agentId, String agentAliasName,
                                                   AwsbedrockConfiguration configuration,
                                                   AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    DeleteAgentAliasResponse response = deleteAgentAliasByName(agentId, agentAliasName, bedrockAgent);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(AGENT_ID, response.agentId());
    jsonObject.put(AGENT_ALIAS_ID, response.agentAliasId());
    jsonObject.put(AGENT_ALIAS_STATUS, response.agentAliasStatus());
    jsonObject.put(AGENT_STATUS, response.agentAliasStatusAsString());

    return jsonObject.toString();
  }

  private static DeleteAgentAliasResponse deleteAgentAliasByName(String agentId, String agentAliasName,
                                                                 BedrockAgentClient bedrockAgentClient) {
    DeleteAgentAliasResponse response = null;

    // Build a ListAgentAliasesRequest instance with the agent ID
    ListAgentAliasesRequest listAgentAliasesRequest = ListAgentAliasesRequest.builder()
        .agentId(agentId)
        .build();

    // Call the listAgentAliases method of the BedrockAgentClient instance
    ListAgentAliasesResponse listAgentAliasesResponse = bedrockAgentClient
        .listAgentAliases(listAgentAliasesRequest);

    // Retrieve the list of agent alias summaries from the ListAgentAliasesResponse
    // instance
    List<AgentAliasSummary> agentAliasSummaries = listAgentAliasesResponse.agentAliasSummaries();

    // Iterate through the list of agent alias summaries to find the one with the
    // specified name
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
      DeleteAgentAliasResponse deleteAgentAliasResponse = bedrockAgentClient
          .deleteAgentAlias(deleteAgentAliasRequest);

      logger.info("Agent alias with name " + agentAliasName + " deleted successfully.");
      response = deleteAgentAliasResponse;
    } else {
      logger.info("No agent alias with name " + agentAliasName + " found.");
    }
    return response;
  }

  public static String deleteAgentByAgentId(String agentId, AwsbedrockConfiguration configuration,
                                            AwsbedrockAgentsParameters awsBedrockParameters) {
    BedrockAgentClient bedrockAgent = createBedrockAgentClient(configuration, awsBedrockParameters);
    DeleteAgentResponse response = deleteAgentById(agentId, bedrockAgent);

    JSONObject jsonObject = new JSONObject();
    jsonObject.put(AGENT_ID, response.agentId());
    jsonObject.put(AGENT_STATUS, response.agentStatusAsString());

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

  public static String chatWithAgent(String agentAlias, String agentId, String sessionId, String prompt,
                                     AwsbedrockConfiguration configuration, AwsbedrockAgentsParameters awsBedrockParameters) {

    BedrockAgentRuntimeAsyncClient bedrockAgent = createBedrockAgentRuntimeAsyncClient(configuration,
                                                                                       awsBedrockParameters);

    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
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
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Thread was interrupted while waiting for agent response", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to execute agent invocation", e);
    }
  }

  private static CompletableFuture<String> invokeAgent(String agentAlias, String agentId, String prompt,
                                                       String sessionId,
                                                       BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient)
      throws InterruptedException, ExecutionException {
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
            chunkData.put(TYPE, CHUNK);
            chunkData.put(TIMESTAMP, System.currentTimeMillis());

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
          finalResult.put(SESSION_ID, sessionId);
          finalResult.put(AGENT_ID, agentId);
          finalResult.put(AGENT_ALIAS, agentAlias);
          finalResult.put(PROMPT, prompt);
          finalResult.put(PROCESSED_AT, System.currentTimeMillis());
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

          finalResult.put(SUMMARY, summary);

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

  public static InputStream chatWithAgentSSEStream(String agentAlias, String agentId, String sessionId, String prompt,
                                                   AwsbedrockConfiguration configuration,
                                                   AwsbedrockAgentsParameters awsBedrockParameters) {

    BedrockAgentRuntimeAsyncClient bedrockAgent = createBedrockAgentRuntimeAsyncClient(configuration,
                                                                                       awsBedrockParameters);

    String effectiveSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId
        : UUID.randomUUID().toString();
    logger.info("Using sessionId: {}", effectiveSessionId);

    return invokeAgentSSEStream(agentAlias, agentId, prompt, effectiveSessionId, bedrockAgent);
  }

  /**
   * Invokes Bedrock Agent and returns streaming SSE response as InputStream.
   *
   * This method is designed to work with Mule's binary streaming.
   **/
  public static InputStream invokeAgentSSEStream(String agentAlias, String agentId, String prompt,
                                                 String sessionId,
                                                 BedrockAgentRuntimeAsyncClient bedrockAgentRuntimeAsyncClient) {

    try {
      // Create piped streams for real-time streaming
      PipedOutputStream outputStream = new PipedOutputStream();
      PipedInputStream inputStream = new PipedInputStream(outputStream);

      // Start the streaming process asynchronously
      CompletableFuture.runAsync(() -> {
        try {
          streamBedrockResponse(agentAlias, agentId, prompt, sessionId,
                                bedrockAgentRuntimeAsyncClient, outputStream);
        } catch (Exception e) {
          try {
            // Send error as SSE event
            String errorEvent = formatSSEEvent("error", createErrorJson(e).toString());
            outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
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
      return new ByteArrayInputStream(errorEvent.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static void streamBedrockResponse(String agentAlias, String agentId, String prompt,
                                            String sessionId, BedrockAgentRuntimeAsyncClient client,
                                            PipedOutputStream outputStream)
      throws Exception {

    try {
      // Send initial event
      JSONObject startEvent = createSessionStartJson(agentAlias, agentId, prompt, sessionId, System.currentTimeMillis());
      String sseStart = formatSSEEvent("session-start", startEvent.toString());
      outputStream.write(sseStart.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();

      InvokeAgentRequest request = InvokeAgentRequest.builder()
          .agentId(agentId)
          .agentAliasId(agentAlias)
          .sessionId(sessionId)
          .inputText(prompt)
          .streamingConfigurations(builder -> builder.streamFinalResponse(true))
          // .enableTrace(true)
          .build();

      InvokeAgentResponseHandler.Visitor visitor = InvokeAgentResponseHandler.Visitor.builder()
          .onChunk(chunk -> {
            try {
              JSONObject chunkData = createChunkJson(chunk);
              String sseEvent = formatSSEEvent("chunk", chunkData.toString());
              outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
              outputStream.flush();
            } catch (Exception e) {
              try {
                String errorEvent = formatSSEEvent("chunk-error", createErrorJson(e).toString());
                outputStream.write(errorEvent.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
              } catch (IOException ioException) {
                // Can't write error, stream is likely closed
                logger.error("Error writing error event: {}", ioException.getMessage());
              }
            }
          })
          .build();

      InvokeAgentResponseHandler handler = InvokeAgentResponseHandler.builder()
          .subscriber(visitor)
          .build();

      CompletableFuture<Void> invocationFuture = client.invokeAgent(request, handler);
      invocationFuture.get(); // Wait for completion

      // Send completion event
      JSONObject completionData = createCompletionJson(sessionId, agentId, agentAlias, 0);
      String completionEvent = formatSSEEvent("session-complete", completionData.toString());
      outputStream.write(completionEvent.getBytes(StandardCharsets.UTF_8));

    } finally {
      outputStream.close();
    }
  }

  private static JSONObject createChunkJson(PayloadPart chunk) {
    JSONObject chunkData = new JSONObject();
    chunkData.put(TYPE, CHUNK);
    chunkData.put(TIMESTAMP, System.currentTimeMillis());

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

  private static JSONObject createSessionStartJson(String agentAlias, String agentId, String prompt,
                                                   String sessionId, long timestamp) {
    JSONObject startData = new JSONObject();
    startData.put(SESSION_ID, sessionId);
    startData.put(AGENT_ID, agentId);
    startData.put(AGENT_ALIAS, agentAlias);
    startData.put(PROMPT, prompt);
    startData.put(PROCESSED_AT, timestamp);
    startData.put("status", "started");
    return startData;
  }

  private static JSONObject createCompletionJson(String sessionId, String agentId, String agentAlias, long duration) {
    JSONObject completionData = new JSONObject();
    completionData.put(SESSION_ID, sessionId);
    completionData.put(AGENT_ID, agentId);
    completionData.put(AGENT_ALIAS, agentAlias);
    completionData.put("status", "completed");
    completionData.put("duration_ms", duration);
    completionData.put(TIMESTAMP, System.currentTimeMillis());
    return completionData;
  }

  private static JSONObject createErrorJson(Throwable error) {
    JSONObject errorData = new JSONObject();
    errorData.put("error", error.getMessage());
    errorData.put("type", error.getClass().getSimpleName());
    errorData.put(TIMESTAMP, System.currentTimeMillis());
    return errorData;
  }

  private static String formatSSEEvent(String eventType, String data) {
    int eventId = eventCounter.incrementAndGet();
    return String.format("id: %d%nevent: %s%ndata: %s%n%n", eventId, eventType, data);
  }
}
