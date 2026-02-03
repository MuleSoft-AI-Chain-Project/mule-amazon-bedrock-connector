package org.mule.extension.aws.connection.provider;

import org.mule.extension.aws.connection.provider.parameters.CommonParameters;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.exception.AWSConnectionException;
import org.mule.extension.bedrock.internal.util.RegionUtils;
import org.mule.runtime.api.connection.ConnectionException;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
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

public class AssumeRoleConnectionProvider extends AbstractAssumeRoleConnectionProvider<BedrockConnection> {

  public AssumeRoleConnectionProvider() {
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
    org.mule.extension.bedrock.internal.util.RegionUtils.configureRegionProperty(bedrockRuntimeClientBuilder, commonParams);

    // Build BedrockClient
    BedrockClientBuilder bedrockClientBuilder = BedrockClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    org.mule.extension.bedrock.internal.util.RegionUtils.configureRegionProperty(bedrockClientBuilder, commonParams);

    // Build BedrockAgentClient
    BedrockAgentClientBuilder bedrockAgentClientBuilder = BedrockAgentClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    org.mule.extension.bedrock.internal.util.RegionUtils.configureRegionProperty(bedrockAgentClientBuilder, commonParams);

    // Build BedrockAgentRuntimeClient
    BedrockAgentRuntimeClientBuilder bedrockAgentRuntimeClientBuilder = BedrockAgentRuntimeClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    org.mule.extension.bedrock.internal.util.RegionUtils.configureRegionProperty(bedrockAgentRuntimeClientBuilder, commonParams);

    // Build IamClient
    IamClientBuilder iamClientBuilder = IamClient.builder()
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParams))
        .credentialsProvider(credentialsProvider);
    RegionUtils.configureRegionProperty(iamClientBuilder, commonParams);

    int connectionTimeoutMs = commonParams.getConnectionTimeout();
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

    String region = RegionUtils.getRegion(commonParams);
    return new BedrockConnection(region, bedrockRuntimeClientBuilder, bedrockClientBuilder,
                                 bedrockAgentClientBuilder, bedrockAgentRuntimeClientBuilder, iamClientBuilder,
                                 connectionTimeoutMs, agentRuntimeAsyncClientFactory, runtimeAsyncClientFactory);
  }
}
