package org.mule.extension.bedrock.internal.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbedding;
import org.mule.extension.bedrock.api.params.BedrockParametersEmbeddingDocument;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class EmbeddingServiceImpl extends BedrockServiceImpl implements EmbeddingService {

  private static final Logger logger = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

  public EmbeddingServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  private static String getAmazonTitanEmbeddingG1(String prompt) {

    return new JSONObject()
        .put("inputText", prompt)
        .toString();
  }

  private static String getAmazonTitanEmbeddingG2(String prompt, BedrockParametersEmbedding awsBedrockParameters) {

    return new JSONObject()
        .put("inputText", prompt)
        .put("dimensions", awsBedrockParameters.getDimension())
        .put("normalize", awsBedrockParameters.getNormalize())
        .toString();
  }

  private static String getAmazonTitanEmbeddingG2Doc(String prompt,
                                                     BedrockParametersEmbeddingDocument awsBedrockParameters) {

    return new JSONObject()
        .put("inputText", prompt)
        .put("dimensions", awsBedrockParameters.getDimension())
        .put("normalize", awsBedrockParameters.getNormalize())
        .toString();
  }

  private static String getAmazonTitanImageEmbeddingG1(String prompt,
                                                       BedrockParametersEmbedding awsBedrockParameters) {

    JSONObject embeddingConfig = new JSONObject();
    embeddingConfig.put("outputEmbeddingLength", 256);

    JSONObject body = new JSONObject();
    body.put("inputText", prompt);
    body.put("embeddingConfig", embeddingConfig);

    return body.toString();

  }

  private static String getAmazonTitanImageEmbeddingG1Doc(String prompt,
                                                          BedrockParametersEmbeddingDocument awsBedrockParameters) {

    JSONObject embeddingConfig = new JSONObject();
    embeddingConfig.put("outputEmbeddingLength", 256);

    JSONObject body = new JSONObject();
    body.put("inputText", prompt);
    body.put("embeddingConfig", embeddingConfig);

    return body.toString();

  }

  private static String getCoherEmbeddingModelDoc(String prompt,
                                                  BedrockParametersEmbeddingDocument awsBedrockParameters) {

    JSONObject jsonObject = new JSONObject();

    // Add "texts" array
    JSONArray textsArray = new JSONArray();
    for (String text : prompt.split(".")) {
      textsArray.put(text);
    }
    jsonObject.put("texts", textsArray);

    // Add other fields
    jsonObject.put("input_type", "search_query");

    return jsonObject.toString();
  }

  private static String getCoherEmbeddingModel(String prompt, BedrockParametersEmbedding awsBedrockParameters) {

    JSONObject jsonObject = new JSONObject();

    // Add "texts" array
    JSONArray textsArray = new JSONArray();
    for (String text : prompt.split(".")) {
      textsArray.put(text);
    }
    jsonObject.put("texts", textsArray);

    // Add other fields
    jsonObject.put("input_type", "search_query");

    return jsonObject.toString();
  }

  @Override
  public String generateEmbeddings(String prompt, BedrockParametersEmbedding bedrockEmbeddingParameters) {

    String modelId = bedrockEmbeddingParameters.getModelName();

    String body = identifyPayload(prompt, bedrockEmbeddingParameters);

    JSONObject response = generateEmbedding(modelId, body);

    return response.toString();
  }

  private JSONObject generateEmbedding(String modelId, String body) {
    InvokeModelRequest request = createInvokeRequest(modelId, body);

    InvokeModelResponse response = getConnection().invokeModel(request);

    String responseBody = new String(response.body().asByteArray(), StandardCharsets.UTF_8);

    return new JSONObject(responseBody);
  }

  private static InvokeModelRequest createInvokeRequest(String modelId, String nativeRequest) {

    return InvokeModelRequest.builder()
        .body(SdkBytes.fromUtf8String(nativeRequest))
        .accept("application/json")
        .contentType("application/json")
        .modelId(modelId)
        .build();
  }

  private static String identifyPayload(String prompt, BedrockParametersEmbedding awsBedrockParameters) {
    return identifyPayloadInternal(prompt, awsBedrockParameters.getModelName(), awsBedrockParameters);
  }

  private static String identifyPayloadInternal(String prompt, String modelName, Object awsBedrockParameters) {
    if (modelName.contains("amazon.titan-embed-text-v1")) {
      return getAmazonTitanEmbeddingG1(prompt);
    } else if (modelName.contains("amazon.titan-embed-text-v2:0")) {
      if (awsBedrockParameters instanceof BedrockParametersEmbedding) {
        return getAmazonTitanEmbeddingG2(prompt, (BedrockParametersEmbedding) awsBedrockParameters);
      } else {
        return getAmazonTitanEmbeddingG2Doc(prompt,
                                            (BedrockParametersEmbeddingDocument) awsBedrockParameters);
      }
    } else if (modelName.contains("amazon.titan-embed-image-v1")) {
      if (awsBedrockParameters instanceof BedrockParametersEmbedding) {
        return getAmazonTitanImageEmbeddingG1(prompt, (BedrockParametersEmbedding) awsBedrockParameters);
      } else {
        return getAmazonTitanImageEmbeddingG1Doc(prompt,
                                                 (BedrockParametersEmbeddingDocument) awsBedrockParameters);
      }
    } else if (modelName.contains("cohere.embed")) {
      if (awsBedrockParameters instanceof BedrockParametersEmbedding) {
        return getCoherEmbeddingModel(prompt, (BedrockParametersEmbedding) awsBedrockParameters);
      } else {
        return getCoherEmbeddingModelDoc(prompt, (BedrockParametersEmbeddingDocument) awsBedrockParameters);
      }
    } else {
      return "Unsupported model";
    }
  }

  @Override
  public String invokeAdhocRAG(String prompt, String filePath, BedrockParametersEmbeddingDocument bedrockEmbeddingParameters)
      throws IOException, SAXException, TikaException {

    String modelId = bedrockEmbeddingParameters.getModelName();
    List<String> corpus;
    if (bedrockEmbeddingParameters.getOptionType().equals("FULL")) {
      corpus = Arrays.asList(splitFullDocument(filePath, bedrockEmbeddingParameters));
    } else {
      corpus = Arrays.asList(splitByType(filePath, bedrockEmbeddingParameters));
    }

    String body = identifyPayloadDoc(prompt, bedrockEmbeddingParameters);

    try {

      JSONObject queryResponse = generateEmbedding(modelId, body);
      // Generate embedding for query
      JSONArray queryEmbedding = queryResponse.getJSONArray("embedding");

      String corpusBody = null;
      // Generate embeddings for the corpus
      List<JSONArray> corpusEmbeddings = new ArrayList<>();
      for (String text : corpus) {
        corpusBody = identifyPayloadDoc(text, bedrockEmbeddingParameters);
        // logger.info(corpusBody);
        if (text != null && !text.isEmpty()) {
          body = identifyPayloadDoc(corpusBody, bedrockEmbeddingParameters);
          corpusEmbeddings
              .add(generateEmbedding(modelId, body).getJSONArray("embedding"));
        }
      }

      // Compare embeddings and rank results
      List<Double> similarityScores = new ArrayList<>();
      for (JSONArray corpusEmbedding : corpusEmbeddings) {
        similarityScores.add(calculateCosineSimilarity(queryEmbedding, corpusEmbedding));
      }

      // Rank and print results
      List<String> results = rankAndPrintResults(corpus, similarityScores);

      // Convert results list to a JSONArray
      JSONArray jsonArray = new JSONArray(results);

      return jsonArray.toString();

    } catch (Exception e) {
      logger.error("Error: {}", e.getMessage(), e);
      return null;

    }
  }


  private static String splitFullDocument(String filePath, BedrockParametersEmbeddingDocument awsBedrockParameters)
      throws IOException, SAXException, TikaException {
    String content = getContentFromFile(filePath);
    return content;
  }

  private static String getContentFromFile(String filePath) throws IOException, SAXException, TikaException {
    BodyContentHandler handler = new BodyContentHandler();
    Metadata metadata = new Metadata();
    FileInputStream inputstream = new FileInputStream(new File(filePath));
    ParseContext pcontext = new ParseContext();

    // parsing the document using PDF parser
    PDFParser pdfparser = new PDFParser();
    pdfparser.parse(inputstream, handler, metadata, pcontext);
    return handler.toString();
  }

  private static String[] splitByType(String filePath, BedrockParametersEmbeddingDocument awsBedrockParameters)
      throws IOException, SAXException, TikaException {
    String content = getContentFromFile(filePath);
    String[] parts = splitContent(content, awsBedrockParameters.getOptionType());
    return parts;
  }

  private static String[] splitContent(String text, String option) {
    switch (option) {
      case "PARAGRAPH":
        return splitByParagraphs(text);
      case "SENTENCES":
        return splitBySentences(text);
      default:
        throw new IllegalArgumentException("Unknown split option: " + option);
    }
  }

  private static String[] splitByParagraphs(String text) {
    // Assuming paragraphs are separated by two or more newlines

    return removeEmptyStrings(text.split("\\r?\\n\\r?\\n"));
  }

  private static String[] splitBySentences(String text) {
    // Split by sentences (simple implementation using period followed by space)
    return removeEmptyStrings(text.split("(?<!Mr|Mrs|Ms|Dr|Sr|Jr|Prof)\\.\\s+"));
  }

  public static String[] removeEmptyStrings(String[] array) {
    // Convert array to list
    List<String> list = new ArrayList<>(Arrays.asList(array));

    // Remove empty strings from the list
    list.removeIf(String::isEmpty);

    // Convert list back to array
    return list.toArray(new String[0]);
  }

  private static String identifyPayloadDoc(String prompt,
                                           BedrockParametersEmbeddingDocument awsBedrockParameters) {
    return identifyPayloadInternal(prompt, awsBedrockParameters.getModelName(), awsBedrockParameters);
  }

  private static double calculateCosineSimilarity(JSONArray vec1, JSONArray vec2) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < vec1.length(); i++) {
      double a = vec1.getDouble(i);
      double b = vec2.getDouble(i);
      dotProduct += a * b;
      normA += Math.pow(a, 2);
      normB += Math.pow(b, 2);
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  private static List<String> rankAndPrintResults(List<String> corpus, List<Double> similarityScores) {
    List<Integer> indices = new ArrayList<>();
    logger.info("Corpus size: {}", corpus.size());
    for (int i = 0; i < corpus.size(); i++) {
      indices.add(i);
    }

    indices.sort((i, j) -> Double.compare(similarityScores.get(j), similarityScores.get(i)));

    logger.info("Ranked results:");
    List<String> results = new ArrayList<>();
    for (int index : indices) {
      logger.info("Score: {} - Text: {}", similarityScores.get(index), corpus.get(index));
      results.add(similarityScores.get(index) + " - " + corpus.get(index));
    }

    return results;
  }
}
