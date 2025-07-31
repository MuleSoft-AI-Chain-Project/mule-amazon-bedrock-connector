package org.mule.extension.mulechain.internal.agents;

import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsbedrockAgentsModelNameProvider implements ValueProvider {

  private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(

                                                                         "anthropic.claude-v2",
                                                                         "anthropic.claude-instant-v1",
                                                                         "anthropic.claude-v2:1",
                                                                         "amazon.titan-text-premier-v1:0",
                                                                         "amazon.nova-lite-v1:0",
                                                                         "amazon.nova-micro-v1:0",
                                                                         "amazon.nova-pro-v1:0",
                                                                         "anthropic.claude-3-sonnet-20240229-v1:0",
                                                                         "anthropic.claude-3-haiku-20240307-v1:0",
                                                                         "anthropic.claude-3-opus-20240229-v1:0"

  );

  @Override
  public Set<Value> resolve() throws ValueResolvingException {

    return VALUES_FOR;
  }

}
