package org.mule.extension.bedrock.internal.helper;

import java.util.HashMap;
import java.util.Map;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.helper.payload.PayloadGenerator;
import org.mule.extension.bedrock.internal.helper.payload.PayloadGeneratorFactory;
import org.mule.extension.bedrock.internal.helper.response.ResponseFormatterFactory;
import org.mule.extension.bedrock.internal.util.ModelIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class PromptPayloadHelper {

  private static final Logger logger = LoggerFactory.getLogger(PromptPayloadHelper.class);

  /**
   * Identifies and generates the appropriate payload for the given model. Uses Strategy pattern with fallback to default payload
   * generator.
   *
   * @param prompt the user prompt
   * @param bedrockParameters the bedrock parameters
   * @return JSON payload as string
   */
  public static String identifyPayload(String prompt, BedrockParameters bedrockParameters) {
    String modelName = bedrockParameters.getModelName();

    // Use Factory to get appropriate generator (Strategy pattern)
    PayloadGenerator generator = PayloadGeneratorFactory.getGenerator(modelName);

    // Generate payload using the selected strategy
    return generator.generatePayload(prompt, bedrockParameters);
  }


  public static InvokeModelRequest createInvokeRequest(BedrockParameters bedrockParameters,
                                                       String region,
                                                       String nativeRequest) {

    String modelId = bedrockParameters.getModelName();
    logger.debug("modelId: {}", modelId);

    String accountId = ModelIdentifier.getAccountIdOrDefault(bedrockParameters.getAwsAccountId());
    logger.debug("accountId: {}", accountId);

    // for Anthropic Claude 3-x, mistral.pxtral, meta.llama3, prep the model id
    // using the following format
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.anthropic.claude-3-5-sonnet-20241022-v2:0
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.mistral.pixtral-large-2502-v1:0
    // arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.meta.llama3-3-70b-instruct-v1:0

    if (ModelIdentifier.requiresInferenceProfileArn(modelId)) {
      modelId = ModelIdentifier.buildInferenceProfileArn(region, accountId, modelId);
    }

    String guardrailIdentifier = bedrockParameters.getGuardrailIdentifier();
    String guardrailVersion = bedrockParameters.getGuardrailVersion();

    InvokeModelRequest request;
    if (guardrailIdentifier != null && !guardrailIdentifier.isEmpty() &&
        guardrailVersion != null && !guardrailVersion.isEmpty()) {

      // Both values are present ? specify guardrail
      request = InvokeModelRequest.builder()
          .modelId(modelId)
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .contentType("application/json")
          .guardrailIdentifier(guardrailIdentifier)
          .guardrailVersion(guardrailVersion)
          .build();

    } else {
      request = InvokeModelRequest.builder()
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .modelId(modelId)
          .build();
    }

    return request;
  }

  public static String formatBedrockResponse(BedrockParameters bedrockParameters,
                                             InvokeModelResponse response) {
    // Use Strategy pattern to get appropriate formatter
    String modelId = bedrockParameters.getModelName();
    return ResponseFormatterFactory.getFormatter(modelId).format(response);
  }

  public static String definePromptTemplate(String promptTemplate, String instructions, String dataSet) {
    // Create the final template by concatenating strings with line separators
    String finalTemplate = promptTemplate
        + System.lineSeparator()
        + "Instructions: "
        + "{{instructions}}"
        + System.lineSeparator()
        + "Dataset: "
        + "{{dataset}}";

    // Create a map for the variables
    Map<String, String> variables = new HashMap<>();
    variables.put("instructions", instructions);
    variables.put("dataset", dataSet);

    // Replace the placeholders with actual values
    return processTemplate(finalTemplate, variables);
  }

  private static String processTemplate(String template, Map<String, String> variables) {
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return template;
  }
}
