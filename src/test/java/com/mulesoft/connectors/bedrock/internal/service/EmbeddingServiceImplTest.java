package com.mulesoft.connectors.bedrock.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbedding;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParametersEmbeddingDocument;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@DisplayName("EmbeddingServiceImpl")
class EmbeddingServiceImplTest {

  @Test
  @DisplayName("can be constructed with config and connection")
  void constructor() {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("implements EmbeddingService")
  void implementsEmbeddingService() {
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(mock(BedrockConfiguration.class), mock(BedrockConnection.class));
    assertThat(service).isInstanceOf(EmbeddingService.class);
  }

  @Test
  @DisplayName("generateEmbeddings returns JSON when connection returns embedding body")
  void generateEmbeddingsReturnsJson() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    String body = "{\"embedding\":[0.1,0.2,0.3]}";
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(body)).build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Sample text.", params);

    assertThat(result).isNotBlank();
    assertThat(result).contains("embedding");
  }

  @Test
  @DisplayName("generateEmbeddings with cohere.embed model returns embedding")
  void generateEmbeddingsCohere() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embeddings\":[[0.1,0.2]]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "cohere.embed-multilingual-v3");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Text for cohere.", params);

    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("generateEmbeddings with amazon.titan-embed-text-v2 returns embedding")
  void generateEmbeddingsTitanV2() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2,0.3]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v2:0");
    setField(params, "dimension", 256);
    setField(params, "normalize", true);
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Text.", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("generateEmbeddings with amazon.titan-embed-image returns embedding")
  void generateEmbeddingsTitanImage() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-image-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("Image text.", params);
    assertThat(result).isNotBlank();
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("generateEmbeddings throws SdkClientException when connection throws")
  void generateEmbeddingsThrowsWhenConnectionThrows() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.getRegion()).thenReturn("us-east-1");
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenThrow(software.amazon.awssdk.core.exception.SdkClientException.builder().message("timeout").build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.generateEmbeddings("text", params))
        .isInstanceOf(software.amazon.awssdk.core.exception.SdkClientException.class)
        .hasMessageContaining("timeout");
  }

  @Test
  @DisplayName("generateEmbeddings with unsupported model uses fallback body")
  void generateEmbeddingsUnsupportedModel() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    when(connection.invokeModel(any(InvokeModelRequest.class)))
        .thenReturn(InvokeModelResponse.builder()
            .body(SdkBytes.fromUtf8String("{\"embedding\":[0.0]}"))
            .build());

    BedrockParametersEmbedding params = new BedrockParametersEmbedding();
    setField(params, "modelName", "unsupported.embed-model-v1");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    String result = service.generateEmbeddings("text", params);
    assertThat(result).isNotBlank();
  }

  @Test
  @DisplayName("invokeAdhocRAG throws when file does not exist")
  void invokeAdhocRAGInvalidFileThrows() throws Exception {
    BedrockConfiguration config = mock(BedrockConfiguration.class);
    BedrockConnection connection = mock(BedrockConnection.class);
    BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
    setField(params, "modelName", "amazon.titan-embed-text-v1");
    setField(params, "optionType", "FULL");
    EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
    org.assertj.core.api.Assertions.assertThatThrownBy(
                                                       () -> service.invokeAdhocRAG("query", "/nonexistent/path/to/file.pdf",
                                                                                    params))
        .isInstanceOf(java.io.IOException.class);
  }

  @Test
  @DisplayName("document payload methods produce valid JSON when invoked via reflection")
  void documentPayloadMethodsProduceJson() throws Exception {
    BedrockParametersEmbeddingDocument docParams = new BedrockParametersEmbeddingDocument();
    setField(docParams, "modelName", "amazon.titan-embed-text-v2:0");
    setField(docParams, "dimension", 256);
    setField(docParams, "normalize", true);
    String prompt = "doc text";

    java.lang.reflect.Method g2Doc =
        EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal", String.class, String.class, Object.class);
    g2Doc.setAccessible(true);
    Object body = g2Doc.invoke(null, prompt, "amazon.titan-embed-text-v2:0", docParams);
    assertThat(body).isNotNull();
    assertThat(body.toString()).contains("inputText");

    setField(docParams, "modelName", "amazon.titan-embed-image-v1");
    body = g2Doc.invoke(null, prompt, "amazon.titan-embed-image-v1", docParams);
    assertThat(body).isNotNull();
    assertThat(body.toString()).contains("embeddingConfig");

    setField(docParams, "modelName", "cohere.embed-multilingual-v3");
    body = g2Doc.invoke(null, prompt, "cohere.embed-multilingual-v3", docParams);
    assertThat(body).isNotNull();
    assertThat(body.toString()).contains("texts");
  }

  @Nested
  @DisplayName("removeEmptyStrings")
  class RemoveEmptyStrings {

    @Test
    @DisplayName("removes empty strings from array")
    void removesEmptyStrings() {
      String[] input = {"a", "", "b", "", "c"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("returns empty array when all elements are empty")
    void allEmpty() {
      String[] input = {"", "", ""};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns same array when no empty strings")
    void noEmptyStrings() {
      String[] input = {"x", "y", "z"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);
      assertThat(result).containsExactly("x", "y", "z");
    }

    @Test
    @DisplayName("handles single element array")
    void singleElement() {
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {"only"})).containsExactly("only");
      assertThat(EmbeddingServiceImpl.removeEmptyStrings(new String[] {""})).isEmpty();
    }
  }

  @Nested
  @DisplayName("calculateCosineSimilarity")
  class CalculateCosineSimilarity {

    @Test
    @DisplayName("returns 1.0 for identical vectors")
    void identicalVectors() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("calculateCosineSimilarity",
                                                       org.json.JSONArray.class, org.json.JSONArray.class);
      m.setAccessible(true);

      org.json.JSONArray vec1 = new org.json.JSONArray(java.util.Arrays.asList(1.0, 2.0, 3.0));
      org.json.JSONArray vec2 = new org.json.JSONArray(java.util.Arrays.asList(1.0, 2.0, 3.0));

      double result = (double) m.invoke(null, vec1, vec2);
      assertThat(result).isCloseTo(1.0, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    @DisplayName("returns 0.0 for orthogonal vectors")
    void orthogonalVectors() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("calculateCosineSimilarity",
                                                       org.json.JSONArray.class, org.json.JSONArray.class);
      m.setAccessible(true);

      org.json.JSONArray vec1 = new org.json.JSONArray(java.util.Arrays.asList(1.0, 0.0));
      org.json.JSONArray vec2 = new org.json.JSONArray(java.util.Arrays.asList(0.0, 1.0));

      double result = (double) m.invoke(null, vec1, vec2);
      assertThat(result).isCloseTo(0.0, org.assertj.core.api.Assertions.within(0.0001));
    }

    @Test
    @DisplayName("returns -1.0 for opposite vectors")
    void oppositeVectors() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("calculateCosineSimilarity",
                                                       org.json.JSONArray.class, org.json.JSONArray.class);
      m.setAccessible(true);

      org.json.JSONArray vec1 = new org.json.JSONArray(java.util.Arrays.asList(1.0, 0.0, 0.0));
      org.json.JSONArray vec2 = new org.json.JSONArray(java.util.Arrays.asList(-1.0, 0.0, 0.0));

      double result = (double) m.invoke(null, vec1, vec2);
      assertThat(result).isCloseTo(-1.0, org.assertj.core.api.Assertions.within(0.0001));
    }
  }

  @Nested
  @DisplayName("rankAndPrintResults")
  class RankAndPrintResults {

    @Test
    @DisplayName("ranks results by similarity score descending")
    void ranksDescending() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("rankAndPrintResults",
                                                       java.util.List.class, java.util.List.class);
      m.setAccessible(true);

      java.util.List<String> corpus = java.util.Arrays.asList("low", "high", "medium");
      java.util.List<Double> scores = java.util.Arrays.asList(0.1, 0.9, 0.5);

      @SuppressWarnings("unchecked")
      java.util.List<String> result = (java.util.List<String>) m.invoke(null, corpus, scores);

      assertThat(result).hasSize(3);
      assertThat(result.get(0)).contains("0.9");
      assertThat(result.get(1)).contains("0.5");
      assertThat(result.get(2)).contains("0.1");
    }

    @Test
    @DisplayName("handles single element corpus")
    void singleElement() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("rankAndPrintResults",
                                                       java.util.List.class, java.util.List.class);
      m.setAccessible(true);

      java.util.List<String> corpus = java.util.Arrays.asList("only");
      java.util.List<Double> scores = java.util.Arrays.asList(0.5);

      @SuppressWarnings("unchecked")
      java.util.List<String> result = (java.util.List<String>) m.invoke(null, corpus, scores);

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).contains("only");
    }
  }

  @Nested
  @DisplayName("splitContent")
  class SplitContent {

    @Test
    @DisplayName("splits by paragraphs when PARAGRAPH option")
    void splitsByParagraphs() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitContent", String.class, String.class);
      m.setAccessible(true);

      String text = "Paragraph one.\n\nParagraph two.\n\nParagraph three.";
      String[] result = (String[]) m.invoke(null, text, "PARAGRAPH");

      assertThat(result).hasSize(3);
      assertThat(result[0]).isEqualTo("Paragraph one.");
      assertThat(result[1]).isEqualTo("Paragraph two.");
      assertThat(result[2]).isEqualTo("Paragraph three.");
    }

    @Test
    @DisplayName("splits by sentences when SENTENCES option")
    void splitsBySentences() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitContent", String.class, String.class);
      m.setAccessible(true);

      String text = "First sentence. Second sentence. Third sentence.";
      String[] result = (String[]) m.invoke(null, text, "SENTENCES");

      assertThat(result).hasSize(3);
      assertThat(result[0]).isEqualTo("First sentence");
      assertThat(result[1]).isEqualTo("Second sentence");
      assertThat(result[2]).isEqualTo("Third sentence.");
    }

    @Test
    @DisplayName("throws for unknown option")
    void throwsForUnknownOption() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitContent", String.class, String.class);
      m.setAccessible(true);

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> m.invoke(null, "text", "UNKNOWN"))
          .hasCauseInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("splitByParagraphs")
  class SplitByParagraphs {

    @Test
    @DisplayName("handles Windows line endings")
    void handlesWindowsLineEndings() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitByParagraphs", String.class);
      m.setAccessible(true);

      String text = "Para one.\r\n\r\nPara two.";
      String[] result = (String[]) m.invoke(null, text);

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("handles Unix line endings")
    void handlesUnixLineEndings() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitByParagraphs", String.class);
      m.setAccessible(true);

      String text = "Para one.\n\nPara two.";
      String[] result = (String[]) m.invoke(null, text);

      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("splitBySentences")
  class SplitBySentences {

    @Test
    @DisplayName("does not split on Mr. Mrs. Dr. etc.")
    void respectsTitles() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("splitBySentences", String.class);
      m.setAccessible(true);

      String text = "Dr. Smith went to see Mr. Jones. They had a meeting.";
      String[] result = (String[]) m.invoke(null, text);

      assertThat(result).hasSize(2);
      assertThat(result[0]).contains("Dr. Smith");
      assertThat(result[0]).contains("Mr. Jones");
    }
  }

  @Nested
  @DisplayName("identifyPayloadInternal")
  class IdentifyPayloadInternal {

    @Test
    @DisplayName("returns Unsupported model for unknown model")
    void unsupportedModel() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "unknown.model");

      String result = (String) m.invoke(null, "prompt", "unknown.model", params);
      assertThat(result).isEqualTo("Unsupported model");
    }

    @Test
    @DisplayName("returns Titan G1 payload for amazon.titan-embed-text-v1")
    void titanEmbedTextV1() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "amazon.titan-embed-text-v1");

      String result = (String) m.invoke(null, "test prompt", "amazon.titan-embed-text-v1", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("test prompt");
    }

    @Test
    @DisplayName("returns Titan G2 payload for amazon.titan-embed-text-v2:0 with Embedding params")
    void titanEmbedTextV2Embedding() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "amazon.titan-embed-text-v2:0");
      setField(params, "dimension", 512);
      setField(params, "normalize", true);

      String result = (String) m.invoke(null, "test", "amazon.titan-embed-text-v2:0", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("dimensions");
    }

    @Test
    @DisplayName("returns Titan G2 payload for amazon.titan-embed-text-v2:0 with Document params")
    void titanEmbedTextV2Document() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      setField(params, "modelName", "amazon.titan-embed-text-v2:0");
      setField(params, "dimension", 256);
      setField(params, "normalize", false);

      String result = (String) m.invoke(null, "test", "amazon.titan-embed-text-v2:0", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("dimensions");
    }

    @Test
    @DisplayName("returns Titan Image payload for amazon.titan-embed-image-v1 with Embedding params")
    void titanEmbedImageV1Embedding() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "amazon.titan-embed-image-v1");

      String result = (String) m.invoke(null, "test", "amazon.titan-embed-image-v1", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("embeddingConfig");
    }

    @Test
    @DisplayName("returns Titan Image payload for amazon.titan-embed-image-v1 with Document params")
    void titanEmbedImageV1Document() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      setField(params, "modelName", "amazon.titan-embed-image-v1");

      String result = (String) m.invoke(null, "test", "amazon.titan-embed-image-v1", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("embeddingConfig");
    }

    @Test
    @DisplayName("returns Cohere payload for cohere.embed with Embedding params")
    void cohereEmbedEmbedding() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "cohere.embed-multilingual-v3");

      String result = (String) m.invoke(null, "test.sentence", "cohere.embed-multilingual-v3", params);
      assertThat(result).contains("texts");
      assertThat(result).contains("input_type");
    }

    @Test
    @DisplayName("returns Cohere payload for cohere.embed with Document params")
    void cohereEmbedDocument() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("identifyPayloadInternal",
                                                       String.class, String.class, Object.class);
      m.setAccessible(true);

      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      setField(params, "modelName", "cohere.embed-english-v3");

      String result = (String) m.invoke(null, "test.sentence", "cohere.embed-english-v3", params);
      assertThat(result).contains("texts");
      assertThat(result).contains("input_type");
    }
  }

  @Nested
  @DisplayName("getAmazonTitanEmbeddingG1")
  class GetAmazonTitanEmbeddingG1 {

    @Test
    @DisplayName("generates payload with inputText")
    void generatesPayload() throws Exception {
      java.lang.reflect.Method m =
          EmbeddingServiceImpl.class.getDeclaredMethod("getAmazonTitanEmbeddingG1", String.class);
      m.setAccessible(true);

      String result = (String) m.invoke(null, "test prompt");
      assertThat(result).contains("inputText");
      assertThat(result).contains("test prompt");
    }
  }

  @Nested
  @DisplayName("getAmazonTitanEmbeddingG2")
  class GetAmazonTitanEmbeddingG2 {

    @Test
    @DisplayName("generates payload with dimensions and normalize")
    void generatesPayloadWithParams() throws Exception {
      java.lang.reflect.Method m = EmbeddingServiceImpl.class.getDeclaredMethod(
                                                                                "getAmazonTitanEmbeddingG2", String.class,
                                                                                BedrockParametersEmbedding.class);
      m.setAccessible(true);

      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "dimension", 1024);
      setField(params, "normalize", true);

      String result = (String) m.invoke(null, "embed this", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("dimensions");
      assertThat(result).contains("1024");
      assertThat(result).contains("normalize");
    }
  }

  @Nested
  @DisplayName("generateEmbeddings")
  class GenerateEmbeddings {

    @Test
    @DisplayName("calls connection.invokeModel and returns response")
    void callsInvokeModel() throws Exception {
      BedrockConfiguration config = mock(BedrockConfiguration.class);
      BedrockConnection connection = mock(BedrockConnection.class);

      software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse mockResponse =
          software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse.builder()
              .body(software.amazon.awssdk.core.SdkBytes.fromUtf8String("{\"embedding\": [0.1, 0.2]}"))
              .build();
      when(connection.invokeModel(any())).thenReturn(mockResponse);

      EmbeddingServiceImpl service = new EmbeddingServiceImpl(config, connection);
      BedrockParametersEmbedding params = new BedrockParametersEmbedding();
      setField(params, "modelName", "amazon.titan-embed-text-v1");

      String result = service.generateEmbeddings("test", params);

      assertThat(result).contains("embedding");
      verify(connection).invokeModel(any());
    }
  }

  @Nested
  @DisplayName("createInvokeRequest")
  class CreateInvokeRequest {

    @Test
    @DisplayName("creates request with correct parameters")
    void createsRequest() throws Exception {
      java.lang.reflect.Method m = EmbeddingServiceImpl.class.getDeclaredMethod(
                                                                                "createInvokeRequest", String.class,
                                                                                String.class);
      m.setAccessible(true);

      software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest result =
          (software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest) m.invoke(
                                                                                             null, "amazon.titan-embed-text-v1",
                                                                                             "{\"inputText\": \"test\"}");

      assertThat(result.modelId()).isEqualTo("amazon.titan-embed-text-v1");
      assertThat(result.accept()).isEqualTo("application/json");
      assertThat(result.contentType()).isEqualTo("application/json");
    }
  }

  @Nested
  @DisplayName("removeEmptyStrings additional tests")
  class RemoveEmptyStringsAdditional {

    @Test
    @DisplayName("removes empty strings from array")
    void removesEmptyStrings() {
      String[] input = {"hello", "", "world", "", "test"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);

      assertThat(result).containsExactly("hello", "world", "test");
    }

    @Test
    @DisplayName("returns same array when no empty strings")
    void returnsSameWhenNoEmpty() {
      String[] input = {"hello", "world"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);

      assertThat(result).containsExactly("hello", "world");
    }

    @Test
    @DisplayName("returns empty array when all strings are empty")
    void returnsEmptyWhenAllEmpty() {
      String[] input = {"", "", ""};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("handles single element array")
    void handlesSingleElement() {
      String[] input = {"single"};
      String[] result = EmbeddingServiceImpl.removeEmptyStrings(input);

      assertThat(result).containsExactly("single");
    }
  }

  @Nested
  @DisplayName("identifyPayloadDoc")
  class IdentifyPayloadDoc {

    @Test
    @DisplayName("calls identifyPayloadInternal with titan v1")
    void callsIdentifyPayloadInternalTitanV1() throws Exception {
      java.lang.reflect.Method m = EmbeddingServiceImpl.class.getDeclaredMethod(
                                                                                "identifyPayloadDoc", String.class,
                                                                                BedrockParametersEmbeddingDocument.class);
      m.setAccessible(true);

      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      setField(params, "modelName", "amazon.titan-embed-text-v1");

      String result = (String) m.invoke(null, "test prompt", params);

      assertThat(result).contains("inputText");
    }
  }

  @Nested
  @DisplayName("getAmazonTitanEmbeddingG2Doc")
  class GetAmazonTitanEmbeddingG2Doc {

    @Test
    @DisplayName("generates payload with dimensions and normalize")
    void generatesPayloadWithParams() throws Exception {
      java.lang.reflect.Method m = EmbeddingServiceImpl.class.getDeclaredMethod(
                                                                                "getAmazonTitanEmbeddingG2Doc", String.class,
                                                                                BedrockParametersEmbeddingDocument.class);
      m.setAccessible(true);

      BedrockParametersEmbeddingDocument params = new BedrockParametersEmbeddingDocument();
      setField(params, "dimension", 512);
      setField(params, "normalize", true);

      String result = (String) m.invoke(null, "embed this", params);
      assertThat(result).contains("inputText");
      assertThat(result).contains("dimensions");
      assertThat(result).contains("512");
    }
  }
}
