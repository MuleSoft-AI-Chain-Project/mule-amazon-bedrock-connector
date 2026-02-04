package org.mule.extension.bedrock.internal.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatOperations")
class ChatOperationsTest {

  @Test
  @DisplayName("extends BedrockOperation and uses ChatService")
  void extendsBedrockOperation() {
    ChatOperations ops = new ChatOperations();
    assertThat(ops).isNotNull();
    assertThat(ops).isInstanceOf(BedrockOperation.class);
  }

  @Test
  @DisplayName("can be instantiated with no-arg constructor")
  void instantiation() {
    assertThat(new ChatOperations()).isNotNull();
  }
}
