package org.mule.extension.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockParamRegion;
import org.mule.extension.bedrock.api.params.BedrockParamsModelDetails;

public interface CustomModelService extends ConnectorService {

  public String getCustomModelByModelID(BedrockParamsModelDetails bedrockParamsModelDetails);

  public String listCustomModels(BedrockParamRegion bedrockParamRegion);
}
