package org.mule.extension.aws.commons.internal.connection.provider;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import org.mule.extension.aws.commons.internal.connection.provider.client.SdkHttpClientFactory;
import org.mule.extension.aws.commons.internal.connection.provider.parameters.CommonParameters;
import org.mule.extension.bedrock.internal.util.RegionUtils;
import org.mule.runtime.api.connection.ConnectionException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

/**
 * Helper class to build StsClient instances, reducing code duplication across connection providers.
 */
public class StsClientHelper {

  private StsClientHelper() {
    // Utility class - private constructor
  }

  /**
   * Builds an StsClient with the given parameters.
   *
   * @param commonParameters Common AWS parameters
   * @param sdkHttpClientFactory HTTP client factory
   * @param customStsEndpoint Optional custom STS endpoint
   * @return Configured StsClient
   * @throws ConnectionException if client cannot be built
   */
  public static StsClient buildStsClient(CommonParameters commonParameters,
                                         SdkHttpClientFactory sdkHttpClientFactory,
                                         String customStsEndpoint)
      throws ConnectionException {
    String region = RegionUtils.getRegion(commonParameters);
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();

    StsClientBuilder stsClientBuilder = StsClient.builder()
        .credentialsProvider(commonParameters.isTryDefaultAWSCredentialsProviderChain()
            ? DefaultCredentialsProvider.builder().build()
            : StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                                          commonParameters.getAccessKey(),
                                                                          commonParameters.getSecretKey())))
        .httpClient(sdkHttpClientFactory.buildHttpClient(commonParameters));

    if (!StringUtils.isBlank(customStsEndpoint)) {
      stsClientBuilder.endpointOverride(URI.create(customStsEndpoint));
    }

    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      stsClientBuilder.endpointOverride(URI.create(serviceEndpoint))
          .region(Region.of(region));
    } else {
      stsClientBuilder.region(Region.of(region));
    }

    return stsClientBuilder.build();
  }
}
