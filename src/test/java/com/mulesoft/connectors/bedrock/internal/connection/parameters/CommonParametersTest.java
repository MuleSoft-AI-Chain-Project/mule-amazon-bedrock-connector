package com.mulesoft.connectors.bedrock.internal.connection.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommonParameters")
class CommonParametersTest {

  private static void setField(Object target, String name, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  @Test
  @DisplayName("default constructor and getters")
  void defaults() {
    CommonParameters params = new CommonParameters();
    assertThat(params).isNotNull();
  }

  @Test
  @DisplayName("getConnectionTimeout returns millis from connectionTimeoutUnit")
  void getConnectionTimeout() throws Exception {
    CommonParameters params = new CommonParameters();
    params.setConnectionTimeout(5);
    setField(params, "connectionTimeoutUnit", TimeUnit.SECONDS);
    assertThat(params.getConnectionTimeout()).isEqualTo(5000);
  }

  @Test
  @DisplayName("getSocketTimeout returns millis from socketTimeoutUnit")
  void getSocketTimeout() throws Exception {
    CommonParameters params = new CommonParameters();
    params.setSocketTimeout(2);
    setField(params, "socketTimeoutUnit", TimeUnit.SECONDS);
    assertThat(params.getSocketTimeout()).isEqualTo(2000);
  }

  @Test
  @DisplayName("getRegion returns normalized region")
  void getRegion() {
    CommonParameters params = new CommonParameters();
    params.setRegion("US_EAST_1");
    assertThat(params.getRegion()).isEqualTo("us-east-1");
  }

  @Test
  @DisplayName("getRegion throws when null")
  void getRegionNullThrows() {
    CommonParameters params = new CommonParameters();
    assertThatThrownBy(params::getRegion).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("getRegion throws when empty")
  void getRegionEmptyThrows() {
    CommonParameters params = new CommonParameters();
    params.setRegion("");
    assertThatThrownBy(params::getRegion).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("access key and secret key getters and setters")
  void accessKeySecretKey() {
    CommonParameters params = new CommonParameters();
    params.setAccessKey("ak");
    params.setSecretKey("sk");
    assertThat(params.getAccessKey()).isEqualTo("ak");
    assertThat(params.getSecretKey()).isEqualTo("sk");
  }

  @Test
  @DisplayName("tryDefaultAWSCredentialsProviderChain getter and setter")
  void tryDefaultCredentials() {
    CommonParameters params = new CommonParameters();
    params.setTryDefaultAWSCredentialsProviderChain(true);
    assertThat(params.isTryDefaultAWSCredentialsProviderChain()).isTrue();
  }

  @Test
  @DisplayName("maxConnections getter and setter")
  void maxConnections() {
    CommonParameters params = new CommonParameters();
    params.setMaxConnections(100);
    assertThat(params.getMaxConnections()).isEqualTo(100);
  }

  @Test
  @DisplayName("customServiceEndpoint getter")
  void customServiceEndpoint() throws Exception {
    CommonParameters params = new CommonParameters();
    setField(params, "customServiceEndpoint", "https://custom.endpoint");
    assertThat(params.getCustomServiceEndpoint()).isEqualTo("https://custom.endpoint");
  }

  @Test
  @DisplayName("tlsContext getter and setter")
  void tlsContext() {
    CommonParameters params = new CommonParameters();
    assertThat(params.getTlsContext()).isNull();
    params.setTlsContext(null);
    assertThat(params.getTlsContext()).isNull();
  }

  @Test
  @DisplayName("connectionTimeoutUnit getter and setter")
  void connectionTimeoutUnit() {
    CommonParameters params = new CommonParameters();
    assertThat(params.getConnectionTimeoutUnit()).isNull();
    params.setConnectionTimeoutUnit(TimeUnit.MINUTES);
    assertThat(params.getConnectionTimeoutUnit()).isEqualTo(TimeUnit.MINUTES);
    params.setConnectionTimeout(2);
    assertThat(params.getConnectionTimeout()).isEqualTo(120_000);
  }

  @Test
  @DisplayName("socketTimeoutUnit getter and setter")
  void socketTimeoutUnit() {
    CommonParameters params = new CommonParameters();
    assertThat(params.getSocketTimeoutUnit()).isNull();
    params.setSocketTimeoutUnit(TimeUnit.MINUTES);
    assertThat(params.getSocketTimeoutUnit()).isEqualTo(TimeUnit.MINUTES);
    params.setSocketTimeout(1);
    assertThat(params.getSocketTimeout()).isEqualTo(60_000);
  }
}
