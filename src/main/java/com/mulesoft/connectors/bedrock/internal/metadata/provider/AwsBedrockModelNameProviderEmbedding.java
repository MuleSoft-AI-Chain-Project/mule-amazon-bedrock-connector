package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsBedrockModelNameProviderEmbedding implements ValueProvider {

  private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
                                                                         "amazon.titan-embed-text-v1",
                                                                         "amazon.titan-embed-text-v2:0",
                                                                         "amazon.titan-embed-image-v1",
                                                                         "amazon.nova-lite-v1:0",
                                                                         "amazon.nova-micro-v1:0",
                                                                         "amazon.nova-pro-v1:0");

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return VALUES_FOR;
  }

}
