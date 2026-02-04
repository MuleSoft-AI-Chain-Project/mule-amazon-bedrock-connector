package org.mule.extension.bedrock.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;

@DisplayName("StreamingRetryUtility")
class StreamingRetryUtilityTest {

  @Nested
  @DisplayName("RetryConfig")
  class RetryConfigTests {

    @Test
    @DisplayName("holds maxRetries baseBackoffMs and enabled")
    void holdsValues() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(3, 100L, true);
      assertThat(config.getMaxRetries()).isEqualTo(3);
      assertThat(config.getBaseBackoffMs()).isEqualTo(100L);
      assertThat(config.isEnabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("RetryResult")
  class RetryResultTests {

    @Test
    @DisplayName("holds success exception attempts and chunksReceived")
    void holdsValues() {
      Exception e = new RuntimeException("test");
      StreamingRetryUtility.RetryResult result =
          new StreamingRetryUtility.RetryResult(false, e, 2, true);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getLastException()).isSameAs(e);
      assertThat(result.getAttemptsMade()).isEqualTo(2);
      assertThat(result.isChunksReceived()).isTrue();
    }
  }

  @Nested
  @DisplayName("isRetryableException")
  class IsRetryableException {

    @Test
    @DisplayName("returns false for null")
    void returnsFalseForNull() {
      assertThat(StreamingRetryUtility.isRetryableException(null)).isFalse();
    }

    @Test
    @DisplayName("returns true for TimeoutException")
    void returnsTrueForTimeout() {
      assertThat(StreamingRetryUtility.isRetryableException(new TimeoutException())).isTrue();
    }

    @Test
    @DisplayName("unwraps CompletionException and checks cause")
    void unwrapsCompletionException() {
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new CompletionException(new TimeoutException())))
          .isTrue();
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new CompletionException(new RuntimeException("other"))))
          .isFalse();
    }

    @Test
    @DisplayName("returns true for SdkClientException with retryable message")
    void sdkClientExceptionRetryable() {
      SdkClientException e1 = mock(SdkClientException.class);
      when(e1.getMessage()).thenReturn("connection timed out");
      SdkClientException e2 = mock(SdkClientException.class);
      when(e2.getMessage()).thenReturn("read timeout");
      SdkClientException e3 = mock(SdkClientException.class);
      when(e3.getMessage()).thenReturn("connection refused");
      assertThat(StreamingRetryUtility.isRetryableException(e1)).isTrue();
      assertThat(StreamingRetryUtility.isRetryableException(e2)).isTrue();
      assertThat(StreamingRetryUtility.isRetryableException(e3)).isTrue();
    }

    @Test
    @DisplayName("returns true for SdkClientException with null message")
    void sdkClientExceptionNullMessage() {
      SdkClientException e = mock(SdkClientException.class);
      when(e.getMessage()).thenReturn(null);
      assertThat(StreamingRetryUtility.isRetryableException(e)).isTrue();
    }

    @Test
    @DisplayName("returns false for SdkClientException with non-retryable message")
    void sdkClientExceptionNonRetryable() {
      SdkClientException e = mock(SdkClientException.class);
      when(e.getMessage()).thenReturn("invalid configuration");
      assertThat(StreamingRetryUtility.isRetryableException(e)).isFalse();
    }

    @Test
    @DisplayName("unwraps ExecutionException")
    void unwrapsExecutionException() {
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new ExecutionException(new TimeoutException())))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("calculateExponentialBackoff")
  class CalculateExponentialBackoff {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("computes base * 2^attempt")
    void computesCorrectly(int attempt) {
      long base = 100L;
      long expected = base * (1L << attempt);
      assertThat(StreamingRetryUtility.calculateExponentialBackoff(attempt, base)).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("executeWithRetry (non-streaming)")
  class ExecuteWithRetry {

    @Test
    @DisplayName("returns result when operation succeeds")
    void returnsResultWhenSuccess() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 10L, true);
      String result = StreamingRetryUtility.executeWithRetry(() -> "ok", config);
      assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("executes once when retry disabled")
    void noRetryWhenDisabled() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 10L, false);
      int[] count = {0};
      String result = StreamingRetryUtility.executeWithRetry(() -> {
        count[0]++;
        return "done";
      }, config);
      assertThat(result).isEqualTo("done");
      assertThat(count[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("throws when all retries exhausted")
    void throwsWhenExhausted() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(1, 1L, true);
      SdkClientException sdkEx = mock(SdkClientException.class);
      when(sdkEx.getMessage()).thenReturn("timeout");
      SdkClientException finalEx = sdkEx;
      assertThatThrownBy(() -> StreamingRetryUtility.executeWithRetry(() -> {
        throw finalEx;
      }, config)).isInstanceOf(RuntimeException.class);
    }
  }

  @Nested
  @DisplayName("executeWithRetry (streaming)")
  class ExecuteWithRetryStreaming {

    @Test
    @DisplayName("returns success when operation succeeds")
    void successWhenOk() throws Exception {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 10L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> chunks.set(true),
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getAttemptsMade()).isEqualTo(1);
    }

    @Test
    @DisplayName("executes once when retry disabled and returns on failure")
    void disabledReturnsFailure() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 10L, false);
      AtomicBoolean chunks = new AtomicBoolean(false);
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          throw new RuntimeException("fail");
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getLastException()).hasMessage("fail");
    }
  }

  @Nested
  @DisplayName("createRetryErrorMessage")
  class CreateRetryErrorMessage {

    @Test
    @DisplayName("appends retry info when attempts > 1")
    void appendsRetryInfo() {
      String msg = StreamingRetryUtility.createRetryErrorMessage("Original", 2, 3, false);
      assertThat(msg).contains("Original");
      assertThat(msg).contains("failed after 2 attempt(s)");
      assertThat(msg).contains("out of 4 total");
    }

    @Test
    @DisplayName("appends chunks note when chunksReceived")
    void appendsChunksNote() {
      String msg = StreamingRetryUtility.createRetryErrorMessage("Err", 1, 0, true);
      assertThat(msg).contains("Some chunks were received before failure");
    }
  }
}
