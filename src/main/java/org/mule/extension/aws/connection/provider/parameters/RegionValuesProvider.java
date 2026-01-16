package org.mule.extension.aws.connection.provider.parameters;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import software.amazon.awssdk.regions.Region;

public class RegionValuesProvider implements ValueProvider {

  public RegionValuesProvider() {
    // Default constructor intentionally empty.
    // Required for framework/deserialization/reflection-based instantiation.
  }

  public Set<Value> resolve() {
    return Stream.of(Region.regions()).map((region) -> {
      return ValueBuilder.newValue(region.toString()).withDisplayName(region.toString()).build();
    }).collect(Collectors.toSet());
  }
}
