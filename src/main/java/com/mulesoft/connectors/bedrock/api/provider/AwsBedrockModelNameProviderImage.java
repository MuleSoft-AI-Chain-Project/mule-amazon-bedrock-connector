package com.mulesoft.connectors.bedrock.api.provider;

import java.util.Objects;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsBedrockModelNameProviderImage implements ValueProvider {

  private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
                                                                         "amazon.titan-image-generator-v2:0",
                                                                         "amazon.titan-image-generator-v1",
                                                                         "stability.stable-diffusion-xl-v1",
                                                                         "amazon.nova-canvas-v1:0");

  public static Set<Value> getValuesFor() {
    return VALUES_FOR;
  }

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return VALUES_FOR;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass());
  }

}
