package org.mule.extension.mulechain.internal.proxy;

import java.util.Set;

public interface ProxyConfig {

  public String getScheme();

  public String getHost();

  public int getPort();

  public String getUsername();

  public String getPassword();

  public Set<String> getNonProxyHosts();

  public String getTrustStorePath();

  public String getTrustStorePassword();

  public TrustStoreType getTrustStoreType();

}
