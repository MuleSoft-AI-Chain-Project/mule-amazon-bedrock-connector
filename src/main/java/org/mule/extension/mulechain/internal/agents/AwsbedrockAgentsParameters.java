package org.mule.extension.mulechain.internal.agents;

import org.mule.extension.mulechain.internal.AwsbedrockRegionNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class AwsbedrockAgentsParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsbedrockAgentsModelNameProvider.class)
  @Optional(defaultValue = "amazon.titan-text-premier-v1:0")
  private String modelName;

  public String getModelName() {
    return modelName;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsbedrockRegionNameProvider.class)
  @Optional(defaultValue = "us-east-1")
  private String region;

  public String getRegion() {
    return region;
  }
}
