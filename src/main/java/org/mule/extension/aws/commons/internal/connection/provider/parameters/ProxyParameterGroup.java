package org.mule.extension.aws.commons.internal.connection.provider.parameters;

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

  public void setProxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public void setProxyPort(Integer proxyPort) {
    this.proxyPort = proxyPort;
  }

  public String getProxyUsername() {
    return proxyUsername;
  }

  public void setProxyUsername(String proxyUsername) {
    this.proxyUsername = proxyUsername;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public void setProxyPassword(String proxyPassword) {
    this.proxyPassword = proxyPassword;
  }

  public String getProxyDomain() {
    return proxyDomain;
  }

  public void setProxyDomain(String proxyDomain) {
    this.proxyDomain = proxyDomain;
  }

  public String getProxyWorkstation() {
    return proxyWorkstation;
  }

  public void setProxyWorkstation(String proxyWorkstation) {
    this.proxyWorkstation = proxyWorkstation;
  }
}
