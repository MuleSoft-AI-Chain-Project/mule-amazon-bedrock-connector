package org.mule.extension.bedrock.api.params;

import org.mule.extension.bedrock.internal.metadata.provider.AwsBedrockModelNameProviderImage;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class BedrockImageParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockModelNameProviderImage.class)
  @Optional(defaultValue = "amazon.titan-image-generator-v1:0")
  private String modelName;

  public String getModelName() {
    return modelName;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "1")
  private Integer numOfImages;

  public Integer getNumOfImages() {
    return numOfImages;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "512")
  private Integer height;

  public Integer getHeight() {
    return height;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "512")
  private Integer width;

  public Integer getWidth() {
    return width;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "8.0")
  private Float cfgScale;

  public Float getCfgScale() {
    return cfgScale;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "0")
  private Integer seed;

  public Integer getSeed() {
    return seed;
  }

}
