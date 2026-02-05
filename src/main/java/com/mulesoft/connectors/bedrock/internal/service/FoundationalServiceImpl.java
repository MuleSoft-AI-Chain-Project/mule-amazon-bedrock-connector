package com.mulesoft.connectors.bedrock.internal.service;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.mulesoft.connectors.bedrock.internal.parameter.BedrockParamsModelDetails;
import com.mulesoft.connectors.bedrock.internal.config.BedrockConfiguration;
import com.mulesoft.connectors.bedrock.internal.connection.BedrockConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.bedrock.model.FoundationModelDetails;
import software.amazon.awssdk.services.bedrock.model.FoundationModelSummary;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelRequest;
import software.amazon.awssdk.services.bedrock.model.GetFoundationModelResponse;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.bedrock.model.ValidationException;

public class FoundationalServiceImpl extends BedrockServiceImpl implements FoundationalService {

  private static final Logger logger = LoggerFactory.getLogger(FoundationalServiceImpl.class);

  public FoundationalServiceImpl(BedrockConfiguration config, BedrockConnection bedrockConnection) {
    super(config, bedrockConnection);
  }

  @Override
  public String getFoundationModel(BedrockParamsModelDetails bedrockParamsModelDetails) {
    try {
      GetFoundationModelResponse response = getConnection().getFoundationModel(
                                                                               GetFoundationModelRequest.builder()
                                                                                   .modelIdentifier(bedrockParamsModelDetails
                                                                                       .getModelName())
                                                                                   .build());
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
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public String listFoundationModels() {
    try {
      ListFoundationModelsResponse response = getConnection().listFoundationalModels();
      List<FoundationModelSummary> models = response.modelSummaries();

      logger.info(response.toString());
      logger.info(models.toString());

      JSONArray modelsArray = new JSONArray();

      if (models.isEmpty()) {
        logger.info("No available foundation models");
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
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }

  }
}
