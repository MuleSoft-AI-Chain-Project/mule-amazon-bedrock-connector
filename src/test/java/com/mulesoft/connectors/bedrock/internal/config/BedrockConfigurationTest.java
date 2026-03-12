package com.mulesoft.connectors.bedrock.internal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.lang.reflect.Field;

@DisplayName("BedrockConfiguration")
class BedrockConfigurationTest {

  private BedrockConfiguration config;
  private SchedulerService mockSchedulerService;
  private Scheduler mockScheduler;

  @BeforeEach
  void setUp() throws Exception {
    config = new BedrockConfiguration();
    mockSchedulerService = mock(SchedulerService.class);
    mockScheduler = mock(Scheduler.class);

    when(mockSchedulerService.ioScheduler(any(SchedulerConfig.class))).thenReturn(mockScheduler);

    // Inject the mock SchedulerService via reflection (normally @Inject handles this)
    Field schedulerServiceField = BedrockConfiguration.class.getDeclaredField("schedulerService");
    schedulerServiceField.setAccessible(true);
    schedulerServiceField.set(config, mockSchedulerService);
  }

  @Nested
  @DisplayName("getStreamingScheduler")
  class GetStreamingScheduler {

    @Test
    @DisplayName("lazily creates IO scheduler on first access")
    void createsSchedulerOnFirstAccess() {
      Scheduler result = config.getStreamingScheduler();

      assertThat(result).isSameAs(mockScheduler);
      verify(mockSchedulerService).ioScheduler(any(SchedulerConfig.class));
    }

    @Test
    @DisplayName("returns same instance on subsequent calls (singleton)")
    void returnsSameInstanceOnSubsequentCalls() {
      Scheduler first = config.getStreamingScheduler();
      Scheduler second = config.getStreamingScheduler();

      assertThat(first).isSameAs(second);
      // ioScheduler should only be called once despite two getStreamingScheduler calls
      verify(mockSchedulerService).ioScheduler(any(SchedulerConfig.class));
    }
  }

  @Nested
  @DisplayName("dispose")
  class Dispose {

    @Test
    @DisplayName("stops scheduler when it was created")
    void stopsSchedulerWhenCreated() {
      config.getStreamingScheduler(); // trigger creation
      config.dispose();

      verify(mockScheduler).stop();
    }

    @Test
    @DisplayName("does nothing when scheduler was never created")
    void doesNothingWhenNeverCreated() {
      config.dispose(); // should not throw

      verify(mockScheduler, never()).stop();
    }

    @Test
    @DisplayName("nulls out scheduler so next access creates a new one")
    void nullsOutSchedulerAfterDispose() {
      config.getStreamingScheduler();
      config.dispose();

      // A second call to getStreamingScheduler should create a new scheduler
      Scheduler newScheduler = mock(Scheduler.class);
      when(mockSchedulerService.ioScheduler(any(SchedulerConfig.class))).thenReturn(newScheduler);

      Scheduler result = config.getStreamingScheduler();
      assertThat(result).isSameAs(newScheduler);
    }
  }

  @Nested
  @DisplayName("getSchedulerService")
  class GetSchedulerService {

    @Test
    @DisplayName("returns injected SchedulerService")
    void returnsInjectedSchedulerService() {
      assertThat(config.getSchedulerService()).isSameAs(mockSchedulerService);
    }
  }

  @Nested
  @DisplayName("getSchedulerConfig")
  class GetSchedulerConfigTest {

    @Test
    @DisplayName("returns injected SchedulerConfig")
    void returnsInjectedSchedulerConfig() throws Exception {
      SchedulerConfig mockConfig = mock(SchedulerConfig.class);
      Field schedulerConfigField = BedrockConfiguration.class.getDeclaredField("schedulerConfig");
      schedulerConfigField.setAccessible(true);
      schedulerConfigField.set(config, mockConfig);

      assertThat(config.getSchedulerConfig()).isSameAs(mockConfig);
    }
  }
}
