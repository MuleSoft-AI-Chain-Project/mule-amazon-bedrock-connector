package com.mulesoft.connectors.bedrock.internal.util;

import java.net.URI;
import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClientBuilder;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.iam.IamClientBuilder;

public class RegionUtils {

  public static String getRegion(CommonParameters commonParameters) {
    try {
      return Region.of(commonParameters.getRegion()).toString();
    } catch (IllegalArgumentException | NullPointerException e) {
      return commonParameters.getRegion();
    }
  }

  public static void configureRegionProperty(BedrockRuntimeClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(BedrockClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(BedrockAgentClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(BedrockAgentRuntimeClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(IamClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(BedrockAgentRuntimeAsyncClientBuilder clientBuilder,
                                             CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }

  public static void configureRegionProperty(BedrockRuntimeAsyncClientBuilder clientBuilder, CommonParameters commonParameters) {
    Region region = Region.of(getRegion(commonParameters));
    String serviceEndpoint = commonParameters.getCustomServiceEndpoint();
    if (serviceEndpoint != null && !serviceEndpoint.isEmpty()) {
      clientBuilder.endpointOverride(URI.create(commonParameters.getCustomServiceEndpoint())).region(region);
    } else {
      clientBuilder.region(region);
    }
  }
}
