package org.mule.extension.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockParameters;

public interface SentimentService extends ConnectorService {

  public String extractSentiments(String textToAnalyze, BedrockParameters bedrockParameters);
}
