package org.mule.extension.bedrock.internal.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.bedrock.api.params.BedrockImageParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class ImageServiceImpl extends BedrockServiceImpl implements ImageService {

  @FunctionalInterface
  private interface TriFunction<A, B, C, R> {

    R apply(A a, B b, C c);
  }

  @FunctionalInterface
  private interface PayloadGenerator extends TriFunction<String, String, BedrockImageParameters, String> {
  }

  @FunctionalInterface
  private interface ImageBytesExtractor extends Function<JSONObject, byte[]> {
  }

  private static final Map<ModelProvider, PayloadGenerator> payloadGeneratorMap = new EnumMap<>(ModelProvider.class);
  private static final Map<ModelProvider, ImageBytesExtractor> imageBytesExtractorMap = new EnumMap<>(
                                                                                                      ModelProvider.class);

  static {
    payloadGeneratorMap.put(ModelProvider.AMAZON, ImageServiceImpl::getAmazonTitanImage);
    payloadGeneratorMap.put(ModelProvider.AMAZON_NOVA, ImageServiceImpl::getAmazonNovaImage);
    payloadGeneratorMap.put(ModelProvider.STABILITY, ImageServiceImpl::getStabilityAiDiffusionImage);

    imageBytesExtractorMap.put(ModelProvider.AMAZON, responseBody -> {
      String base64Image = responseBody.getJSONArray("images").getString(0);
      return Base64.getDecoder().decode(base64Image);
    });
    imageBytesExtractorMap.put(ModelProvider.AMAZON_NOVA, responseBody -> {
      String base64Image = responseBody.getJSONArray("images").getString(0);
      return Base64.getDecoder().decode(base64Image);
    });
    imageBytesExtractorMap.put(ModelProvider.STABILITY, responseBody -> {
      JSONArray artifactsArray = responseBody.getJSONArray("artifacts");
      String base64Image = artifactsArray.getJSONObject(0).getString("base64");
      return Base64.getDecoder().decode(base64Image);
    });
  }

  private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

  public ImageServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  private static String getAmazonTitanImage(String prompt, String avoidInImage,
                                            BedrockImageParameters awsBedrockParameters) {

    return new JSONObject()
        .put("taskType", "TEXT_IMAGE")
        .put("textToImageParams", new JSONObject()
            .put("text", prompt)
            .put("negativeText", avoidInImage))
        .put("imageGenerationConfig", new JSONObject()
            .put("numberOfImages", awsBedrockParameters.getNumOfImages())
            .put("height", awsBedrockParameters.getHeight())
            .put("width", awsBedrockParameters.getWidth())
            .put("cfgScale", awsBedrockParameters.getCfgScale())
            .put("seed", awsBedrockParameters.getSeed()))
        .toString();
  }

  private static String getAmazonNovaImage(String prompt, String avoidInImage,
                                           BedrockImageParameters awsBedrockParameters) {
    // Since the payload is the same as Titan models, we'll simply call the
    // getAmazonTitanImage to avoid code duplication
    return getAmazonTitanImage(prompt, avoidInImage, awsBedrockParameters);
  }

  private static String getStabilityAiDiffusionImage(String prompt, String avoidInImage,
                                                     BedrockImageParameters awsBedrockParameters) {
    JSONArray textPromptsArray = new JSONArray()
        .put(new JSONObject()
            .put("text", prompt)
            .put("weight", 0));

    // Construct the main JSON object
    JSONObject json = new JSONObject()
        .put("text_prompts", textPromptsArray)
        .put("height", awsBedrockParameters.getHeight())
        .put("width", awsBedrockParameters.getWidth())
        .put("cfg_scale", awsBedrockParameters.getCfgScale())
        .put("seed", awsBedrockParameters.getSeed());

    return json.toString();
  }

  @Override
  public String invokeModel(String prompt, String avoidInImage, String fullPath, BedrockImageParameters awsBedrockParameters) {

    String modelId = awsBedrockParameters.getModelName();

    String body = identifyPayload(prompt, avoidInImage, awsBedrockParameters);

    logger.debug(body);

    try {
      byte[] imageBytes = generateImage(modelId, body);

      // Convert byte array to BufferedImage
      ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
      BufferedImage bufferedImage = ImageIO.read(bis);
      bis.close();

      // Save the image to a file
      String filePath = fullPath;
      File outputImageFile = new File(filePath);
      ImageIO.write(bufferedImage, "png", outputImageFile);

      // Display image
      if (bufferedImage != null) {
        logger.info("Successfully generated image.");
      } else {
        logger.warn("Failed to generate image.");
      }
      return filePath;

    } catch (Exception e) {
      logger.error("Error: {}", e.getMessage(), e);
      return null;

    }
  }

  private byte[] generateImage(String modelId, String body) {
    InvokeModelRequest request = InvokeModelRequest.builder()
        .body(SdkBytes.fromUtf8String(body))
        .modelId(modelId)
        .build();

    InvokeModelResponse response = getConnection().invokeModel(request);

    JSONObject responseBody = new JSONObject(response.body().asUtf8String());

    byte[] imageBytes = ModelProvider.fromModelId(modelId)
        .map(provider -> imageBytesExtractorMap.get(provider).apply(responseBody))
        .orElseThrow(() -> new RuntimeException("Unsupported model: " + modelId));

    String finishReason = responseBody.optString("error", null);
    if (finishReason != null) {
      throw new RuntimeException("Image generation error. Error is " + finishReason);
    }

    return imageBytes;
  }

  private static String identifyPayload(String prompt, String avoidInImage,
                                        BedrockImageParameters awsBedrockParameters) {
    return ModelProvider.fromModelId(awsBedrockParameters.getModelName())
        .map(provider -> payloadGeneratorMap.get(provider).apply(prompt, avoidInImage, awsBedrockParameters))
        .orElse("Unsupported model");
  }



}
