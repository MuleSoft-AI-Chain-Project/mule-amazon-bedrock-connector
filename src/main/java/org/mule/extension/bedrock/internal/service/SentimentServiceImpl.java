package org.mule.extension.bedrock.internal.service;

import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.error.ErrorHandler;
import org.mule.extension.bedrock.internal.helper.PromptPayloadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class SentimentServiceImpl extends BedrockServiceImpl implements SentimentService {

  private static final Logger logger = LoggerFactory.getLogger(SentimentServiceImpl.class);

  public SentimentServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String extractSentiments(String textToAnalyze, BedrockParameters bedrockParameters) {
    try {
      String SentimentTemplate = "Analyze sentiment of: " + textToAnalyze
          + ". Does it have a positive sentiment? Respond in JSON with Sentiment (value of POSITIVE, NEGATIVE, NEUTRAL) and IsPositive (true or false)";
      String nativeRequest = PromptPayloadHelper.identifyPayload(SentimentTemplate, bedrockParameters);
      logger.info("Native request: {}", nativeRequest);
      String region = getConnection().getRegion();
      InvokeModelRequest invokeModelRequest = PromptPayloadHelper.createInvokeRequest(bedrockParameters, region, nativeRequest);
      InvokeModelResponse invokeModelResponse = getConnection().answerPrompt(invokeModelRequest);
      return PromptPayloadHelper.formatBedrockResponse(bedrockParameters, invokeModelResponse);
    } catch (SdkClientException e) {
      throw ErrorHandler.handleSdkClientException(e, bedrockParameters.getModelName());
    }

  }
}
