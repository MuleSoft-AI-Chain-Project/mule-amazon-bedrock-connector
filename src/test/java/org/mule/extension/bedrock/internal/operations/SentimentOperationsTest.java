package org.mule.extension.bedrock.internal.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SentimentOperations")
class SentimentOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses SentimentService")
  void extendsBedrockOperation() {
    SentimentOperations ops = new SentimentOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new SentimentOperations()).isNotNull();
  }
}
