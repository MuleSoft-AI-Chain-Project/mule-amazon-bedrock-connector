package org.mule.extension.mulechain.helpers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mule.extension.mulechain.internal.AwsbedrockConfiguration;
import org.mule.extension.mulechain.internal.AwsbedrockParameters;
import org.mule.extension.mulechain.internal.AwsbedrockParams;
import org.mule.extension.mulechain.internal.AwsbedrockParamsModelDetails;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.CustomModelSummary;
import software.amazon.awssdk.services.bedrock.model.FoundationModelDetails;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetCustomModelResponse;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListCustomModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ValidationException;
import java.util.List;

public class AwsbedrockPayloadHelper {

  private static BedrockRuntimeClient createClient(AwsBasicCredentials awsCreds, Region region) {
    return BedrockRuntimeClient.builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
    .region(region)
    .build();
  }


  private static BedrockRuntimeClient createClientSession(AwsSessionCredentials awsCreds, Region region) {
    return BedrockRuntimeClient.builder()
    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
    .region(region)
    .build();
  }


  private static InvokeModelRequest createInvokeRequest(String modelId, String nativeRequest) {
    return InvokeModelRequest.builder()
          .body(SdkBytes.fromUtf8String(nativeRequest))
          .modelId(modelId)
          .build();

  }

  public static Region getRegion(String region){
    switch (region) {
      case "us-east-1":
          return Region.US_EAST_1;
      case "us-east-2":
          return Region.US_EAST_2;
      case "us-west-1":
          return Region.US_WEST_1;
      case "us-west-2":
          return Region.US_WEST_2;
      case "af-south-1":
          return Region.AF_SOUTH_1;
      case "ap-east-1":
          return Region.AP_EAST_1;
      case "ap-south-1":
          return Region.AP_SOUTH_1;
      case "ap-south-2":
          return Region.AP_SOUTH_2;
      case "ap-southeast-1":
          return Region.AP_SOUTHEAST_1;
      case "ap-southeast-2":
          return Region.AP_SOUTHEAST_2;
      case "ap-southeast-3":
          return Region.AP_SOUTHEAST_3;
      case "ap-southeast-4":
          return Region.AP_SOUTHEAST_4;
      case "ap-northeast-1":
          return Region.AP_NORTHEAST_1;
      case "ap-northeast-2":
          return Region.AP_NORTHEAST_2;
      case "ap-northeast-3":
          return Region.AP_NORTHEAST_3;
      case "ca-central-1":
          return Region.CA_CENTRAL_1;
      case "eu-central-1":
          return Region.EU_CENTRAL_1;
      case "eu-central-2":
          return Region.EU_CENTRAL_2;
      case "eu-west-1":
          return Region.EU_WEST_1;
      case "eu-west-2":
          return Region.EU_WEST_2;
      case "eu-west-3":
          return Region.EU_WEST_3;
      case "eu-north-1":
          return Region.EU_NORTH_1;
      case "eu-south-1":
          return Region.EU_SOUTH_1;
      case "eu-south-2":
          return Region.EU_SOUTH_2;
      case "me-south-1":
          return Region.ME_SOUTH_1;
      case "me-central-1":
          return Region.ME_CENTRAL_1;
      case "sa-east-1":
          return Region.SA_EAST_1;
      case "us-gov-east-1":
          return Region.US_GOV_EAST_1;
      case "us-gov-west-1":
          return Region.US_GOV_WEST_1;
      default:
          throw new IllegalArgumentException("Unknown region: " + region);
  }
 }
  
  


  private static String getAmazonTitanText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inputText", prompt);

    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("temperature", awsBedrockParameters.getTemperature());
    textGenerationConfig.put("topP", awsBedrockParameters.getTopP());
    textGenerationConfig.put("maxTokenCount", awsBedrockParameters.getMaxTokenCount());

    jsonRequest.put("textGenerationConfig", textGenerationConfig);

    return jsonRequest.toString();
}

private static String getAmazonNovaText(String prompt, AwsbedrockParameters awsBedrockParameters) {

    JSONObject textObject = new JSONObject();
    textObject.put("text", prompt);

    // Create the "content" array containing the "text" object
    JSONArray contentArray = new JSONArray();
    contentArray.put(textObject);

    // Create the "messages" array and add the "user" message
    JSONObject userMessage = new JSONObject();
    userMessage.put("role", "user");
    userMessage.put("content", contentArray);

    JSONArray messagesArray = new JSONArray();
    messagesArray.put(userMessage);

    // Create the "inferenceConfig" object with optional parameters
    JSONObject inferenceConfig = new JSONObject();
    inferenceConfig.put("max_new_tokens", awsBedrockParameters.getMaxTokenCount());
    inferenceConfig.put("temperature", awsBedrockParameters.getTemperature());
    inferenceConfig.put("top_p", awsBedrockParameters.getTopP());
    inferenceConfig.put("top_k", awsBedrockParameters.getTopK());

    // Combine everything into the root JSON object
    JSONObject rootObject = new JSONObject();
    rootObject.put("messages", messagesArray);
    rootObject.put("inferenceConfig", inferenceConfig);

    return rootObject.toString();

}


private static String getStabilityTitanText(String prompt) {
    JSONObject jsonRequest = new JSONObject();
    JSONObject textGenerationConfig = new JSONObject();
    textGenerationConfig.put("text", prompt);

    jsonRequest.put("text_prompts", textGenerationConfig);

    return jsonRequest.toString();
}


private static String getAnthropicClaudeText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens_to_sample", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
}

private static String getMistralAIText(String prompt, AwsbedrockParameters awsBedrockParameters) {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
   //jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    //jsonRequest.put("top_k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
}


  private static String getAI21Text(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("topP", awsBedrockParameters.getTopP());
    jsonRequest.put("maxTokens", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}

private static String getCohereText(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("p", awsBedrockParameters.getTopP());
    jsonRequest.put("k", awsBedrockParameters.getTopK());
    jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}

private static String getLlamaText(String prompt, AwsbedrockParameters awsBedrockParameters){
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("prompt", prompt);
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
    jsonRequest.put("top_p", awsBedrockParameters.getTopP());
    jsonRequest.put("max_gen_len", awsBedrockParameters.getMaxTokenCount());
    
    return jsonRequest.toString();
}



  private static String identifyPayload(String prompt, AwsbedrockParameters awsBedrockParameters){
    if (awsBedrockParameters.getModelName().contains("amazon.titan-text")) {
        return getAmazonTitanText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("amazon.nova")) {
        System.out.println("Generating payload for nova");
        return getAmazonNovaText(prompt, awsBedrockParameters);
    }else if (awsBedrockParameters.getModelName().contains("anthropic.claude")) {
        return getAnthropicClaudeText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("ai21.j2")) {
        return getAI21Text(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("mistral.mistral")) {
        return getMistralAIText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("cohere.command")) {
        return getCohereText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("meta.llama")) {
        return getLlamaText(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("stability.stable")) {
        return getStabilityTitanText(prompt);
    } else {
        return "Unsupported model";
    }

  }


    private static BedrockRuntimeClient InitiateClient(AwsbedrockConfiguration configuration, AwsbedrockParameters awsBedrockParameters){
        // Initialize the AWS credentials
        //AwsCredentials awsCredentials;

        //AwsBasicCredentials awsCreds = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey());
        if (configuration.getAwsSessionToken() == null || configuration.getAwsSessionToken().isEmpty()) {
            AwsBasicCredentials awsCredsBasic = AwsBasicCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey());
            return createClient(awsCredsBasic, getRegion(awsBedrockParameters.getRegion()));
        } else {
            AwsSessionCredentials awsCredsSession = AwsSessionCredentials.create(configuration.getAwsAccessKeyId(), configuration.getAwsSecretAccessKey(), configuration.getAwsSessionToken());
            return createClientSession(awsCredsSession, getRegion(awsBedrockParameters.getRegion()));
        }
        
  }


  public static String invokeModel(String prompt, AwsbedrockConfiguration configuration, AwsbedrockParameters awsBedrockParameters) {

    // Create Bedrock Client 
    BedrockRuntimeClient client = InitiateClient(configuration, awsBedrockParameters);

    String nativeRequest = identifyPayload(prompt, awsBedrockParameters);

    try {
        // Encode and send the request to the Bedrock Runtime.
        InvokeModelRequest request = createInvokeRequest(awsBedrockParameters.getModelName(), nativeRequest);

        System.out.println("Native request: " + nativeRequest);

        InvokeModelResponse response = client.invokeModel(request);
        

        // Decode the response body.
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());


        //System.out.println(responseBody);
        // Retrieve the generated text from the model's response.
        //String text = new JSONPointer("/completions/0/data/text").queryFrom(responseBody).toString();
        //System.out.println(text);

        return responseBody.toString();

    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", awsBedrockParameters.getModelName(), e.getMessage());
        throw new RuntimeException(e);
    }
}


private static BedrockClient createBedrockClient(AwsbedrockConfiguration configuration, AwsbedrockParams awsBedrockParameters) {
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

    BedrockClient bedrockClient = BedrockClient.builder()
    .region(getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .build();

    return bedrockClient;
}


private static BedrockClient createBedrockClientDetails(AwsbedrockConfiguration configuration, AwsbedrockParamsModelDetails awsBedrockParameters) {
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

    BedrockClient bedrockClient = BedrockClient.builder()
    .region(getRegion(awsBedrockParameters.getRegion())) 
    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
    .build();

    return bedrockClient;
}




public static String getFoundationModel(AwsbedrockConfiguration configuration, AwsbedrockParamsModelDetails awsBedrockParameters) {

    BedrockClient bedrockClient = createBedrockClientDetails(configuration, awsBedrockParameters);


    try {
        
        GetFoundationModelResponse response = bedrockClient.getFoundationModel(
            GetFoundationModelRequest.builder()
                .modelIdentifier(awsBedrockParameters.getModelName())
                .build()
        );
        
        FoundationModelDetails model = response.modelDetails();

        JSONObject jsonModel = new JSONObject();
        jsonModel.put("modelId", model.modelId());
        jsonModel.put("modelArn", model.modelArn());
        jsonModel.put("modelName", model.modelName());
        jsonModel.put("providerName", model.providerName());
        jsonModel.put("modelLifecycleStatus", model.modelLifecycle().statusAsString());
        jsonModel.put("inputModalities", model.inputModalities());
        jsonModel.put("outputModalities", model.outputModalities());
        jsonModel.put("customizationsSupported", model.customizationsSupported());
        jsonModel.put("inferenceTypesSupported", model.inferenceTypesSupported());
        jsonModel.put("responseStreamingSupported", model.responseStreamingSupported());

        return jsonModel.toString();

    } catch (ValidationException e) {
        throw new IllegalArgumentException(e.getMessage());
    } catch (SdkException e) {
        System.err.println(e.getMessage());
        throw new RuntimeException(e);
    }

    }


public static String listFoundationModels(AwsbedrockConfiguration configuration, AwsbedrockParams awsBedrockParameters) {

        BedrockClient bedrockClient = createBedrockClient(configuration, awsBedrockParameters);

        try {
            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(r -> {});

            List<FoundationModelSummary> models = response.modelSummaries();

            System.out.println(response.toString());
            System.out.println(models.toString());
            
            JSONArray modelsArray = new JSONArray();

            if (models.isEmpty()) {
                System.out.println("No available foundation models" );
            } else {
                for (FoundationModelSummary model : models) {

                    // Create a JSONObject for each model and add to JSONArray
                    JSONObject modelJson = new JSONObject();
                    modelJson.put("modelId", model.modelId());
                    modelJson.put("modelName", model.providerName());
                    modelJson.put("provider", model.modelName());
                    modelJson.put("modelArn", model.modelArn());
                    modelJson.put("modelLifecycleStatus", model.modelLifecycle().statusAsString());
                    modelJson.put("inputModalities", model.inputModalities());
                    modelJson.put("outputModalities", model.outputModalities());
                    modelJson.put("customizationsSupported", model.customizationsSupported());
                    modelJson.put("inferenceTypesSupported", model.inferenceTypesSupported());
                    modelJson.put("responseStreamingSupported", model.responseStreamingSupported());
                                
                    modelsArray.put(modelJson);
                }
            }

            return modelsArray.toString();

        } catch (SdkClientException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }


public static String listCustomModels(AwsbedrockConfiguration configuration, AwsbedrockParams awsBedrockParameters) {

        BedrockClient bedrockClient = createBedrockClient(configuration, awsBedrockParameters);


        try {

            ListCustomModelsResponse response = bedrockClient.listCustomModels(r -> {});
            //ListFoundationModelsResponse response = bedrockClient.listFoundationModels(r -> {});

            List<CustomModelSummary> models = response.modelSummaries();

            System.out.println(response.toString());
            System.out.println(models.toString());
            
            JSONArray modelsArray = new JSONArray();

            if (models.isEmpty()) {
                System.out.println("No available foundation models.");
            } else {
                for (CustomModelSummary model : models) {
                    // Create a JSONObject for each model and add to JSONArray
                    JSONObject modelJson = new JSONObject();
                    modelJson.put("provider", model.modelName());
                    modelJson.put("modelArn", model.modelArn());
                    modelJson.put("baseModelArn", model.baseModelArn());
                    modelJson.put("baseModelName", model.baseModelName());
                    modelJson.put("customizationType", model.customizationTypeAsString());
                                
                    modelsArray.put(modelJson);
                }
            }

            return modelsArray.toString();

        } catch (SdkClientException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String getCustomModel(AwsbedrockConfiguration configuration, AwsbedrockParamsModelDetails awsBedrockParameters) {

        BedrockClient bedrockClient = createBedrockClientDetails(configuration, awsBedrockParameters);
    
    
        try {
            
            GetCustomModelResponse response = bedrockClient.getCustomModel(
                //GetCustomModelRequest.builder()
                GetCustomModelRequest.builder()
                    .modelIdentifier(awsBedrockParameters.getModelName())
                    .build()
            );
            
            //CustomModelSummary model = response.;
    
            JSONObject jsonModel = new JSONObject();
            jsonModel.put("modelArn", response.modelArn());
            jsonModel.put("modelName", response.modelName());
            jsonModel.put("jobName", response.jobName());
            jsonModel.put("jobArn", response.jobArn());
            jsonModel.put("customizationTypeAsString", response.customizationTypeAsString());
            jsonModel.put("baseModelArn", response.baseModelArn());
            jsonModel.put("hyperParameters", response.hyperParameters());
            jsonModel.put("hasHyperParameters", response.hasHyperParameters());
            jsonModel.put("hasValidationMetrics", response.hasValidationMetrics());
    
            return jsonModel.toString();
    
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (SdkException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    
        }



}