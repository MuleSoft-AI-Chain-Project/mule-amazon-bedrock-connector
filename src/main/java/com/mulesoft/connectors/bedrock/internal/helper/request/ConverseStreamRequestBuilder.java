package com.mulesoft.connectors.bedrock.internal.helper.request;

import java.util.List;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;
import com.mulesoft.connectors.bedrock.internal.util.ModelIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.GuardrailStreamConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

/**
 * Builder for constructing ConverseStreamRequest objects. Implements Builder pattern to simplify complex request construction.
 */
public final class ConverseStreamRequestBuilder {

  private static final Logger logger = LoggerFactory.getLogger(ConverseStreamRequestBuilder.class);

  private final BedrockParameters parameters;
  private final String prompt;
  private final String region;
  private String modelId;

  private ConverseStreamRequestBuilder(BedrockParameters parameters, String region, String prompt) {
    this.parameters = parameters;
    this.region = region;
    this.prompt = prompt;
  }

  /**
   * Creates a new builder instance.
   *
   * @param parameters the bedrock parameters
   * @param region the connection region (used for inference profile ARN)
   * @param prompt the user prompt
   * @return new builder instance
   */
  public static ConverseStreamRequestBuilder create(BedrockParameters parameters, String region, String prompt) {
    return new ConverseStreamRequestBuilder(parameters, region, prompt);
  }

  /**
   * Builds the ConverseStreamRequest with all configured parameters.
   *
   * @return configured ConverseStreamRequest
   */
  public ConverseStreamRequest build() {
    initializeModelConfiguration();

    ConverseStreamRequest.Builder requestBuilder = ConverseStreamRequest.builder()
        .modelId(modelId)
        .messages(createUserMessage())
        .inferenceConfig(createInferenceConfig());

    // Configure guardrail if both identifier and version are provided
    if (hasGuardrailConfiguration()) {
      requestBuilder.guardrailConfig(createGuardrailConfig());
    }

    return requestBuilder.build();
  }

  private void initializeModelConfiguration() {
    modelId = parameters.getModelName();

    if (ModelIdentifier.requiresInferenceProfileArn(modelId)) {
      String accountId = ModelIdentifier.requireAccountId(parameters.getAwsAccountId());
      logger.debug("accountId: {}", accountId);
      modelId = ModelIdentifier.buildInferenceProfileArn(region, accountId, modelId);
    }
  }

  private List<Message> createUserMessage() {
    Message userMessage = Message.builder()
        .role(ConversationRole.USER)
        .content(ContentBlock.fromText(prompt))
        .build();
    return List.of(userMessage);
  }

  private InferenceConfiguration createInferenceConfig() {
    return InferenceConfiguration.builder()
        .temperature(parameters.getTemperature())
        .topP(parameters.getTopP())
        .maxTokens(parameters.getMaxTokenCount())
        .build();
  }

  private boolean hasGuardrailConfiguration() {
    String guardrailIdentifier = parameters.getGuardrailIdentifier();
    String guardrailVersion = parameters.getGuardrailVersion();
    return guardrailIdentifier != null && !guardrailIdentifier.isBlank()
        && guardrailVersion != null && !guardrailVersion.isBlank();
  }

  private GuardrailStreamConfiguration createGuardrailConfig() {
    return GuardrailStreamConfiguration.builder()
        .guardrailIdentifier(parameters.getGuardrailIdentifier())
        .guardrailVersion(parameters.getGuardrailVersion())
        .build();
  }
}
