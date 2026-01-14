package org.mule.extension.bedrock.internal.config;

import org.mule.connectors.commons.template.config.ConnectorConfig;
import org.mule.extension.bedrock.internal.connection.provider.AssumeRoleConnectionProvider;
import org.mule.extension.bedrock.internal.connection.provider.BasicConnectionProvider;
import org.mule.extension.bedrock.internal.operations.AgentOperations;
import org.mule.extension.bedrock.internal.operations.ChatOperations;
import org.mule.extension.bedrock.internal.operations.CustomModelOperations;
import org.mule.extension.bedrock.internal.operations.EmbeddingOperation;
import org.mule.extension.bedrock.internal.operations.FoundationalModelOperations;
import org.mule.extension.bedrock.internal.operations.ImageOperation;
import org.mule.extension.bedrock.internal.operations.SentimentOperations;
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
    CustomModelOperations.class,
    ImageOperation.class,
    EmbeddingOperation.class
})

@ConnectionProviders({
    BasicConnectionProvider.class,
    AssumeRoleConnectionProvider.class
})

public class BedrockConfiguration implements ConnectorConfig {
}
