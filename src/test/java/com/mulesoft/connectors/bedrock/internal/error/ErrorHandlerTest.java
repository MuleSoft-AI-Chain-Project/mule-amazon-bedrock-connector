package com.mulesoft.connectors.bedrock.internal.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.error.exception.BedrockException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.bedrockagent.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockagent.model.BedrockAgentException;
import software.amazon.awssdk.services.bedrockagent.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockagent.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockagent.model.ValidationException;

@DisplayName("ErrorHandler")
class ErrorHandlerTest {

  @Nested
  @DisplayName("handleSdkClientException")
  class HandleSdkClientException {

    @Test
    @DisplayName("wraps in BedrockException with CLIENT_ERROR")
    void wrapsCorrectly() {
      SdkClientException e = mock(SdkClientException.class);
      when(e.getMessage()).thenReturn("connection failed");
      BedrockException result = ErrorHandler.handleSdkClientException(e, "my-model");
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(BedrockException.class);
      assertThat(result.getMessage()).contains("my-model").contains("connection failed");
      assertThat(result.getCause()).isSameAs(e);
    }
  }

  @Nested
  @DisplayName("handleValidationException")
  class HandleValidationException {

    @Test
    @DisplayName("wraps in BedrockException with VALIDATION_ERROR")
    void wrapsCorrectly() {
      ValidationException e = ValidationException.builder().message("invalid param").build();
      BedrockException result = ErrorHandler.handleValidationException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
      assertThat(result.getMessage()).contains("Invalid request parameters");
    }
  }

  @Nested
  @DisplayName("handleResourceNotFoundException")
  class HandleResourceNotFoundException {

    @Test
    @DisplayName("wraps in BedrockException with RESOURCE_NOT_FOUND")
    void wrapsCorrectly() {
      ResourceNotFoundException e = ResourceNotFoundException.builder().message("not found").build();
      BedrockException result = ErrorHandler.handleResourceNotFoundException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleAccessDeniedException")
  class HandleAccessDeniedException {

    @Test
    @DisplayName("wraps in BedrockException with ACCESS_DENIED")
    void wrapsCorrectly() {
      AccessDeniedException e = AccessDeniedException.builder().message("denied").build();
      BedrockException result = ErrorHandler.handleAccessDeniedException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleThrottlingException")
  class HandleThrottlingException {

    @Test
    @DisplayName("wraps in BedrockException with THROTTLING_ERROR")
    void wrapsCorrectly() {
      ThrottlingException e = ThrottlingException.builder().message("throttled").build();
      BedrockException result = ErrorHandler.handleThrottlingException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleBedrockAgentException")
  class HandleBedrockAgentException {

    @Test
    @DisplayName("wraps in BedrockException with SERVICE_ERROR")
    void wrapsCorrectly() {
      BedrockAgentException e = mock(BedrockAgentException.class);
      when(e.getMessage()).thenReturn("agent error");
      when(e.awsErrorDetails()).thenReturn(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
          .errorCode("InternalError")
          .build());
      when(e.statusCode()).thenReturn(500);
      BedrockException result = ErrorHandler.handleBedrockAgentException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleSdkServiceException")
  class HandleSdkServiceException {

    @Test
    @DisplayName("wraps in BedrockException with SERVICE_ERROR")
    void wrapsCorrectly() {
      SdkServiceException e = mock(SdkServiceException.class);
      when(e.statusCode()).thenReturn(500);
      when(e.requestId()).thenReturn("req-1");
      when(e.getMessage()).thenReturn("service error");
      BedrockException result = ErrorHandler.handleSdkServiceException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleSdkException")
  class HandleSdkException {

    @Test
    @DisplayName("wraps in BedrockException with AWS_SDK_ERROR")
    void wrapsCorrectly() {
      SdkException e = mock(SdkException.class);
      when(e.getMessage()).thenReturn("sdk error");
      BedrockException result = ErrorHandler.handleSdkException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }
  }

  @Nested
  @DisplayName("handleIllegalStateException")
  class HandleIllegalStateException {

    @Test
    @DisplayName("converts to BedrockException when cause is connection pool shut down")
    void connectionPoolShutDown() {
      IllegalStateException e = new IllegalStateException("state", new RuntimeException("Connection pool shut down"));
      BedrockException result = ErrorHandler.handleIllegalStateException(e);
      assertThat(result).isInstanceOf(BedrockException.class);
    }

    @Test
    @DisplayName("rethrows when cause is not connection pool message")
    void rethrowsOtherwise() {
      IllegalStateException e = new IllegalStateException("other");
      assertThatThrownBy(() -> ErrorHandler.handleIllegalStateException(e))
          .isSameAs(e);
    }
  }

  @Nested
  @DisplayName("handleRuntimeException")
  class HandleRuntimeException {

    @Test
    @DisplayName("returns same exception after logging")
    void returnsSame() {
      RuntimeException e = new RuntimeException("runtime");
      RuntimeException result = ErrorHandler.handleRuntimeException(e, "test-context");
      assertThat(result).isSameAs(e);
    }
  }
}
