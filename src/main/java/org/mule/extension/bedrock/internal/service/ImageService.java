package org.mule.extension.bedrock.internal.service;

import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockImageParameters;

public interface ImageService extends ConnectorService {

  public String invokeModel(String prompt, String avoidInImage, String fullPath,
                            BedrockImageParameters awsBedrockParameters);

}
