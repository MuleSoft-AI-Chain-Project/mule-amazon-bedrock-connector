package com.mulesoft.connectors.bedrock.internal.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Utility class for handling retries in streaming operations. Implements exponential backoff and retryable exception detection.
 */
public class StreamingRetryUtility {

  private static final Logger logger = LoggerFactory.getLogger(StreamingRetryUtility.class);

  /**
   * Retry configuration for streaming operations
   */
  public static class RetryConfig {

    private final int maxRetries;
    private final long baseBackoffMs;
    private final boolean enabled;

    public RetryConfig(int maxRetries, long baseBackoffMs, boolean enabled) {
      this.maxRetries = maxRetries;
      this.baseBackoffMs = baseBackoffMs;
      this.enabled = enabled;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public long getBaseBackoffMs() {
      return baseBackoffMs;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /**
   * Result of a retry attempt
   */
  public static class RetryResult {

    private final boolean success;
    private final Exception lastException;
    private final int attemptsMade;
    private final boolean chunksReceived;

    public RetryResult(boolean success, Exception lastException, int attemptsMade, boolean chunksReceived) {
      this.success = success;
      this.lastException = lastException;
      this.attemptsMade = attemptsMade;
      this.chunksReceived = chunksReceived;
    }

    public boolean isSuccess() {
      return success;
    }

    public Exception getLastException() {
      return lastException;
    }

    public int getAttemptsMade() {
      return attemptsMade;
    }

    public boolean isChunksReceived() {
      return chunksReceived;
    }
  }

  /**
   * Determines if a throwable is retryable for operations. Only retries on timeout and network-related errors. Handles both
   * Exception and RuntimeException (including CompletionException).
   *
   * @param throwable The throwable to check
   * @return true if the throwable is retryable, false otherwise
   */
  public static boolean isRetryableException(Throwable throwable) {
    if (throwable == null) {
      return false;
    }
    if (throwable instanceof CompletionException) {
      return isRetryableCompletionException((CompletionException) throwable);
    }
    if (throwable instanceof SdkClientException) {
      return isRetryableSdkClientException((SdkClientException) throwable);
    }
    if (throwable instanceof TimeoutException) {
      return true;
    }
    if (throwable instanceof ExecutionException) {
      return isRetryableExecutionException((ExecutionException) throwable);
    }
    if (throwable instanceof RuntimeException) {
      return isRetryableRuntimeException((RuntimeException) throwable);
    }
    if (throwable instanceof Exception) {
      return isRetryableGenericException((Exception) throwable);
    }
    return false;
  }

  private static boolean isRetryableCompletionException(CompletionException throwable) {
    Throwable cause = throwable.getCause();
    return cause != null && isRetryableException(cause);
  }

  private static boolean isRetryableExecutionException(ExecutionException throwable) {
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return false;
    }
    if (cause instanceof TimeoutException) {
      return true;
    }
    if (cause instanceof SdkClientException) {
      return isRetryableSdkClientException((SdkClientException) cause);
    }
    return isRetryableException(cause);
  }

  private static boolean isRetryableRuntimeException(RuntimeException throwable) {
    Throwable cause = throwable.getCause();
    return cause != null && isRetryableException(cause);
  }

  private static boolean isRetryableGenericException(Exception exception) {
    if (exception instanceof TimeoutException) {
      return true;
    }
    if (exception instanceof SdkClientException) {
      return isRetryableSdkClientException((SdkClientException) exception);
    }
    return false;
  }

  /**
   * Checks if an SdkClientException is retryable based on its message. SdkClientException typically indicates network/timeout
   * issues, so we check for common patterns.
   *
   * @param exception The SdkClientException to check
   * @return true if retryable, false otherwise
   */
  private static boolean isRetryableSdkClientException(SdkClientException exception) {
    String message = exception.getMessage();
    if (message == null) {
      // If message is null, check if it's a generic client exception (likely network/timeout related)
      // Most SdkClientExceptions are retryable unless they're clearly not (e.g., configuration errors)
      return true;
    }

    String lowerMessage = message.toLowerCase();
    return lowerMessage.contains("timeout") ||
        lowerMessage.contains("timed out") ||
        lowerMessage.contains("connection") ||
        lowerMessage.contains("network") ||
        lowerMessage.contains("socket") ||
        lowerMessage.contains("read timeout") ||
        lowerMessage.contains("connect timeout") ||
        lowerMessage.contains("unable to execute") ||
        lowerMessage.contains("http request") ||
        lowerMessage.contains("failed to connect") ||
        lowerMessage.contains("connection refused") ||
        lowerMessage.contains("connection reset");
  }

  /**
   * Calculates exponential backoff delay in milliseconds. Formula: baseBackoffMs * (2 ^ attempt)
   *
   * @param attempt The current attempt number (0-based)
   * @param baseBackoffMs The base backoff delay in milliseconds
   * @return The calculated backoff delay in milliseconds
   */
  public static long calculateExponentialBackoff(int attempt, long baseBackoffMs) {
    return baseBackoffMs * (1L << attempt);
  }

  /**
   * Executes a non-streaming operation with retry logic. This version doesn't track chunks and can always retry on retryable
   * exceptions.
   *
   * @param operation The operation to execute
   * @param config Retry configuration
   * @param <T> The return type of the operation
   * @return The result of the operation
   * @throws RuntimeException if all retries are exhausted
   */
  public static <T> T executeWithRetry(
                                       java.util.function.Supplier<T> operation,
                                       RetryConfig config) {
    if (!config.isEnabled()) {
      return operation.get();
    }

    RuntimeException lastException = null;
    int attemptsMade = 0;

    for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
      attemptsMade++;
      try {
        return operation.get();
      } catch (RuntimeException e) {
        lastException = e;
        Throwable exceptionToCheck = getExceptionToCheck(e);
        if (!canRetryNonStreaming(attempt, config, exceptionToCheck)) {
          throw cannotRetryException(e, exceptionToCheck, attemptsMade, config);
        }
        sleepBeforeRetry(attempt, config.getBaseBackoffMs(), attemptsMade, e.getMessage());
      }
    }

    throw new RuntimeException(
                               createRetryErrorMessage(getExceptionMessage(lastException), attemptsMade, config.getMaxRetries(),
                                                       false),
                               lastException);
  }

  private static Throwable getExceptionToCheck(RuntimeException e) {
    Throwable cause = e.getCause();
    return cause != null ? cause : e;
  }

  private static boolean canRetryNonStreaming(int attempt, RetryConfig config, Throwable exceptionToCheck) {
    return attempt < config.getMaxRetries() && isRetryableException(exceptionToCheck);
  }

  private static RuntimeException cannotRetryException(RuntimeException e, Throwable exceptionToCheck,
                                                       int attemptsMade, RetryConfig config) {
    String reason = isRetryableException(exceptionToCheck)
        ? "max retries (" + config.getMaxRetries() + ") exceeded"
        : "exception is not retryable: " + e.getClass().getSimpleName()
            + (exceptionToCheck != e ? " (cause: " + exceptionToCheck.getClass().getSimpleName() + ")" : "");
    logger.warn("Cannot retry operation (attempt {}): {}", attemptsMade, reason);
    return new RuntimeException(
                                createRetryErrorMessage(getExceptionMessage(e), attemptsMade, config.getMaxRetries(), false),
                                e);
  }

  private static String getExceptionMessage(Throwable t) {
    if (t == null) {
      return "Unknown exception";
    }
    return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
  }

  private static void sleepBeforeRetry(int attempt, long baseBackoffMs, int attemptsMade, String lastMessage) {
    long backoffMs = calculateExponentialBackoff(attempt, baseBackoffMs);
    logger.warn("Retryable error on attempt {}: {}. Retrying in {}ms", attemptsMade, lastMessage, backoffMs);
    try {
      Thread.sleep(backoffMs);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Retry interrupted", ie);
    }
  }

  /**
   * Executes a streaming operation with retry logic. Only retries if no chunks have been received yet.
   *
   * @param operation The operation to execute (should set chunksReceived to true when chunks arrive)
   * @param config Retry configuration
   * @param chunksReceived AtomicBoolean to track if chunks have been received
   * @return RetryResult containing success status, exception, attempts made, and chunks received flag
   */
  public static RetryResult executeWithRetry(
                                             StreamingOperation operation,
                                             RetryConfig config,
                                             AtomicBoolean chunksReceived) {

    if (!config.isEnabled()) {
      return executeStreamingOnce(operation, chunksReceived);
    }

    Exception lastException = null;
    int attemptsMade = 0;

    for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
      attemptsMade++;
      try {
        if (attempt > 0) {
          chunksReceived.set(false);
        }
        operation.execute();
        return new RetryResult(true, null, attemptsMade, chunksReceived.get());
      } catch (Exception e) {
        lastException = e;
        boolean chunksWereReceived = chunksReceived.get();
        if (!canRetryStreaming(attempt, config, e, chunksWereReceived)) {
          return failStreamingRetry(e, attemptsMade, chunksWereReceived, config);
        }
        RetryResult interrupted = sleepBeforeStreamingRetry(attempt, config, attemptsMade, e.getMessage(),
                                                            chunksWereReceived);
        if (interrupted != null) {
          return interrupted;
        }
      }
    }

    return new RetryResult(false, lastException, attemptsMade, chunksReceived.get());
  }

  private static RetryResult executeStreamingOnce(StreamingOperation operation, AtomicBoolean chunksReceived) {
    try {
      operation.execute();
      return new RetryResult(true, null, 1, chunksReceived.get());
    } catch (Exception e) {
      return new RetryResult(false, e, 1, chunksReceived.get());
    }
  }

  private static boolean canRetryStreaming(int attempt, RetryConfig config, Exception e, boolean chunksWereReceived) {
    return attempt < config.getMaxRetries() && isRetryableException(e) && !chunksWereReceived;
  }

  private static RetryResult failStreamingRetry(Exception e, int attemptsMade, boolean chunksWereReceived,
                                                RetryConfig config) {
    String reason = buildStreamingCannotRetryReason(e, chunksWereReceived, config);
    logger.warn("Cannot retry streaming operation (attempt {}): {}", attemptsMade, reason);
    return new RetryResult(false, e, attemptsMade, chunksWereReceived);
  }

  private static String buildStreamingCannotRetryReason(Exception e, boolean chunksWereReceived, RetryConfig config) {
    if (chunksWereReceived) {
      return "chunks were already received";
    }
    if (!isRetryableException(e)) {
      return "exception is not retryable: " + e.getClass().getSimpleName();
    }
    return "max retries (" + config.getMaxRetries() + ") exceeded";
  }

  /** Returns non-null RetryResult if interrupted, null to continue retrying. */
  private static RetryResult sleepBeforeStreamingRetry(int attempt, RetryConfig config, int attemptsMade,
                                                       String lastMessage, boolean chunksWereReceived) {
    long backoffMs = calculateExponentialBackoff(attempt, config.getBaseBackoffMs());
    logger.warn("Retryable error on attempt {}: {}. Retrying in {}ms", attemptsMade, lastMessage, backoffMs);
    try {
      Thread.sleep(backoffMs);
      return null;
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return new RetryResult(false, new Exception("Retry interrupted", ie), attemptsMade, chunksWereReceived);
    }
  }

  /**
   * Functional interface for streaming operations that can be retried.
   */
  @FunctionalInterface
  public interface StreamingOperation {

    /**
     * Executes the streaming operation. Should set chunksReceived to true when chunks start arriving.
     *
     * @throws Exception if the operation fails
     */
    void execute() throws Exception;
  }

  /**
   * Creates an enhanced error message that includes retry attempt information.
   *
   * @param originalMessage The original error message
   * @param attemptsMade The number of retry attempts made
   * @param maxRetries The maximum number of retries configured
   * @param chunksReceived Whether chunks were received before failure
   * @return Enhanced error message with retry information
   */
  public static String createRetryErrorMessage(String originalMessage, int attemptsMade,
                                               int maxRetries, boolean chunksReceived) {
    StringBuilder errorMsg = new StringBuilder(originalMessage);

    if (attemptsMade > 1) {
      errorMsg.append(" (Connector retry: failed after ").append(attemptsMade).append(" attempt(s)");
      if (maxRetries > 0) {
        errorMsg.append(" out of ").append(maxRetries + 1).append(" total connector attempts");
      }
      errorMsg.append(")");
    }

    if (chunksReceived) {
      errorMsg.append(" - Note: Some chunks were received before failure, so retry was not attempted");
    }

    return errorMsg.toString();
  }
}
