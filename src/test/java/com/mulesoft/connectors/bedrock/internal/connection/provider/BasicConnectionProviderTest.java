package com.mulesoft.connectors.bedrock.internal.connection.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mulesoft.connectors.bedrock.internal.connection.parameters.CommonParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mule.runtime.api.connection.ConnectionException;

@DisplayName("BasicConnectionProvider")
class BasicConnectionProviderTest {

  @Test
  @DisplayName("can be instantiated")
  void constructor() {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    assertThat(provider).isNotNull();
  }

  @Test
  @DisplayName("getCommonParameters returns new CommonParameters when null")
  void getCommonParametersWhenNull() {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    assertThat(provider.getCommonParameters()).isNotNull();
  }

  @Test
  @DisplayName("setCommonParameters and getCommonParameters")
  void setAndGetCommonParameters() {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    CommonParameters params = new CommonParameters();
    params.setRegion("us-east-1");
    provider.setCommonParameters(params);
    assertThat(provider.getCommonParameters()).isSameAs(params);
    assertThat(provider.getCommonParameters().getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @DisplayName("start and stop do not throw when httpClient is null")
  void startAndStop() throws Exception {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    provider.setCommonParameters(new CommonParameters());
    provider.start();
    provider.stop();
  }

  @Test
  @DisplayName("initialise does not throw when tls context is null")
  void initialiseWithNullTls() throws Exception {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    provider.setCommonParameters(new CommonParameters());
    provider.initialise();
  }

  @Test
  @DisplayName("connect throws when credentials are blank and not using default chain")
  void connectThrowsWhenCredentialsBlank() {
    BasicConnectionProvider provider = new BasicConnectionProvider();
    CommonParameters params = new CommonParameters();
    params.setRegion("us-east-1");
    params.setAccessKey("");
    params.setSecretKey("");
    params.setTryDefaultAWSCredentialsProviderChain(false);
    provider.setCommonParameters(params);

    assertThatThrownBy(provider::connect)
        .isInstanceOf(ConnectionException.class)
        .hasMessageContaining("Access Key or Secret Key is blank");
  }
}
