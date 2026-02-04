package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import java.util.Set;
import java.util.stream.Collectors;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import software.amazon.awssdk.regions.Region;

public class AwsBedrockRegionNameProvider implements ValueProvider {

  public AwsBedrockRegionNameProvider() {}

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return Region.regions().stream()
        .map(region -> ValueBuilder.newValue(region.id()).withDisplayName(region.id()).build())
        .collect(Collectors.toSet());
  }
}
