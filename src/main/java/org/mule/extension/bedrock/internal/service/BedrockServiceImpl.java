package org.mule.extension.bedrock.internal.service;

import org.mule.connectors.commons.template.service.DefaultConnectorService;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;

public class BedrockServiceImpl extends DefaultConnectorService<BedrockConfiguration, BedrockConnection> {

  public BedrockServiceImpl(BedrockConfiguration bedrockConfiguration, BedrockConnection bedrockConnection) {
    super(bedrockConfiguration, bedrockConnection);
  }
}
