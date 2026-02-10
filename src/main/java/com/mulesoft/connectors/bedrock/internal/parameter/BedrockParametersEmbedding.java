package com.mulesoft.connectors.bedrock.internal.parameter;

import com.mulesoft.connectors.bedrock.internal.metadata.provider.AwsBedrockModelNameProviderEmbedding;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.Objects;

public class BedrockParametersEmbedding {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockModelNameProviderEmbedding.class)
  @Optional(defaultValue = "amazon.titan-embed-text-v1")
  private String modelName;

  public String getModelName() {
    return modelName;
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockParametersEmbedding that))
      return false;
    return normalize == that.normalize && Objects.equals(modelName, that.modelName) && Objects.equals(dimension, that.dimension);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelName, dimension, normalize);
  }
}
