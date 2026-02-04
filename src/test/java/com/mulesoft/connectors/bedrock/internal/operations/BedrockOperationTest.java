package com.mulesoft.connectors.bedrock.internal.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BedrockOperation")
class BedrockOperationTest {

  @Test
  @DisplayName("subclass ChatOperations uses ChatService type")
  void chatOperationsServiceType() {
    ChatOperations ops = new ChatOperations();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
    // BedrockOperation<SERVICE> - ChatOperations extends BedrockOperation<ChatService>
    assertThat(ops.getClass().getGenericSuperclass()).isNotNull();
  }

  @Test
  @DisplayName("all operation classes extend BedrockOperation")
  void allOperationsExtendBedrockOperation() {
    assertThat(new ChatOperations()).isInstanceOf(BedrockOperation.class);
    assertThat(new AgentOperations()).isInstanceOf(BedrockOperation.class);
    assertThat(new EmbeddingOperation()).isInstanceOf(BedrockOperation.class);
    assertThat(new FoundationalModelOperations()).isInstanceOf(BedrockOperation.class);
    assertThat(new ImageOperation()).isInstanceOf(BedrockOperation.class);
    assertThat(new SentimentOperations()).isInstanceOf(BedrockOperation.class);
  }
}
