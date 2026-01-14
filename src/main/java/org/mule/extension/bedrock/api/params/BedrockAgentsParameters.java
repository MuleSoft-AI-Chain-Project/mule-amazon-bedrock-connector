package org.mule.extension.bedrock.api.params;

import org.mule.extension.bedrock.internal.metadata.provider.AwsBedrockRegionNameProvider;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockAgentsModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class BedrockAgentsParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(BedrockAgentsModelNameProvider.class)
  @Optional(defaultValue = "amazon.titan-text-premier-v1:0")
  private String modelName;

  public String getModelName() {
    return modelName;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockRegionNameProvider.class)
  @Optional(defaultValue = "us-east-1")
  private String region;

  public String getRegion() {
    return region;
  }
}
