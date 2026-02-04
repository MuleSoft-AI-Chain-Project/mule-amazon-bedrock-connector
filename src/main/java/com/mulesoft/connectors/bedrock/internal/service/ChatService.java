package com.mulesoft.connectors.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.params.BedrockParameters;

public interface ChatService extends ConnectorService {

  public String answerPrompt(String prompt, BedrockParameters bedrockParameters);

  public InputStream answerPromptStreaming(String prompt, BedrockParameters bedrockParameters);
}
