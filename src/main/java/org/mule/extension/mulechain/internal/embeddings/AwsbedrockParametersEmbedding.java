package org.mule.extension.mulechain.internal.embeddings;

import org.mule.extension.mulechain.internal.AwsbedrockRegionNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class AwsbedrockParametersEmbedding {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsbedrockModelNameProviderEmbedding.class)
  @Optional(defaultValue = "amazon.titan-embed-text-v1")
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

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "1024")
  private Integer dimension;

  public Integer getDimension() {
    return dimension;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "true")
  private boolean normalize;

  public boolean getNormalize() {
    return normalize;
  }

}
