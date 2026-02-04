package com.mulesoft.connectors.bedrock.internal.error.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.error.BedrockErrorType;

@DisplayName("BedrockException")
class BedrockExceptionTest {

  @Test
  @DisplayName("constructor with message and errorType")
  void constructorMessageAndType() {
    BedrockException e = new BedrockException("test message", BedrockErrorType.VALIDATION_ERROR);
    assertThat(e.getMessage()).isEqualTo("test message");
  }

  @Test
  @DisplayName("constructor with message errorType and cause")
  void constructorWithCause() {
    RuntimeException cause = new RuntimeException("cause");
    BedrockException e = new BedrockException("msg", BedrockErrorType.CLIENT_ERROR, cause);
    assertThat(e.getMessage()).isEqualTo("msg");
    assertThat(e.getCause()).isSameAs(cause);
  }

  @Test
  @DisplayName("constructor with errorType and cause")
  void constructorTypeAndCause() {
    RuntimeException cause = new RuntimeException("cause");
    BedrockException e = new BedrockException(BedrockErrorType.SERVICE_ERROR, cause);
    assertThat(e.getCause()).isSameAs(cause);
  }
}
