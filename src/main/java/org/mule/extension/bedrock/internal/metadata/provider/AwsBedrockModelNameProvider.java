package org.mule.extension.bedrock.internal.metadata.provider;

import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsBedrockModelNameProvider implements ValueProvider {

  public AwsBedrockModelNameProvider() {}

  private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
                                                                         "amazon.titan-text-express-v1",
                                                                         "amazon.titan-text-lite-v1",
                                                                         "amazon.titan-text-premier-v1:0",
                                                                         "amazon.nova-micro-v1:0",
                                                                         "amazon.nova-premier-v1:0",
                                                                         "amazon.nova-lite-v1:0",
                                                                         "amazon.nova-pro-v1:0",
                                                                         "anthropic.claude-3-7-sonnet-20250219-v1:0",
                                                                         "anthropic.claude-3-5-sonnet-20240620-v1:0",
                                                                         "anthropic.claude-3-5-sonnet-20241022-v2:0",
                                                                         "anthropic.claude-v2",
                                                                         "anthropic.claude-v2:1",
                                                                         "anthropic.claude-instant-v1",
                                                                         "ai21.jamba-1-5-large-v1:0",
                                                                         "ai21.jamba-instruct-v1:0",
                                                                         "ai21.jamba-1-5-mini-v1:0",
                                                                         "cohere.command-text-v14",
                                                                         "cohere.command-light-text-v14",
                                                                         "meta.llama3-3-70b-instruct-v1:0",
                                                                         "meta.llama3-2-3b-instruct-v1:0",
                                                                         "meta.llama3-2-1b-instruct-v1:0",
                                                                         "meta.llama3-1-70b-instruct-v1:0",
                                                                         "meta.llama3-1-8b-instruct-v1:0",
                                                                         "meta.llama3-70b-instruct-v1:0",
                                                                         "meta.llama3-8b-instruct-v1:0",
                                                                         "meta.llama4-maverick-17b-instruct-v1:0",
                                                                         "meta.llama4-scout-17b-instruct-v1:0",
                                                                         "mistral.pixtral-large-2502-v1:0",
                                                                         "mistral.mistral-7b-instruct-v0:2",
                                                                         "mistral.mixtral-8x7b-instruct-v0:1",
                                                                         "mistral.mistral-large-2402-v1:0",
                                                                         "mistral.mistral-small-2402-v1:0",
                                                                         "stability.stable-diffusion-xl-v0");

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return VALUES_FOR;
  }

}
