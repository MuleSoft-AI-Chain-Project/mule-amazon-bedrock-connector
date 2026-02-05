package com.mulesoft.connectors.bedrock.internal.connection.provider;

import org.apache.commons.lang3.StringUtils;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import com.mulesoft.connectors.bedrock.internal.error.exception.AWSConnectionException;
import com.mulesoft.connectors.bedrock.internal.util.RegionUtils;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.security.TokenId;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;

@Alias("basic")
public class BasicConnectionProvider extends AbstractBedrockConnectionProvider<BedrockConnection> {

  @Parameter
  @DisplayName("Session Token")
  @Placement(order = 6)
  @Optional
  @Summary("The session token provided by Amazon STS.")
  @TokenId
  private String sessionToken;

  public BasicConnectionProvider() {
    // Default constructor intentionally empty.
    // Required for framework/deserialization/reflection-based instantiation.
  }

  @Override
  protected BedrockConnection buildConnection(CommonParameters commonParams) throws AWSConnectionException, ConnectionException {
    AwsCredentialsProvider credentialsProvider = this.getAWSCredentialsProvider(commonParams);

    // Build BedrockRuntimeClient
    BedrockRuntimeClientBuilder bedrockRuntimeClientBuilder = BedrockRuntimeClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(bedrockRuntimeClientBuilder, commonParams);

    // Build BedrockClient
    BedrockClientBuilder bedrockClientBuilder = BedrockClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(bedrockClientBuilder, commonParams);

    // Build BedrockAgentClient
    BedrockAgentClientBuilder bedrockAgentClientBuilder = BedrockAgentClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(bedrockAgentClientBuilder, commonParams);

    // Build BedrockAgentRuntimeClient
    BedrockAgentRuntimeClientBuilder bedrockAgentRuntimeClientBuilder = BedrockAgentRuntimeClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(bedrockAgentRuntimeClientBuilder, commonParams);

    // Build IamClient
    IamClientBuilder iamClientBuilder = IamClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(iamClientBuilder, commonParams);

    int connectionTimeoutMs = commonParams.getConnectionTimeout();
    // Factories build async clients with a given effective timeout so they can be cached by timeout
    java.util.function.LongFunction<BedrockAgentRuntimeAsyncClient> agentRuntimeAsyncClientFactory = timeoutMs -> {
      try {
        SdkAsyncHttpClient httpClient = sdkHttpClientFactory.buildHttpAsyncClient(commonParams, timeoutMs);
        BedrockAgentRuntimeAsyncClientBuilder builder = BedrockAgentRuntimeAsyncClient.builder()
            .httpClient(httpClient)
            .credentialsProvider(credentialsProvider);
        RegionUtils.configureRegionProperty(builder, commonParams);
        return builder.build();
      } catch (ConnectionException e) {
        throw new RuntimeException("Failed to build BedrockAgentRuntimeAsyncClient", e);
      }
    };
    java.util.function.LongFunction<BedrockRuntimeAsyncClient> runtimeAsyncClientFactory = timeoutMs -> {
      try {
        SdkAsyncHttpClient httpClient = sdkHttpClientFactory.buildHttpAsyncClient(commonParams, timeoutMs);
        BedrockRuntimeAsyncClientBuilder builder = BedrockRuntimeAsyncClient.builder()
            .httpClient(httpClient)
            .credentialsProvider(credentialsProvider);
        RegionUtils.configureRegionProperty(builder, commonParams);
        return builder.build();
      } catch (ConnectionException e) {
        throw new RuntimeException("Failed to build BedrockRuntimeAsyncClient", e);
      }
    };

    Region region = RegionUtils.getRegion(commonParams);
    return new BedrockConnection(region.id(), bedrockRuntimeClientBuilder, bedrockClientBuilder,
                                 bedrockAgentClientBuilder, bedrockAgentRuntimeClientBuilder, iamClientBuilder,
                                 connectionTimeoutMs, agentRuntimeAsyncClientFactory, runtimeAsyncClientFactory);
  }

  @Override
  protected AwsCredentialsProvider getAWSCredentialsProvider(CommonParameters commonParameters) {
    if (commonParameters.isTryDefaultAWSCredentialsProviderChain()) {
      return DefaultCredentialsProvider.builder().build();
    } else {
      return getStaticCredentialsProvider(commonParameters, sessionToken);
    }
  }

  private StaticCredentialsProvider getStaticCredentialsProvider(CommonParameters commonParameters, String sessionToken) {
    if (StringUtils.isBlank(sessionToken)) {
      return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                                         commonParameters.getAccessKey(),
                                                                         commonParameters.getSecretKey()));
    }
    return StaticCredentialsProvider.create(AwsSessionCredentials.create(
                                                                         commonParameters.getAccessKey(),
                                                                         commonParameters.getSecretKey(), sessionToken));
  }
}
