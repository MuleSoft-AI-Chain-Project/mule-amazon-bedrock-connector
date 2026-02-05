package com.mulesoft.connectors.bedrock.internal.parameter;

import com.mulesoft.connectors.bedrock.internal.metadata.provider.AwsBedrockModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.Objects;

public class BedrockParameters {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockModelNameProvider.class)
  @Optional(defaultValue = "amazon.nova-lite-v1:0")
  private String modelName;

  public String getModelName() {
    return modelName;
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
  @Optional()
  private Float topP;

  public Float getTopP() {
    return topP;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional()
  private Integer topK;

  public Integer getTopK() {
    return topK;
  }

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @Optional()
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockParameters that))
      return false;
    return Objects.equals(modelName, that.modelName) && Objects.equals(temperature, that.temperature)
        && Objects.equals(topP, that.topP) && Objects.equals(topK, that.topK) && Objects.equals(maxTokenCount, that.maxTokenCount)
        && Objects.equals(guardrailIdentifier, that.guardrailIdentifier)
        && Objects.equals(guardrailVersion, that.guardrailVersion) && Objects.equals(awsAccountId, that.awsAccountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelName, temperature, topP, topK, maxTokenCount, guardrailIdentifier, guardrailVersion, awsAccountId);
  }
}
