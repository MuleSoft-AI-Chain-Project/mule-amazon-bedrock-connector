package com.mulesoft.connectors.bedrock.api.provider;

import java.util.Objects;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsBedrockModelNameProvider implements ValueProvider {

  public AwsBedrockModelNameProvider() {
    // Default constructor intentionally empty.
    // Required for framework/deserialization/reflection-based instantiation.
  }

  private static final Set<Value> VALUES_FOR = ValueBuilder.getValuesFor(
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
                                                                         "ai21.jamba-1-5-mini-v1:0",
                                                                         "cohere.command-r-plus-v1:0",
                                                                         "cohere.command-r-v1:0",
                                                                         "google.gemma-3-12b-it",
                                                                         "google.gemma-3-27b-it",
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
                                                                         "openai.gpt-oss-safeguard-120b",
                                                                         "openai.gpt-oss-safeguard-20b",
                                                                         "openai.gpt-oss-120b-1:0",
                                                                         "openai.gpt-oss-20b-1:0",
                                                                         "stability.stable-diffusion-xl-v0");

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
