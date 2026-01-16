package org.mule.extension.bedrock.internal.helper.streaming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

/**
 * Factory for creating ConverseStreamResponseHandler instances. Encapsulates complex handler construction logic.
 */
public final class StreamResponseHandlerFactory {

  private static final Logger logger = LoggerFactory.getLogger(StreamResponseHandlerFactory.class);

  private StreamResponseHandlerFactory() {
    // Utility class - prevent instantiation
  }

  /**
   * Creates a response handler for streaming with proper error handling and stream management.
   *
   * @param outputStream the output stream to write to
   * @param streamClosed atomic boolean to track stream state
   * @return configured ConverseStreamResponseHandler
   */
  public static ConverseStreamResponseHandler createHandler(PipedOutputStream outputStream,
                                                            AtomicBoolean streamClosed) {
    return ConverseStreamResponseHandler.builder()
        .onResponse(response -> {
          // Connection opened successfully
          logger.debug("Streaming connection opened");
        })
        .onEventStream(publisher -> {
          // Subscribe to event publisher
          publisher.subscribe(event -> {
            event.accept(createVisitor(outputStream, streamClosed));
          });
        })
        .onError(error -> {
          handleStreamError(error, outputStream, streamClosed);
        })
        .onComplete(() -> {
          handleStreamCompletion(outputStream, streamClosed);
        })
        .build();
  }

  private static ConverseStreamResponseHandler.Visitor createVisitor(PipedOutputStream outputStream,
                                                                     AtomicBoolean streamClosed) {
    return ConverseStreamResponseHandler.Visitor.builder()
        .onContentBlockDelta(deltaEvent -> {
          handleContentDelta(deltaEvent.delta().text(), outputStream, streamClosed);
        })
        .onMessageStop(stopEvent -> {
          handleMessageStop(outputStream, streamClosed);
        })
        .onDefault(unknown -> {
          handleUnknownEvent(unknown, outputStream, streamClosed);
        })
        .build();
  }

  private static void handleContentDelta(String text, PipedOutputStream outputStream, AtomicBoolean streamClosed) {
    if (text != null && !streamClosed.get()) {
      try {
        logger.debug("[STREAMING] {}", text);
        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      } catch (IOException e) {
        logger.error("Error writing to stream", e);
        // Stream may be closed by consumer, handle gracefully
        if (streamClosed.compareAndSet(false, true)) {
          closeQuietly(outputStream);
        }
      }
    }
  }

  private static void handleMessageStop(PipedOutputStream outputStream, AtomicBoolean streamClosed) {
    // End of this message - normal completion
    logger.debug("Message stream completed");
    if (streamClosed.compareAndSet(false, true)) {
      closeQuietly(outputStream);
    }
  }

  private static void handleUnknownEvent(Object unknown, PipedOutputStream outputStream, AtomicBoolean streamClosed) {
    // Optional: handle other event types
    logger.debug("Received unknown event type: {}", unknown.getClass().getSimpleName());

    // Don't close stream for informational events like MessageStart, ContentBlockStop, Metadata
    // Only close on actual completion/error events
    String eventType = unknown.getClass().getSimpleName();
    if (eventType.contains("Stop") || eventType.contains("Complete") || eventType.contains("Error")) {
      if (streamClosed.compareAndSet(false, true)) {
        closeQuietly(outputStream);
      }
    }
  }

  /**
   * Handles stream errors by writing error information and closing the stream. Public method for use by ChatServiceImpl.
   */
  public static void handleStreamError(Throwable error, PipedOutputStream outputStream, AtomicBoolean streamClosed) {
    // Handle error & cleanup
    logger.error("Error in streaming response: {}", error.getMessage(), error);
    if (streamClosed.compareAndSet(false, true)) {
      // Write error information to stream before closing
      try {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", true);
        errorJson.put("message", error.getMessage());
        errorJson.put("errorType", error.getClass().getSimpleName());
        String errorText = "\n[ERROR: " + errorJson.toString() + "]";
        outputStream.write(errorText.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      } catch (IOException e) {
        logger.warn("Could not write error to stream", e);
      } finally {
        closeQuietly(outputStream);
      }
    }
  }

  private static void handleStreamCompletion(PipedOutputStream outputStream, AtomicBoolean streamClosed) {
    // Stream closed normally
    logger.debug("Streaming completed");
    if (streamClosed.compareAndSet(false, true)) {
      closeQuietly(outputStream);
    }
  }

  private static void closeQuietly(OutputStream os) {
    try {
      os.close();
    } catch (IOException ignored) {
      // Ignore close errors
    }
  }
}
