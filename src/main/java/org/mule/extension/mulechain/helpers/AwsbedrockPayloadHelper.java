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
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.GuardrailConfiguration;
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


  private static InvokeModelRequest createInvokeRequest(AwsbedrockParameters awsBedrockParameters, String nativeRequest) {
	  
	  String modelId = awsBedrockParameters.getModelName();
      System.out.println("modelId: " + modelId);

	  String region = awsBedrockParameters.getRegion();
	  
	  //for Anthropic Claude 3-x, mistral.pxtral, meta.llama3, prep the model id using the following format
	  //arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.anthropic.claude-3-5-sonnet-20241022-v2:0	  
	  //arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.mistral.pixtral-large-2502-v1:0
	  //arn:aws:bedrock:us-east-1:076261412953:inference-profile/us.meta.llama3-3-70b-instruct-v1:0
	  
	  if (modelId.contains("amazon.nova-premier") ||
			  modelId.contains("anthropic.claude-3") ||
			    modelId.contains("mistral.pixtral") ||
			    modelId.contains("meta.llama4") ||
			    modelId.contains("meta.llama3-3") ||
			    modelId.contains("meta.llama3-2") ||			    
			    modelId.contains("meta.llama3-1")) {

			    modelId = "arn:aws:bedrock:" + region + ":076261412953:inference-profile/us." + modelId;
	  }
	  
	  String guardrailIdentifier = awsBedrockParameters.getGuardrailIdentifier();
	  String guardrailVersion = awsBedrockParameters.getGuardrailVersion();
	  
	  InvokeModelRequest request; 
	  if (guardrailIdentifier != null && !guardrailIdentifier.isEmpty() &&
			    guardrailVersion != null && !guardrailVersion.isEmpty()) {
		
			    // Both values are present — specify guardrail
		   request = InvokeModelRequest.builder()
				    .modelId(modelId)
				    .body(SdkBytes.fromUtf8String(nativeRequest))
				    .contentType("application/json")
				    .guardrailIdentifier(guardrailIdentifier) 
				    .guardrailVersion(guardrailVersion) 
				    .build();

	  } else {
		  request = InvokeModelRequest.builder()
				  	.body(SdkBytes.fromUtf8String(nativeRequest))
				  	.modelId(modelId)
				  	.build();
	  }
	  
	  return request;
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
	
	// Build the user message
	JSONObject userMessage = new JSONObject();
	userMessage.put("role", "user");
	userMessage.put("content", prompt);  // no need to prepend Human/Assistant here

	// Add to messages array
	JSONArray messages = new JSONArray();
	messages.put(userMessage);

	// Construct the request body for Claude 3.x
	JSONObject jsonRequest = new JSONObject();
	jsonRequest.put("messages", messages);
	jsonRequest.put("anthropic_version", "bedrock-2023-05-31");
	jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
	jsonRequest.put("top_p", awsBedrockParameters.getTopP());
	jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

	
    return jsonRequest.toString();
}

private static String getMistralAIText(String prompt, AwsbedrockParameters awsBedrockParameters) {
	JSONObject jsonRequest = new JSONObject();
	
	if (awsBedrockParameters.getModelName().contains("mistral.pixtral")) {   //for mistral.pixtral
		// Create user message object
		JSONObject userMessage = new JSONObject();
		userMessage.put("role", "user");
		userMessage.put("content", prompt);  // No need for "Human:" and "Assistant:"

		// Wrap in messages array
		JSONArray messages = new JSONArray();
		messages.put(userMessage);

		// Construct the full request body
		jsonRequest.put("messages", messages);
	} else {
		//default for mistral.mistral
		jsonRequest.put("prompt", "\n\nHuman:" + prompt + "\n\nAssistant:");
	}
	
    jsonRequest.put("temperature", awsBedrockParameters.getTemperature());
	jsonRequest.put("max_tokens", awsBedrockParameters.getMaxTokenCount());

    return jsonRequest.toString();
}


  private static String getAI21Text(String prompt, AwsbedrockParameters awsBedrockParameters){
	// Create message object
      JSONObject message = new JSONObject();
      message.put("role", "user");
      message.put("content", prompt);

      // Wrap it into messages array
      JSONArray messages = new JSONArray();
      messages.put(message);

      // Create body object
      JSONObject body = new JSONObject();
      body.put("messages", messages);
      body.put("max_tokens", awsBedrockParameters.getMaxTokenCount());
      body.put("top_p", awsBedrockParameters.getTopP());
      body.put("temperature", awsBedrockParameters.getTemperature());

      return body.toString();
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
	JSONObject jsonRequest = new JSONObject(); jsonRequest.put("prompt", prompt);
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
    } else if (awsBedrockParameters.getModelName().contains("ai21.jamba")) {
        return getAI21Text(prompt, awsBedrockParameters);
    } else if (awsBedrockParameters.getModelName().contains("mistral")) {
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
        InvokeModelRequest request = createInvokeRequest(awsBedrockParameters, nativeRequest);

        System.out.println("Native request: " + nativeRequest);

        InvokeModelResponse response = client.invokeModel(request);
        
        return formatBedrockResponse(awsBedrockParameters, response);
        
    } catch (SdkClientException e) {
        System.err.printf("ERROR: Can't invoke '%s'. Reason: %s", awsBedrockParameters.getModelName(), e.getMessage());
        throw new RuntimeException(e);
    }
  }
  
  private static String formatBedrockResponse(AwsbedrockParameters awsBedrockParameters, InvokeModelResponse response) {
	  
	 String modelId = awsBedrockParameters.getModelName();
	  
	 String responseStr;
	 
     String modelGroup;

     // Normalize model type using contains
     if (modelId.contains("claude")) {
         modelGroup = "claude";
     } else if (modelId.contains("mistral.pixtral")) {
         modelGroup = "mistral.pixtral";
     } else if (modelId.contains("mistral.mistral")) {
         modelGroup = "mistral.mistral";
     } else if (modelId.contains("jamba")) {
         modelGroup = "jamba";
     } else if (modelId.contains("llama")) {
         modelGroup = "llama";
     } else if (modelId.contains("titan")) {
         modelGroup = "titan";
     } else {
         modelGroup = "default";
     }

     JSONObject responseBody;
     
     //convert the response for all models to match Nova response..
     //ex:
//{
  //   "output": {
    //     "message": {
      //       "role": "assistant",
        //     "content": [
          //       {
            //         "text": "Penang is renowned for its rich cultural heritage, delicious food, and historical landmarks, ..."
              //      	                  }
             //]
         //}
     //},
     //"stopReason": "end_turn",
     //"usage": {
       //  "inputTokens": 14,
       //  "outputTokens": 636,
       //  "totalTokens": 650
     //},
    // "amazon-bedrock-guardrailAction": "NONE"
//}
     
     // Switch on model group
     switch (modelGroup) {
     	case "claude":
     		return formatClaudeResponse(response);
     	case "mistral.pixtral":
     		return formatMistralPixtralResponse(response);
     	case "mistral.mistral":
     		return formatMistralMistralResponse(response);
     	case "jamba":
     		return formatJambaResponse(response);
     	case "llama":
     		return formatLlamaResponse(response);
     	case "titan":
     		//Amazon Titan
     		return formatTitanResponse(response);
     	default:
     		//Amazon Nova models & the rest
     		// Default case: pretty-print the raw response
     		responseBody = new JSONObject(response.body().asUtf8String());
     		responseStr = responseBody.toString();
     		break;
     }
     
     return responseStr;
	 	 
  }
  
  
  private static String formatTitanResponse(InvokeModelResponse response) {
      // Step 1: Convert raw response body to string
      String rawJson = response.body().asUtf8String();
      
   // Parse raw JSON
      JSONObject original = new JSONObject(rawJson);
      JSONArray results = original.getJSONArray("results");
      JSONObject result0 = results.getJSONObject(0);

      // Extract values
      String text = result0.getString("outputText").trim();
      String stopReason = result0.optString("completionReason", "").equalsIgnoreCase("FINISH") ? "end_turn" : "unknown";
      String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

      int inputTokens = original.optInt("inputTextTokenCount", 0);
      int outputTokens = result0.optInt("tokenCount", 0);
      int totalTokens = inputTokens + outputTokens;
      
   // Construct message content
      JSONObject textObj = new JSONObject();
      textObj.put("text", text);

      JSONArray contentArray = new JSONArray().put(textObj);

      JSONObject message = new JSONObject();
      message.put("role", "assistant");
      message.put("content", contentArray);

      JSONObject output = new JSONObject();
      output.put("message", message);

      JSONObject usage = new JSONObject();
      usage.put("inputTokens", inputTokens);
      usage.put("outputTokens", outputTokens);
      usage.put("totalTokens", totalTokens);

      // Final result
      JSONObject finalResult = new JSONObject();
      finalResult.put("output", output);
      finalResult.put("stopReason", stopReason);
      finalResult.put("usage", usage);
      finalResult.put("amazon-bedrock-guardrailAction", guardrail);

      // Print the result
      System.out.println(finalResult.toString(2));
      return finalResult.toString(); 
  }

  private static String formatJambaResponse(InvokeModelResponse response) {
      // Step 1: Convert raw response body to string
      String rawJson = response.body().asUtf8String();
      
   // Parse original response
      JSONObject original = new JSONObject(rawJson);
      
   // Extract content and metadata
      JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
      JSONObject message = choice.getJSONObject("message");
      String role = message.getString("role");
      String text = message.getString("content").trim();
      String stopReason = choice.optString("finish_reason", "stop");

      int inputTokens = original.getJSONObject("usage").getInt("prompt_tokens");
      int outputTokens = original.getJSONObject("usage").getInt("completion_tokens");
      int totalTokens = original.getJSONObject("usage").getInt("total_tokens");
      String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

   // Create new JSON format
      JSONObject textObj = new JSONObject();
      textObj.put("text", text);

      JSONArray contentArray = new JSONArray().put(textObj);

      JSONObject newMessage = new JSONObject();
      newMessage.put("role", role);
      newMessage.put("content", contentArray);

      JSONObject output = new JSONObject();
      output.put("message", newMessage);

      JSONObject usage = new JSONObject();
      usage.put("inputTokens", inputTokens);
      usage.put("outputTokens", outputTokens);
      usage.put("totalTokens", totalTokens);

      JSONObject finalResult = new JSONObject();
      finalResult.put("output", output);
      finalResult.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
      finalResult.put("usage", usage);
      finalResult.put("amazon-bedrock-guardrailAction", guardrail);   
   
      // Output the normalized JSON
      System.out.println(finalResult.toString(2));

      return finalResult.toString(); 
  }
  
  private static String formatClaudeResponse(InvokeModelResponse response) {
	  
	// Step 1: Read the raw JSON string from response
      String rawJson = response.body().asUtf8String(); // your actual response object here

      // Step 2: Parse the original JSON
      JSONObject original = new JSONObject(rawJson);

      // Step 3: Extract the content text
      JSONArray contentArray = original.getJSONArray("content");
      JSONObject firstContentObj = contentArray.getJSONObject(0);
      String originalText = firstContentObj.getString("text");
      
      //get token usage
      JSONObject usageOriginal = original.getJSONObject("usage");

      // Map existing keys to normalized token usage
      int inputTokens = usageOriginal.getInt("input_tokens");
      int outputTokens = usageOriginal.getInt("output_tokens");
      int totalTokens = inputTokens + outputTokens;
      
   // Build normalized usage block
      JSONObject usage = new JSONObject();
      usage.put("inputTokens", inputTokens);
      usage.put("outputTokens", outputTokens);
      usage.put("totalTokens", totalTokens);
      
   // Step 4: Build the new content array
      JSONArray newContentArray = new JSONArray();
      JSONObject textObject = new JSONObject();
      textObject.put("text", originalText);
      newContentArray.put(textObject);

      // Step 5: Build the message
      JSONObject message = new JSONObject();
      message.put("role", original.getString("role"));
      message.put("content", newContentArray);

      // Step 6: Wrap it in the new output format
      JSONObject output = new JSONObject();
      output.put("message", message);

      JSONObject finalPayload = new JSONObject();
      finalPayload.put("output", output);
      finalPayload.put("stopReason", original.optString("stop_reason", "end_turn"));
      finalPayload.put("usage", usage);
      finalPayload.put("amazon-bedrock-guardrailAction", original.optString("amazon-bedrock-guardrailAction", "NONE"));

      // Step 7: Print the result
      System.out.println(finalPayload.toString(2));
      	  
      return finalPayload.toString(); 
  }
  
  private static String formatLlamaResponse(InvokeModelResponse response) {
	  
	// Step 1: Read the raw JSON string from response
      String rawJson = response.body().asUtf8String(); // your actual response object here
	  
	// Step 2: Parse original response
      JSONObject original = new JSONObject(rawJson);


      // Step 3: Extract the content text
      String generationText = original.getString("generation");
      int inputTokens = original.getInt("prompt_token_count");
      int outputTokens = original.getInt("generation_token_count");
      String stopReason = original.optString("stop_reason", "stop");

      // Wrap generation text in content array
      JSONObject textObj = new JSONObject();
      textObj.put("text", generationText.trim());

      JSONArray contentArray = new JSONArray().put(textObj);

      // Build message object
      JSONObject message = new JSONObject();
      message.put("role", "assistant");
      message.put("content", contentArray);

      // Wrap message into output
      JSONObject output = new JSONObject();
      output.put("message", message);

      // Build usage block
      JSONObject usage = new JSONObject();
      usage.put("inputTokens", inputTokens);
      usage.put("outputTokens", outputTokens);
      usage.put("totalTokens", inputTokens + outputTokens);

      // Final output structure
      JSONObject finalPayload = new JSONObject();
      finalPayload.put("output", output);
      finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);  //covert stop to end_turn
      finalPayload.put("usage", usage);
      finalPayload.put("amazon-bedrock-guardrailAction", original.optString("amazon-bedrock-guardrailAction", "NONE"));

      // Print final JSON
      System.out.println(finalPayload.toString(2));
	  
      return finalPayload.toString(); 
  }
  

  private static String formatMistralMistralResponse(InvokeModelResponse response) {
	  
	  
		      // Step 1: Convert the raw JSON string from the response body
		      String rawJson = response.body().asUtf8String();
		      
		   // Parse raw JSON
		        JSONObject original = new JSONObject(rawJson);
		        JSONArray outputs = original.getJSONArray("outputs");
		        JSONObject output0 = outputs.getJSONObject(0);

		        String text = output0.getString("text").trim();
		        String stopReason = output0.optString("stop_reason", "stop");
		        String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

		        // Create assistant message block
		        JSONObject textObj = new JSONObject();
		        textObj.put("text", text);
		        JSONArray contentArray = new JSONArray().put(textObj);

		        JSONObject message = new JSONObject();
		        message.put("role", "assistant");
		        message.put("content", contentArray);

		        JSONObject output = new JSONObject();
		        output.put("message", message);

		     // Create usage block with nulls (or you can skip this block)
		        JSONObject usage = new JSONObject();
		        usage.put("inputTokens", JSONObject.NULL);
		        usage.put("outputTokens", JSONObject.NULL);
		        usage.put("totalTokens", JSONObject.NULL);

		        // Assemble final response
		        JSONObject finalPayload = new JSONObject();
		        finalPayload.put("output", output);
		        finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
		        finalPayload.put("usage", usage);
		        finalPayload.put("amazon-bedrock-guardrailAction", guardrail);

		        // Print the final JSON
		        System.out.println(finalPayload.toString(2));
		
		      return finalPayload.toString(); 		  
  }


  private static String formatMistralPixtralResponse(InvokeModelResponse response) {
	  
	  
      // Step 1: Convert the raw JSON string from the response body
      String rawJson = response.body().asUtf8String();
      
   // Parse original JSON
      JSONObject original = new JSONObject(rawJson);
      JSONObject choice = original.getJSONArray("choices").getJSONObject(0);
      JSONObject messageObj = choice.getJSONObject("message");

      String content = messageObj.getString("content").trim();
      String role = messageObj.optString("role", "assistant");
      String stopReason = choice.optString("finish_reason", "stop");
      String guardrail = original.optString("amazon-bedrock-guardrailAction", "NONE");

      // Extract token usage if available
      JSONObject usageObj = original.optJSONObject("usage");
      Object inputTokens = (usageObj != null && usageObj.has("prompt_tokens")) ? usageObj.get("prompt_tokens") : JSONObject.NULL;
      Object outputTokens = (usageObj != null && usageObj.has("completion_tokens")) ? usageObj.get("completion_tokens") : JSONObject.NULL;
      Object totalTokens = (usageObj != null && usageObj.has("total_tokens")) ? usageObj.get("total_tokens") : JSONObject.NULL;

      // Build content array
      JSONObject textObj = new JSONObject();
      textObj.put("text", content);
      JSONArray contentArray = new JSONArray().put(textObj);
      
   // Build message
      JSONObject message = new JSONObject();
      message.put("role", role);
      message.put("content", contentArray);

      // Wrap into output object
      JSONObject output = new JSONObject();
      output.put("message", message);

      // Usage section
      JSONObject usage = new JSONObject();
      usage.put("inputTokens", inputTokens);
      usage.put("outputTokens", outputTokens);
      usage.put("totalTokens", totalTokens);
      
      // Assemble final response
      JSONObject finalPayload = new JSONObject();
      finalPayload.put("output", output);
      finalPayload.put("stopReason", stopReason.equals("stop") ? "end_turn" : stopReason);
      finalPayload.put("usage", usage);
      finalPayload.put("amazon-bedrock-guardrailAction", guardrail);

      // Print the final JSON
      System.out.println(finalPayload.toString(2));

      return finalPayload.toString(); 

  
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