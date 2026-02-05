package com.mulesoft.connectors.bedrock.api.parameter;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresProxy;
import org.mule.sdk.api.annotation.semantics.connectivity.Domain;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.security.Password;
import org.mule.sdk.api.annotation.semantics.security.Username;

import java.util.Objects;

@ConfiguresProxy
public class ProxyParameterGroup {

  /**
   * The optional proxy host.
   */
  @Parameter
  @Optional
  @DisplayName("Host")
  @Placement(tab = "Proxy", order = 1)
  @Summary("The optional proxy host.")
  @Host
  private String proxyHost;

  /**
   * The optional proxy port.
   */
  @Parameter
  @Optional
  @Placement(tab = "Proxy", order = 2)
  @DisplayName("Port")
  @Summary("The optional proxy port.")
  @Port
  private Integer proxyPort;

  /**
   * The optional proxy username.
   */
  @Parameter
  @Optional
  @Placement(tab = "Proxy", order = 3)
  @DisplayName("Username")
  @Summary("The optional proxy username.")
  @Username
  private String proxyUsername;

  /**
   * The optional proxy password.
   */
  @Parameter
  @Optional
  @Placement(tab = "Proxy", order = 4)
  @DisplayName("Password")
  @Summary("The optional proxy password.")
  @Password
  private String proxyPassword;

  /**
   * The optional proxy domain.
   */
  @Parameter
  @Optional
  @Placement(tab = "Proxy", order = 5)
  @DisplayName("Domain")
  @Summary("The optional proxy domain.")
  @Domain
  private String proxyDomain;

  /**
   * The optional proxy workstation.
   */
  @Parameter
  @Optional
  @Placement(tab = "Proxy", order = 6)
  @DisplayName("Workstation")
  @Summary("The optional proxy workstation.")
  private String proxyWorkstation;

  public String getProxyHost() {
    return proxyHost;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public String getProxyUsername() {
    return proxyUsername;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public String getProxyDomain() {
    return proxyDomain;
  }

  public String getProxyWorkstation() {
    return proxyWorkstation;
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ProxyParameterGroup that))
      return false;
    return Objects.equals(proxyHost, that.proxyHost) && Objects.equals(proxyPort, that.proxyPort)
        && Objects.equals(proxyUsername, that.proxyUsername) && Objects.equals(proxyPassword, that.proxyPassword)
        && Objects.equals(proxyDomain, that.proxyDomain) && Objects.equals(proxyWorkstation, that.proxyWorkstation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proxyHost, proxyPort, proxyUsername, proxyPassword, proxyDomain, proxyWorkstation);
  }
}
