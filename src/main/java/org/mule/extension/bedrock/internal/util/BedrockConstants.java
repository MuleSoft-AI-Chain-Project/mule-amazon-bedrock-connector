package org.mule.extension.bedrock.internal.util;

/**
 * Constants used throughout the Bedrock connector. Centralizes magic strings and numbers to improve maintainability.
 */
public final class BedrockConstants {

  private BedrockConstants() {
    // Utility class - prevent instantiation
  }

  // AWS Configuration
  public static final String DEFAULT_AWS_ACCOUNT_ID = "076261412953";
  public static final String INFERENCE_PROFILE_ARN_TEMPLATE = "arn:aws:bedrock:%s:%s:inference-profile/us.%s";

  // Model ID Patterns
  public static final class ModelPatterns {

    private ModelPatterns() {}

    public static final String AMAZON_TITAN_TEXT = "amazon.titan-text";
    public static final String AMAZON_NOVA = "amazon.nova";
    public static final String AMAZON_NOVA_PREMIER = "amazon.nova-premier";
    public static final String ANTHROPIC_CLAUDE = "anthropic.claude";
    public static final String ANTHROPIC_CLAUDE_3 = "anthropic.claude-3";
    public static final String AI21_JAMBA = "ai21.jamba";
    public static final String MISTRAL = "mistral";
    public static final String MISTRAL_PIXTRAL = "mistral.pixtral";
    public static final String MISTRAL_MISTRAL = "mistral.mistral";
    public static final String COHERE_COMMAND = "cohere.command";
    public static final String META_LLAMA = "meta.llama";
    public static final String META_LLAMA_3_1 = "meta.llama3-1";
    public static final String META_LLAMA_3_2 = "meta.llama3-2";
    public static final String META_LLAMA_3_3 = "meta.llama3-3";
    public static final String META_LLAMA_4 = "meta.llama4";
    public static final String STABILITY_STABLE = "stability.stable";
    public static final String GOOGLE_GEMMA = "google.gemma";
    public static final String OPENAPI_GPT = "openai.gpt";
  }

  // Response Formatting
  public static final class ResponseGroups {

    private ResponseGroups() {}

    public static final String CLAUDE = "claude";
    public static final String MISTRAL_PIXTRAL = "mistral.pixtral";
    public static final String MISTRAL_MISTRAL = "mistral.mistral";
    public static final String JAMBA = "jamba";
    public static final String LLAMA = "llama";
    public static final String TITAN = "titan";
    public static final String DEFAULT = "default";
  }

  // Question Detection
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

  public static final String QUESTION_MARK = "?";
  public static final String SPACE = " ";

  // JSON Keys
  public static final class JsonKeys {

    private JsonKeys() {}

    public static final String INPUT_TEXT = "inputText";
    public static final String TEXT_GENERATION_CONFIG = "textGenerationConfig";
    public static final String TEMPERATURE = "temperature";
    public static final String TOP_P = "topP";
    public static final String MAX_TOKEN_COUNT = "maxTokenCount";
    public static final String TEXT = "text";
    public static final String CONTENT = "content";
    public static final String ROLE = "role";
    public static final String USER = "user";
    public static final String MESSAGES = "messages";
    public static final String INFERENCE_CONFIG = "inferenceConfig";
    public static final String MAX_NEW_TOKENS = "max_new_tokens";
    public static final String TOP_K = "top_k";
    public static final String PROMPT = "prompt";
    public static final String MAX_TOKENS_TO_SAMPLE = "max_tokens_to_sample";
    public static final String MAX_TOKENS = "maxTokens";
    public static final String MAX_TOKENS_LIMIT = "MAX_TOKENS";
    public static final String P = "p";
    public static final String K = "k";
    public static final String MAX_GEN_LEN = "max_gen_len";
    public static final String TEXT_PROMPTS = "text_prompts";
    public static final String OUTPUT = "output";
    public static final String MESSAGE = "message";
    public static final String ASSISTANT = "assistant";
    public static final String STOP_REASON = "stopReason";
    public static final String USAGE = "usage";
    public static final String INPUT_TOKENS = "inputTokens";
    public static final String OUTPUT_TOKENS = "outputTokens";
    public static final String TOTAL_TOKENS = "totalTokens";
    public static final String GUARDRAIL_ACTION = "amazon-bedrock-guardrailAction";
  }

  // Error Messages
  public static final class ErrorMessages {

    private ErrorMessages() {}

    public static final String MODEL_INVOCATION_FAILED = "Failed to invoke model: %s";
    public static final String UNSUPPORTED_MODEL = "Unsupported model";
    public static final String STREAMING_PIPE_CREATION_FAILED = "Failed to create streaming pipes";
  }

  // Claude Prompt Format
  public static final String CLAUDE_HUMAN_PREFIX = "\n\nHuman:";
  public static final String CLAUDE_ASSISTANT_SUFFIX = "\n\nAssistant:";
}
