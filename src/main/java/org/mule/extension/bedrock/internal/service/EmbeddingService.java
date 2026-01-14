package org.mule.extension.bedrock.internal.service;

import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.mule.connectors.commons.template.service.ConnectorService;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbedding;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbeddingDocument;
import org.xml.sax.SAXException;

public interface EmbeddingService extends ConnectorService {

  public String generateEmbeddings(String prompt, BedrockParametersEmbedding bedrockEmbeddingParameters);

  public String invokeAdhocRAG(String prompt, String filePath, BedrockParametersEmbeddingDocument bedrockEmbeddingParameters)
      throws IOException, SAXException, TikaException;

}
