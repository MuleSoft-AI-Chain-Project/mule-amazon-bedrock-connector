package com.mulesoft.connectors.bedrock.internal.config;

import org.mule.connectors.commons.template.config.ConnectorConfig;
import com.mulesoft.connectors.bedrock.internal.connection.provider.AssumeRoleConnectionProvider;
import com.mulesoft.connectors.bedrock.internal.connection.provider.BasicConnectionProvider;
import com.mulesoft.connectors.bedrock.internal.operation.AgentOperations;
import com.mulesoft.connectors.bedrock.internal.operation.ChatOperations;
import com.mulesoft.connectors.bedrock.internal.operation.EmbeddingOperation;
import com.mulesoft.connectors.bedrock.internal.operation.FoundationalModelOperations;
import com.mulesoft.connectors.bedrock.internal.operation.ImageOperation;
import com.mulesoft.connectors.bedrock.internal.operation.SentimentOperations;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

@Configuration(name = "config")
@DisplayName("Configuration")
@Operations({
    ChatOperations.class,
    AgentOperations.class,
    SentimentOperations.class,
    FoundationalModelOperations.class,
    ImageOperation.class,
    EmbeddingOperation.class
})

@ConnectionProviders({
    BasicConnectionProvider.class,
    AssumeRoleConnectionProvider.class
})

public class BedrockConfiguration implements ConnectorConfig {
}
