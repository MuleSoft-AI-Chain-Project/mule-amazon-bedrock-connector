package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbedding;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbeddingDocument;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.EmbeddingService;
import org.mule.extension.bedrock.internal.service.EmbeddingServiceImpl;
import org.mule.extension.bedrock.internal.util.BedrockModelFactory;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.xml.sax.SAXException;

public class EmbeddingOperation extends BedrockOperation<EmbeddingService> {

  public EmbeddingOperation() {
    super(EmbeddingServiceImpl::new);
  }

  /**
   * Generates vector embeddings from text using Amazon Bedrock embedding models. Embeddings are numerical representations of text
   * that capture semantic meaning.
   *
   * @param prompt The text to generate embeddings for.
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param awsBedrockParameters Additional properties including embedding model name and region.
   * @return InputStream containing the JSON response with the embedding vector (array of floating-point numbers) representing the
   *         semantic meaning of the input text.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("EMBEDDING-generate-from-text")
  @Execution(ExecutionType.BLOCKING)
  @Summary("Generate embeddings from input text")
  public InputStream generateEmbedding(String prompt,
                                       @Config BedrockConfiguration config,
                                       @Connection BedrockConnection connection,
                                       @ParameterGroup(
                                           name = "Additional properties") BedrockParametersEmbedding awsBedrockParameters) {
    return newExecutionBuilder(config, connection)
        .execute(EmbeddingService::generateEmbeddings, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(awsBedrockParameters);
  }

  /**
   * Performs Retrieval Augmented Generation (RAG) by generating embeddings for a query and a document, then ranking document
   * chunks by similarity to the query.
   *
   * @param prompt The query text to search for in the document.
   * @param filePath File system path to the document file (PDF supported).
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param awsBedrockParameters Additional properties including embedding model name, region, and document splitting options
   *        (FULL, PARAGRAPH, SENTENCES).
   * @return InputStream containing the JSON response with ranked document chunks ordered by similarity score to the query.
   * @throws IOException If the document file cannot be read.
   * @throws SAXException If there is an error parsing the document XML structure.
   * @throws TikaException If there is an error extracting text from the document using Apache Tika.
   */

  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Alias("EMBEDDING-adhoc-query")
  @Execution(ExecutionType.BLOCKING)
  @Summary("Run an ad-hoc embedding query")
  public InputStream ragEmbeddingTextScore(String prompt, String filePath,
                                           @Config BedrockConfiguration config,
                                           @Connection BedrockConnection connection,
                                           @ParameterGroup(
                                               name = "Additional properties") BedrockParametersEmbeddingDocument awsBedrockParameters)
      throws IOException, SAXException, TikaException {
    return newExecutionBuilder(config, connection)
        .execute(EmbeddingService::invokeAdhocRAG, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(filePath)
        .withParam(awsBedrockParameters);
  }
}
