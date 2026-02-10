package com.mulesoft.connectors.bedrock.internal.service;

import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParameters;
import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;

public interface ChatService extends ConnectorService {

  String answerPrompt(String prompt, BedrockParameters bedrockParameters);

  InputStream answerPromptStreaming(String prompt, BedrockParameters bedrockParameters);
}
