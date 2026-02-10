package com.mulesoft.connectors.bedrock.internal.service;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import org.mule.connectors.commons.template.service.ConnectorService;

public interface SentimentService extends ConnectorService {

  String extractSentiments(String textToAnalyze, BedrockParameters bedrockParameters);
}
