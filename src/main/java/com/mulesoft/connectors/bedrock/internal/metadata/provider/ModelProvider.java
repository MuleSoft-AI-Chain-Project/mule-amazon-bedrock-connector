package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import java.util.Arrays;
import java.util.Optional;

public enum ModelProvider {

  AMAZON("amazon.titan-text"), AMAZON_NOVA("amazon.nova"), ANTHROPIC("anthropic.claude"), AI21("ai21.j2"), MISTRAL(
      "mistral.mistral"), COHERE("cohere.command"), META("meta.llama"), STABILITY("stability.stable-diffusion");

  private final String modelIdPrefix;

  ModelProvider(String modelIdPrefix) {
    this.modelIdPrefix = modelIdPrefix;
  }

  public String getModelIdPrefix() {
    return modelIdPrefix;
  }

  public static Optional<ModelProvider> fromModelId(String modelId) {
    return Arrays.stream(values())
        .filter(provider -> modelId.contains(provider.getModelIdPrefix()))
        .findFirst();
  }
}
