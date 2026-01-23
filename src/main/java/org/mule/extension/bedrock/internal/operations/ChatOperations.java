package org.mule.extension.bedrock.internal.operations;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;

import java.io.InputStream;
import org.mule.extension.bedrock.api.params.BedrockParameters;
import org.mule.extension.bedrock.internal.config.BedrockConfiguration;
import org.mule.extension.bedrock.internal.connection.BedrockConnection;
import org.mule.extension.bedrock.internal.metadata.provider.BedrockErrorsProvider;
import org.mule.extension.bedrock.internal.service.ChatService;
import org.mule.extension.bedrock.internal.service.ChatServiceImpl;
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

public class ChatOperations extends BedrockOperation<ChatService> {

  public ChatOperations() {
    super(ChatServiceImpl::new);
  }

  /**
   * Generates a text response from a prompt using the specified Bedrock foundation model.
   *
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param prompt The text prompt to send to the model.
   * @param bedrockParameters Additional properties including model name, region, temperature, topP, topK, maxTokenCount, and
   *        optional guardrail identifier.
   * @return InputStream containing the JSON response with generated text, usage metrics, and stop reason.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("CHAT-answer-prompt")
  @Summary("Generate a text response from a foundation model")
  public InputStream chatAnswerPrompt(@Config BedrockConfiguration config,
                                      @Connection BedrockConnection connection,
                                      String prompt,
                                      @ParameterGroup(name = "Additional properties") BedrockParameters bedrockParameters) {

    return newExecutionBuilder(config, connection)
        .execute(ChatService::answerPrompt, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(bedrockParameters);

  }

  /**
   * Generates a text response from a prompt with conversation memory support. Maintains conversation history in a persistent
   * storage.
   *
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param prompt The text prompt to send to the model.
   * @param memoryPath File system path where the memory database will be stored.
   * @param memoryName Name identifier for the memory storage (used as database name).
   * @param keepLastMessages Number of previous messages to include in the conversation context.
   * @param bedrockParameters Additional properties including model name, region, temperature, topP, topK, maxTokenCount, and
   *        optional guardrail identifier.
   * @return InputStream containing the JSON response with generated text, usage metrics, and stop reason.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("CHAT-answer-prompt-memory")
  @Summary("Generate a response with conversational memory")
  public InputStream chatAnswerPromptMemory(@Config BedrockConfiguration config,
                                            @Connection BedrockConnection connection,
                                            String prompt, String memoryPath, String memoryName, Integer keepLastMessages,
                                            @ParameterGroup(name = "Additional properties") BedrockParameters bedrockParameters) {

    return newExecutionBuilder(config, connection)
        .execute(ChatService::answerPromptMemory, BedrockModelFactory::createInputStream)
        .withParam(prompt)
        .withParam(memoryPath)
        .withParam(memoryName)
        .withParam(keepLastMessages)
        .withParam(bedrockParameters);
  }

  /**
   * Generates a streaming text response from a prompt using Server-Sent Events (SSE). Returns responses in real-time as they are
   * generated.
   *
   * @param config Configuration for Bedrock connector.
   * @param connection Bedrock connection instance.
   * @param prompt The text prompt to send to the model.
   * @param bedrockParameters Additional properties including model name, region, temperature, topP, topK, maxTokenCount, and
   *        optional guardrail identifier.
   * @return InputStream containing Server-Sent Events (SSE) stream with real-time text generation chunks.
   */
  @MediaType(value = APPLICATION_JSON, strict = false)
  @Throws(BedrockErrorsProvider.class)
  @Execution(ExecutionType.BLOCKING)
  @Alias("CHAT-answer-prompt-streaming")
  @Summary("Streaming a text response from a foundation model")
  public InputStream chatAnswerPromptStreaming(@Config BedrockConfiguration config,
                                               @Connection BedrockConnection connection,
                                               String prompt,
                                               @ParameterGroup(
                                                   name = "Additional properties") BedrockParameters bedrockParameters) {

    return newExecutionBuilder(config, connection)
        .execute(ChatService::answerPromptStreaming, BedrockModelFactory::identity)
        .withParam(prompt)
        .withParam(bedrockParameters);

  }

}
