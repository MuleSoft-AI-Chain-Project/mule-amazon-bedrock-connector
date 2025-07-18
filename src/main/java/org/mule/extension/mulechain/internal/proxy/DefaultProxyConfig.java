package org.mule.extension.mulechain.internal.proxy;

import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.*;

import java.util.Set;

/**
 * Basic HTTP Proxy configuration.
 */
@Alias("proxy")
@TypeDsl(allowTopLevelDefinition = true)
public class DefaultProxyConfig implements ProxyConfig {

    @Parameter
    @Optional(defaultValue = "http")
    private String scheme;

    @Parameter
    private String host;

    @Parameter
    private int port = Integer.MAX_VALUE;

    @Parameter
    @Optional
    private String username;

    @Parameter
    @Optional
    @Password
    private String password;

    @Parameter
    @Optional
    private Set<String> nonProxyHosts;

    @Parameter
    @Example("trustStore.jks")
    @Path(type = PathModel.Type.FILE)
    @Optional
    private String trustStore;

    @Parameter
    @Password
    @Optional
    private String trustStorePassword;

    @Parameter
    @Optional
    private TrustStoreType trustStoreType;

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<String> getNonProxyHosts() {
        return nonProxyHosts;
    }

    public String getTrustStorePath() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public TrustStoreType getTrustStoreType() {
        return trustStoreType;
    }

}
