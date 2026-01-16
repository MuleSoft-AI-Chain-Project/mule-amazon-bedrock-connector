package org.mule.extension.bedrock.api.params;

import org.mule.extension.bedrock.internal.metadata.provider.AwsBedrockModelNameProvider;
import org.mule.extension.bedrock.internal.metadata.provider.AwsBedrockRegionNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

public class BedrockParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockModelNameProvider.class)
  @Optional(defaultValue = "amazon.titan-text-express-v1")
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

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "0.7")
  private Float temperature;

  public Float getTemperature() {
    return temperature;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "0.9")
  private Float topP;

  public Float getTopP() {
    return topP;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "250")
  private Integer topK;

  public Integer getTopK() {
    return topK;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "512")
  private Integer maxTokenCount;

  public Integer getMaxTokenCount() {
    return maxTokenCount;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional()
  private String guardrailIdentifier;

  public String getGuardrailIdentifier() {
    return guardrailIdentifier;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional(defaultValue = "1")
  private String guardrailVersion;

  public String getGuardrailVersion() {
    return guardrailVersion;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional()
  private String awsAccountId;

  public String getAwsAccountId() {
    return awsAccountId;
  }
}
