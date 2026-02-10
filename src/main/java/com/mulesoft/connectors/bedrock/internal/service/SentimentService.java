package com.mulesoft.connectors.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;

public interface SentimentService extends ConnectorService {

  String extractSentiments(String textToAnalyze, BedrockParameters bedrockParameters);
}
