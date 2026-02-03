package org.mule.extension.bedrock.internal.service;

import java.io.InputStream;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockParameters;

public interface ChatService extends ConnectorService {

  public String answerPrompt(String prompt, BedrockParameters bedrockParameters);

  public InputStream answerPromptStreaming(String prompt, BedrockParameters bedrockParameters);
}
