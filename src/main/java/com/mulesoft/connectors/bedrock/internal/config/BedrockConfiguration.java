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
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

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

public class BedrockConfiguration implements ConnectorConfig, Disposable {

  private static final Logger logger = LoggerFactory.getLogger(BedrockConfiguration.class);

  private static final int STREAMING_MAX_POOL_SIZE = 200;

  @Inject
  SchedulerService schedulerService;
  @Inject
  SchedulerConfig schedulerConfig;

  private volatile Scheduler streamingScheduler;

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  public SchedulerConfig getSchedulerConfig() {
    return schedulerConfig;
  }

  /**
   * Returns a shared, bounded IO scheduler for streaming operations. Lazily initialized on first access; thread-safe via
   * double-checked locking.
   */
  public Scheduler getStreamingScheduler() {
    Scheduler local = streamingScheduler;
    if (local == null) {
      synchronized (this) {
        local = streamingScheduler;
        if (local == null) {
          local = schedulerService.ioScheduler(
                                               SchedulerConfig.config()
                                                   .withMaxConcurrentTasks(STREAMING_MAX_POOL_SIZE)
                                                   .withName("bedrock-streaming"));
          streamingScheduler = local;
          logger.debug("Created bedrock-streaming IO scheduler with maxConcurrentTasks={}", STREAMING_MAX_POOL_SIZE);
        }
      }
    }
    return local;
  }

  @Override
  public void dispose() {
    Scheduler local = streamingScheduler;
    if (local != null) {
      logger.debug("Stopping bedrock-streaming scheduler");
      local.stop();
      streamingScheduler = null;
    }
  }
}
