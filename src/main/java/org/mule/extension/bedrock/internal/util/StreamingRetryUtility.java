package org.mule.extension.bedrock.internal.util;

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

    // Check for CompletionException (from CompletableFuture.join()) - unwrap it
    if (throwable instanceof CompletionException) {
      Throwable cause = throwable.getCause();
      if (cause != null) {
        return isRetryableException(cause);
      }
      return false;
    }

    // Check for SdkClientException FIRST (before generic RuntimeException check)
    // SdkClientException extends RuntimeException, so we need to check it before the generic check
    if (throwable instanceof SdkClientException) {
      return isRetryableSdkClientException((SdkClientException) throwable);
    }

    // Check for TimeoutException
    if (throwable instanceof TimeoutException) {
      return true;
    }

    // Check for ExecutionException wrapping timeout or network errors
    if (throwable instanceof ExecutionException) {
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

      // Check nested exceptions
      return isRetryableException(cause);
    }

    // Check for other RuntimeException wrapping retryable exceptions (but not SdkClientException or CompletionException)
    if (throwable instanceof RuntimeException) {
      Throwable cause = throwable.getCause();
      if (cause != null) {
        return isRetryableException(cause);
      }
      return false;
    }

    // Check for Exception types (for any other Exception that's not RuntimeException)
    if (throwable instanceof Exception) {
      Exception exception = (Exception) throwable;

      // Check for TimeoutException
      if (exception instanceof TimeoutException) {
        return true;
      }

      // Check for SdkClientException (shouldn't reach here if above check worked, but just in case)
      if (exception instanceof SdkClientException) {
        return isRetryableSdkClientException((SdkClientException) exception);
      }
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

        // Unwrap CompletionException to get the underlying exception
        Throwable cause = e.getCause();
        Throwable exceptionToCheck = cause != null ? cause : e;

        // Check if we can retry - need to check the underlying cause
        boolean canRetry = attempt < config.getMaxRetries()
            && isRetryableException(exceptionToCheck);

        if (!canRetry) {
          String reason = !isRetryableException(exceptionToCheck)
              ? "exception is not retryable: " + e.getClass().getSimpleName()
                  + (exceptionToCheck != e ? " (cause: " + exceptionToCheck.getClass().getSimpleName() + ")" : "")
              : "max retries (" + config.getMaxRetries() + ") exceeded";

          logger.warn("Cannot retry operation (attempt {}): {}", attemptsMade, reason);
          throw new RuntimeException(createRetryErrorMessage(
                                                             e.getMessage() != null ? e.getMessage()
                                                                 : e.getClass().getSimpleName(),
                                                             attemptsMade,
                                                             config.getMaxRetries(),
                                                             false),
                                     e);
        }

        // Calculate exponential backoff
        long backoffMs = calculateExponentialBackoff(attempt, config.getBaseBackoffMs());
        logger.warn("Retryable error on attempt {}: {}. Retrying in {}ms",
                    attemptsMade, e.getMessage(), backoffMs);

        // Wait before retrying
        try {
          Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Retry interrupted", ie);
        }
      }
    }

    // All retries exhausted
    throw new RuntimeException(createRetryErrorMessage(
                                                       lastException.getMessage() != null ? lastException.getMessage()
                                                           : lastException.getClass().getSimpleName(),
                                                       attemptsMade,
                                                       config.getMaxRetries(),
                                                       false),
                               lastException);
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
      // Retry disabled - execute once
      try {
        operation.execute();
        return new RetryResult(true, null, 1, chunksReceived.get());
      } catch (Exception e) {
        return new RetryResult(false, e, 1, chunksReceived.get());
      }
    }

    Exception lastException = null;
    int attemptsMade = 0;

    for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
      attemptsMade++;

      try {
        // Reset chunksReceived for each attempt (except first)
        if (attempt > 0) {
          chunksReceived.set(false);
        }

        // Execute the operation
        operation.execute();

        // Success - exit retry loop
        return new RetryResult(true, null, attemptsMade, chunksReceived.get());

      } catch (Exception e) {
        lastException = e;
        boolean chunksWereReceived = chunksReceived.get();

        // Check if we can retry
        boolean canRetry = attempt < config.getMaxRetries()
            && isRetryableException(e)
            && !chunksWereReceived; // CRITICAL: Only retry if no chunks received

        if (!canRetry) {
          String reason;
          if (chunksWereReceived) {
            reason = "chunks were already received";
          } else if (!isRetryableException(e)) {
            reason = "exception is not retryable: " + e.getClass().getSimpleName();
          } else {
            reason = "max retries (" + config.getMaxRetries() + ") exceeded";
          }

          logger.warn("Cannot retry streaming operation (attempt {}): {}", attemptsMade, reason);
          return new RetryResult(false, e, attemptsMade, chunksWereReceived);
        }

        // Calculate exponential backoff
        long backoffMs = calculateExponentialBackoff(attempt, config.getBaseBackoffMs());
        logger.warn("Retryable error on attempt {}: {}. Retrying in {}ms",
                    attemptsMade, e.getMessage(), backoffMs);

        // Wait before retrying
        try {
          Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return new RetryResult(false,
                                 new Exception("Retry interrupted", ie),
                                 attemptsMade,
                                 chunksWereReceived);
        }
      }
    }

    // All retries exhausted
    return new RetryResult(false, lastException, attemptsMade, chunksReceived.get());
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
