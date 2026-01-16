package org.mule.extension.bedrock.internal.helper.request;

import java.util.List;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;
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
  private String modelId;
  private String accountId;
  private String region;

  private ConverseStreamRequestBuilder(BedrockParameters parameters, String prompt) {
    this.parameters = parameters;
    this.prompt = prompt;
  }

  /**
   * Creates a new builder instance.
   *
   * @param parameters the bedrock parameters
   * @param prompt the user prompt
   * @return new builder instance
   */
  public static ConverseStreamRequestBuilder create(BedrockParameters parameters, String prompt) {
    return new ConverseStreamRequestBuilder(parameters, prompt);
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
    accountId = ModelIdentifier.getAccountIdOrDefault(parameters.getAwsAccountId());
    region = parameters.getRegion();

    logger.debug("accountId: {}", accountId);

    if (ModelIdentifier.requiresInferenceProfileArn(modelId)) {
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
