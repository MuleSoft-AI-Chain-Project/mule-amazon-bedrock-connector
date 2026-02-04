package org.mule.extension.bedrock.internal.error.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AWSConnectionException")
class AWSConnectionExceptionTest {

  @Test
  @DisplayName("constructor with message")
  void constructorMessage() {
    AWSConnectionException e = new AWSConnectionException("connection failed");
    assertThat(e.getMessage()).contains("connection failed");
  }

  @Test
  @DisplayName("constructor with message and cause")
  void constructorWithCause() {
    RuntimeException cause = new RuntimeException("cause");
    AWSConnectionException e = new AWSConnectionException("connection failed", cause);
    assertThat(e.getMessage()).contains("connection failed");
    assertThat(e.getCause()).isSameAs(cause);
  }
}
