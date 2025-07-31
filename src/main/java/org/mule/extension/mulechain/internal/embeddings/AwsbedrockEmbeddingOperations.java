package org.mule.extension.mulechain.internal.embeddings;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.tika.exception.TikaException;
import org.mule.extension.mulechain.helpers.AwsbedrockEmbeddingPayloadHelper;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.xml.sax.SAXException;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class AwsbedrockEmbeddingOperations {

  /**
   * Generate embeddings for text
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-generate-from-text")
  public InputStream generateEmbeddings(String prompt, @Config AwsbedrockConfiguration configuration,
                                        @ParameterGroup(
                                            name = "Additional properties") AwsbedrockParametersEmbedding awsBedrockParameters) {
    String response = AwsbedrockEmbeddingPayloadHelper.invokeModel(prompt, configuration, awsBedrockParameters);
    return toInputStream(response, StandardCharsets.UTF_8);
  }

  /**
   * Performs retrieval augmented generation based on files
   * 
   * @throws TikaException
   * @throws SAXException
   * @throws IOException
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Alias("EMBEDDING-adhoc-query")
  public InputStream ragEmbeddingTextScore(String prompt, String filePath,
                                           @Config AwsbedrockConfiguration configuration,
                                           @ParameterGroup(
                                               name = "Additional properties") AwsbedrockParametersEmbeddingDocument awsBedrockParameters)
      throws IOException, SAXException, TikaException {
    String response = AwsbedrockEmbeddingPayloadHelper.InvokeAdhocRAG(prompt, filePath, configuration,
                                                                      awsBedrockParameters);
    return toInputStream(response, StandardCharsets.UTF_8);
  }

}
