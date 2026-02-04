package com.mulesoft.connectors.bedrock.api.params;

import com.mulesoft.connectors.bedrock.internal.metadata.provider.AwsBedrockRegionNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class BedrockParamRegion {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockRegionNameProvider.class)
  @Optional(defaultValue = "us-east-1")
  private String region;

  public String getRegion() {
    return region;
  }
}
