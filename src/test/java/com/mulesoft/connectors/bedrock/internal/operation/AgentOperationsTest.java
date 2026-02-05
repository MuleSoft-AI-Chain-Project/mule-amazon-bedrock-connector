package com.mulesoft.connectors.bedrock.internal.operation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentOperations")
class AgentOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses AgentService")
  void extendsBedrockOperation() {
    AgentOperations ops = new AgentOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new AgentOperations()).isNotNull();
  }
}
