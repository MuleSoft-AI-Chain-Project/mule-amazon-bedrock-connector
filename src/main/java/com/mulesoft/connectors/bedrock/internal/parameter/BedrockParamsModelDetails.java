package com.mulesoft.connectors.bedrock.internal.parameter;

import com.mulesoft.connectors.bedrock.internal.metadata.provider.AwsBedrockModelNameProvider;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;

import java.util.Objects;

public class BedrockParamsModelDetails {

  @Parameter
  @Expression(ExpressionSupport.SUPPORTED)
  @OfValues(AwsBedrockModelNameProvider.class)
  @Optional(defaultValue = "amazon.titan-text-express-v1")
  private String modelName;

  public String getModelName() {
    return modelName;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BedrockParamsModelDetails that))
      return false;
    return Objects.equals(modelName, that.modelName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(modelName);
  }
}
