package org.mule.extension.mulechain.helpers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.image.AwsbedrockImageParameters;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import java.awt.image.BufferedImage;

;

public class AwsbedrockImagePayloadHelper {


    private static final Logger logger = LoggerFactory.getLogger(AwsbedrockImagePayloadHelper.class);

  private static String getAmazonTitanImage(String prompt, String avoidInImage, AwsbedrockImageParameters awsBedrockParameters) {

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

private static String getAmazonNovaImage(String prompt, String avoidInImage, AwsbedrockImageParameters awsBedrockParameters) {
      //Since the payload is the same as Titan models, we'll simply call the getAmazonTitanImage to avoid code duplication
    return getAmazonTitanImage(prompt, avoidInImage, awsBedrockParameters);
}

private static String getStabilityAiDiffusionImage(String prompt, String avoidInImage, AwsbedrockImageParameters awsBedrockParameters) {
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


  private static String identifyPayload(String prompt, String avoidInImage, AwsbedrockImageParameters awsBedrockParameters){
    if (awsBedrockParameters.getModelName().contains("amazon.titan-image")) {
        return getAmazonTitanImage(prompt, avoidInImage, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("amazon.nova")) {
        return getAmazonNovaImage(prompt, avoidInImage, awsBedrockParameters);
    }
    else if (awsBedrockParameters.getModelName().contains("stability.stable-diffusion-xl")) {
        return getStabilityAiDiffusionImage(prompt, avoidInImage, awsBedrockParameters);
    } else {

        return "Unsupported model";
    }

  }



    private static BedrockRuntimeClient createClient(AwsbedrockConfiguration configuration, Region region) {

    // Initialize the AWS credentials
    
    //AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey());

    AwsCredentials awsCredentials;

    if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
        awsCredentials = AwsBasicCredentials.create(
            configuration.getAwsAccessKeyId(), 
            configuration.getAwsSecretAccessKey()
        );
    } else {
        awsCredentials = AwsSessionCredentials.create(
            configuration.getAwsAccessKeyId(), 
            configuration.getAwsSecretAccessKey(), 
            configuration.getAwsSessionToken());
    }


    return BedrockRuntimeClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .region(region)
            .build();
}

private static InvokeModelRequest createInvokeRequest(String modelId, String nativeRequest) {
    return InvokeModelRequest.builder()
            .body(SdkBytes.fromUtf8String(nativeRequest))
            .modelId(modelId)
            .build();
}

public static byte[] generateImage(String modelId, String body, AwsbedrockConfiguration configuration, Region region) throws IOException {
    BedrockRuntimeClient bedrock = createClient(configuration, region);

    InvokeModelRequest request = createInvokeRequest(modelId, body);

    InvokeModelResponse response = bedrock.invokeModel(request);

    JSONObject responseBody = new JSONObject(response.body().asUtf8String());

    byte[] imageBytes = null;

    if (modelId.contains("amazon.titan-image") || modelId.contains("amazon.nova") ) {
        String base64Image = responseBody.getJSONArray("images").getString(0);
       imageBytes = Base64.getDecoder().decode(base64Image);
    } else if (modelId.contains("stability.stable-diffusion-xl")) {
        JSONArray artifactsArray = responseBody.getJSONArray("artifacts");
        String base64Image = artifactsArray.getJSONObject(0).getString("base64");

        // Decode the base64 string
        imageBytes = Base64.getDecoder().decode(base64Image);
    } 





    // String base64Image = responseBody.getJSONArray("images").getString(0);
    // byte[] imageBytes = Base64.getDecoder().decode(base64Image);

    String finishReason = responseBody.optString("error", null);
    if (finishReason != null) {
        throw new RuntimeException("Image generation error. Error is " + finishReason);
    }

    return imageBytes;
}

   public static String invokeModel(String prompt, String avoidInImage, String fullPath, AwsbedrockConfiguration configuration, AwsbedrockImageParameters awsBedrockParameters) {

        Region region = AwsbedrockPayloadHelper.getRegion(awsBedrockParameters.getRegion());

        String modelId = awsBedrockParameters.getModelName();


         String body = identifyPayload(prompt, avoidInImage, awsBedrockParameters); 

        logger.info(body);

        try {
            byte[] imageBytes = generateImage(modelId, body, configuration, region);

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

}
