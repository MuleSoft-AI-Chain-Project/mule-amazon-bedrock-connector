package org.mule.extension.mulechain.internal;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.mule.extension.mulechain.helpers.AwsbedrockPayloadHelper;
import org.mule.extension.mulechain.helpers.AwsbedrockPromptTemplateHelper;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class AwsbedrockOperations {

  private static final Logger logger = LoggerFactory.getLogger(AwsbedrockOperations.class);

  /**
   * Implements a simple Chat agent
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("CHAT-answer-prompt")
  public InputStream answerPrompt(String prompt, @Config AwsbedrockConfiguration configuration,
                                  @ParameterGroup(name = "Additional properties") AwsbedrockParameters awsBedrockParameters) {
    String response = AwsbedrockPayloadHelper.invokeModel(prompt, configuration, awsBedrockParameters);
    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * Helps defining an AI Agent with a prompt template
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("AGENT-define-prompt-template")
  public InputStream definePromptTemplate(String template, String instructions, String dataset,
                                          @Config AwsbedrockConfiguration configuration,
                                          @ParameterGroup(
                                              name = "Additional properties") AwsbedrockParameters awsBedrockParameters) {

    String finalPromptTemplate = AwsbedrockPromptTemplateHelper.definePromptTemplate(template, instructions, dataset);
    logger.info(finalPromptTemplate);

    String response = AwsbedrockPayloadHelper.invokeModel(finalPromptTemplate, configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * Example of a sentiment analyzer, which accepts text as input.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("SENTIMENT-analyze")
  public InputStream extractSentiments(String TextToAnalyze, @Config AwsbedrockConfiguration configuration,
                                       @ParameterGroup(
                                           name = "Additional properties") AwsbedrockParameters awsBedrockParameters) {

    String SentimentTemplate = "Analyze sentiment of: " + TextToAnalyze
        + ". Does it have a positive sentiment? Respond in JSON with Sentiment (value of POSITIVE, NEGATIVE, NEUTRAL) and IsPositive (true or false)";

    String response = AwsbedrockPayloadHelper.invokeModel(SentimentTemplate, configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * Get foundational models by Id.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("FOUNDATIONAL-model-details")
  public InputStream getFoundationalModelByModelId(@Config AwsbedrockConfiguration configuration,
                                                   @ParameterGroup(
                                                       name = "Additional properties") AwsbedrockParamsModelDetails awsBedrockParameters) {

    String response = AwsbedrockPayloadHelper.getFoundationModel(configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * List foundational models by Id.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("FOUNDATIONAL-models-list")
  public InputStream getFoundationalModelList(@Config AwsbedrockConfiguration configuration,
                                              @ParameterGroup(
                                                  name = "Additional properties") AwsbedrockParams awsBedrockParameters) {

    String response = AwsbedrockPayloadHelper.listFoundationModels(configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * Get custom models by Id.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("CUSTOM-model-details")
  public InputStream getCustomModelByModelId(@Config AwsbedrockConfiguration configuration,
                                             @ParameterGroup(
                                                 name = "Additional properties") AwsbedrockParamsModelDetails awsBedrockParameters) {

    String response = AwsbedrockPayloadHelper.getCustomModel(configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * List custom models by Id.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("CUSTOM-models-list")
  @Throws(BedrockErrorsProvider.class)
  public InputStream getCustomModelList(@Config AwsbedrockConfiguration configuration,
                                        @ParameterGroup(name = "Additional properties") AwsbedrockParams awsBedrockParameters) {

    String response = AwsbedrockPayloadHelper.listCustomModels(configuration, awsBedrockParameters);

    return toInputStream(response, StandardCharsets.UTF_8);
  }

}
