package com.mulesoft.connectors.bedrock.internal.connection.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import com.mulesoft.connectors.bedrock.api.parameter.ProxyParameterGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProxyParameterGroup")
class ProxyParameterGroupTest {

  @Test
  @DisplayName("getters and setters")
  void gettersAndSetters() {
    ProxyParameterGroup group = new ProxyParameterGroup();
    group.setProxyHost("host");
    group.setProxyPort(8080);
    group.setProxyUsername("user");
    group.setProxyPassword("pass");
    group.setProxyDomain("domain");
    group.setProxyWorkstation("ws");

    assertThat(group.getProxyHost()).isEqualTo("host");
    assertThat(group.getProxyPort()).isEqualTo(8080);
    assertThat(group.getProxyUsername()).isEqualTo("user");
    assertThat(group.getProxyPassword()).isEqualTo("pass");
    assertThat(group.getProxyDomain()).isEqualTo("domain");
    assertThat(group.getProxyWorkstation()).isEqualTo("ws");
  }
}
