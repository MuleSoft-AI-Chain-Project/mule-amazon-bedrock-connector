package com.mulesoft.connectors.bedrock.internal.metadata.provider;

import java.util.Objects;
import java.util.Set;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

public class AwsBedrockDocumentSplitProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return ValueBuilder.getValuesFor("FULL", "PARAGRAPH", "SENTENCES");
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
