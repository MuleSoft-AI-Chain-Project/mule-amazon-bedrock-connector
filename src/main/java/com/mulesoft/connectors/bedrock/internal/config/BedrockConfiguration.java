package com.mulesoft.connectors.bedrock.internal.config;

import com.mulesoft.connectors.bedrock.internal.connection.provider.AssumeRoleConnectionProvider;
import com.mulesoft.connectors.bedrock.internal.connection.provider.BasicConnectionProvider;
import org.mule.connectors.commons.template.config.ConnectorConfig;
import com.mulesoft.connectors.bedrock.internal.operations.AgentOperations;
import com.mulesoft.connectors.bedrock.internal.operations.ChatOperations;
import com.mulesoft.connectors.bedrock.internal.operations.EmbeddingOperation;
import com.mulesoft.connectors.bedrock.internal.operations.FoundationalModelOperations;
import com.mulesoft.connectors.bedrock.internal.operations.ImageOperation;
import com.mulesoft.connectors.bedrock.internal.operations.SentimentOperations;
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
