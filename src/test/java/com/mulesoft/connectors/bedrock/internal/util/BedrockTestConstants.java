package com.mulesoft.connectors.bedrock.internal.util;

/**
 * Test-only constants. Do not use in main (production) code.
 */
public final class BedrockTestConstants {

  private BedrockTestConstants() {
    // Utility class - prevent instantiation
  }

  /** Question words used for test data (e.g. prompt/response formatting tests). */
  public static final String[] QUESTION_WORDS = {
      "who", "what", "when", "where", "why", "how",
      "tell", "tell me", "do you", "what is",
      "can you", "could you", "would you",
      "is there", "are there", "will you", "won't you",
      "can't you", "couldn't you", "wouldn't you",
      "is it", "isn't it", "are they", "aren't they",
      "will they", "won't they", "can they", "can't they",
      "could they", "couldn't they", "would they", "wouldn't they"
  };
}
