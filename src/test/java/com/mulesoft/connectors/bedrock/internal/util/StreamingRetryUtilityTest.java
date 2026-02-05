package com.mulesoft.connectors.bedrock.internal.util;

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

    @Test
    @DisplayName("returns false for CompletionException with null cause")
    void completionExceptionNullCause() {
      assertThat(StreamingRetryUtility.isRetryableException(new CompletionException(null))).isFalse();
    }

    @Test
    @DisplayName("returns false for ExecutionException with null cause")
    void executionExceptionNullCause() {
      assertThat(StreamingRetryUtility.isRetryableException(new ExecutionException(null))).isFalse();
    }

    @Test
    @DisplayName("unwraps RuntimeException cause")
    void runtimeExceptionWithCause() {
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new RuntimeException(new TimeoutException())))
          .isTrue();
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new RuntimeException(new IllegalStateException())))
          .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"socket error", "unable to execute", "http request failed",
        "failed to connect", "connection reset"})
    @DisplayName("returns true for SdkClientException with other retryable messages")
    void sdkClientExceptionOtherRetryable(String message) {
      SdkClientException e = mock(SdkClientException.class);
      when(e.getMessage()).thenReturn(message);
      assertThat(StreamingRetryUtility.isRetryableException(e)).isTrue();
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

    @Test
    @DisplayName("succeeds on second attempt when first throws retryable")
    void succeedsOnRetry() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      SdkClientException retryable = mock(SdkClientException.class);
      when(retryable.getMessage()).thenReturn("timeout");
      int[] attempts = {0};
      String result = StreamingRetryUtility.executeWithRetry(() -> {
        if (attempts[0]++ < 1) {
          throw retryable;
        }
        return "ok";
      }, config);
      assertThat(result).isEqualTo("ok");
      assertThat(attempts[0]).isEqualTo(2);
    }

    @Test
    @DisplayName("throws when non-retryable exception")
    void throwsWhenNonRetryable() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      assertThatThrownBy(() -> StreamingRetryUtility.executeWithRetry(
                                                                      () -> {
                                                                        throw new RuntimeException("config error");
                                                                      },
                                                                      config))
          .isInstanceOf(RuntimeException.class);
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

    @Test
    @DisplayName("returns failure when chunks already received and exception")
    void noRetryWhenChunksReceived() throws Exception {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      AtomicBoolean chunks = new AtomicBoolean(true);
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          throw new TimeoutException("timeout");
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.isChunksReceived()).isTrue();
    }

    @Test
    @DisplayName("returns failure when not retryable")
    void noRetryWhenNotRetryable() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          throw new RuntimeException("bad request");
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("succeeds on second attempt when first fails with retryable")
    void streamingSucceedsOnRetry() throws Exception {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      int[] attempts = {0};
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          if (attempts[0]++ < 1) {
                                                                                            throw new TimeoutException("timeout");
                                                                                          }
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getAttemptsMade()).isEqualTo(2);
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

    @Test
    @DisplayName("returns original when attemptsMade is 1")
    void singleAttemptNoAppend() {
      String msg = StreamingRetryUtility.createRetryErrorMessage("Original", 1, 3, false);
      assertThat(msg).isEqualTo("Original");
    }

    @Test
    @DisplayName("appends retry info without total when maxRetries 0")
    void maxRetriesZero() {
      String msg = StreamingRetryUtility.createRetryErrorMessage("Err", 2, 0, false);
      assertThat(msg).contains("failed after 2 attempt(s)");
      assertThat(msg).doesNotContain("out of");
    }

    @Test
    @DisplayName("appends both retry info and chunks note when attemptsMade > 1 and chunksReceived")
    void attemptsGreaterThanOneAndChunksReceived() {
      String msg = StreamingRetryUtility.createRetryErrorMessage("Original", 2, 1, true);
      assertThat(msg).contains("Original");
      assertThat(msg).contains("failed after 2 attempt(s)");
      assertThat(msg).contains("Some chunks were received before failure");
    }
  }

  @Nested
  @DisplayName("executeWithRetry (non-streaming) interrupt")
  class ExecuteWithRetryInterrupt {

    @Test
    @DisplayName("throws RuntimeException with InterruptedException cause when thread interrupted during sleep")
    void interruptDuringSleep() throws Exception {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 5000L, true);
      SdkClientException retryable = mock(SdkClientException.class);
      when(retryable.getMessage()).thenReturn("timeout");
      AtomicBoolean interrupted = new AtomicBoolean(false);
      Thread worker = new Thread(() -> {
        try {
          StreamingRetryUtility.executeWithRetry(() -> {
            throw retryable;
          }, config);
        } catch (RuntimeException e) {
          if (e.getCause() instanceof InterruptedException) {
            interrupted.set(true);
          }
          throw e;
        }
      });
      worker.start();
      Thread.sleep(50);
      worker.interrupt();
      worker.join(3000);
      assertThat(interrupted.get()).isTrue();
    }
  }

  @Nested
  @DisplayName("executeWithRetry (streaming) interrupt")
  class ExecuteWithRetryStreamingInterrupt {

    @Test
    @DisplayName("returns RetryResult with InterruptedException when thread interrupted during sleep")
    void interruptDuringSleep() throws Exception {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 5000L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      AtomicBoolean interruptedResult = new AtomicBoolean(false);
      Thread worker = new Thread(() -> {
        StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                          () -> {
                                                                                            throw new TimeoutException("timeout");
                                                                                          },
                                                                                          config,
                                                                                          chunks);
        if (result.getLastException() != null
            && result.getLastException().getCause() instanceof InterruptedException) {
          interruptedResult.set(true);
        }
      });
      worker.start();
      Thread.sleep(50);
      worker.interrupt();
      worker.join(3000);
      assertThat(interruptedResult.get()).isTrue();
    }
  }

  @Nested
  @DisplayName("isRetryableException additional cases")
  class IsRetryableExceptionAdditionalCases {

    @Test
    @DisplayName("returns false for plain RuntimeException with no cause")
    void runtimeExceptionNoCause() {
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new RuntimeException("no cause")))
          .isFalse();
    }

    @Test
    @DisplayName("unwraps ExecutionException with SdkClientException cause")
    void executionExceptionWithSdkClientCause() {
      SdkClientException sdkEx = mock(SdkClientException.class);
      when(sdkEx.getMessage()).thenReturn("timeout");
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new ExecutionException(sdkEx)))
          .isTrue();
    }

    @Test
    @DisplayName("returns false for generic Exception that is not TimeoutException or SdkClientException")
    void genericExceptionNotRetryable() {
      assertThat(StreamingRetryUtility.isRetryableException(
                                                            new Exception("generic exception")))
          .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"network", "connect timeout"})
    @DisplayName("returns true for SdkClientException with network-related messages")
    void sdkClientExceptionNetworkMessages(String message) {
      SdkClientException e = mock(SdkClientException.class);
      when(e.getMessage()).thenReturn(message);
      assertThat(StreamingRetryUtility.isRetryableException(e)).isTrue();
    }
  }

  @Nested
  @DisplayName("executeWithRetry edge cases")
  class ExecuteWithRetryEdgeCases {

    @Test
    @DisplayName("non-streaming: wraps exception with cause when checking fails")
    void nonStreamingWrapsWithCause() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(1, 1L, true);
      RuntimeException withCause = new RuntimeException("wrapper", new IllegalStateException("cause"));
      assertThatThrownBy(() -> StreamingRetryUtility.executeWithRetry(() -> {
        throw withCause;
      }, config)).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("wrapper");
    }

    @Test
    @DisplayName("streaming: exhausts all retries and returns failure")
    void streamingExhaustsRetries() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      int[] attempts = {0};
      SdkClientException retryable = mock(SdkClientException.class);
      when(retryable.getMessage()).thenReturn("timeout");
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          attempts[0]++;
                                                                                          throw retryable;
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isFalse();
      assertThat(result.getAttemptsMade()).isEqualTo(3); // 1 + 2 retries
    }

    @Test
    @DisplayName("streaming: resets chunksReceived flag on retry")
    void streamingResetsChunksFlag() {
      StreamingRetryUtility.RetryConfig config =
          new StreamingRetryUtility.RetryConfig(2, 1L, true);
      AtomicBoolean chunks = new AtomicBoolean(false);
      int[] attempts = {0};
      SdkClientException retryable = mock(SdkClientException.class);
      when(retryable.getMessage()).thenReturn("timeout");
      StreamingRetryUtility.RetryResult result = StreamingRetryUtility.executeWithRetry(
                                                                                        () -> {
                                                                                          if (attempts[0]++ < 1) {
                                                                                            throw retryable;
                                                                                          }
                                                                                          chunks.set(true);
                                                                                        },
                                                                                        config,
                                                                                        chunks);
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.isChunksReceived()).isTrue();
    }
  }
}
