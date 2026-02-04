package com.mulesoft.connectors.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import com.mulesoft.connectors.bedrock.api.params.BedrockParamsModelDetails;

public interface FoundationalService extends ConnectorService {

  public String getFoundationModel(BedrockParamsModelDetails bedrockParamsModelDetails);

  public String listFoundationModels();
}
