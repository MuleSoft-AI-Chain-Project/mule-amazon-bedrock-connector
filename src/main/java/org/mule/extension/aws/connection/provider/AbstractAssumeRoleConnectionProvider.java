package org.mule.extension.aws.connection.provider;

import java.util.concurrent.atomic.AtomicReference;
import org.mule.connectors.commons.template.connection.ConnectorConnection;
import org.mule.extension.aws.connection.provider.credentials.AssumeRoleCredentialsProvider;
import org.mule.extension.aws.connection.provider.parameters.CommonParameters;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Abstract base class for Assume Role connection providers. Extracts common assume role logic to reduce code duplication.
 *
 * @param <CONNECTION> The type of connection this provider creates
 */
public abstract class AbstractAssumeRoleConnectionProvider<CONNECTION extends ConnectorConnection>
    extends AbstractBedrockConnectionProvider<CONNECTION> {

  @Parameter
  @DisplayName("Role ARN")
  @Placement(order = 1)
  @Summary("The Role ARN unique identifies role to assume in order to gain cross account access.")
  protected String roleARN;

  @Parameter
  @DisplayName("Custom STS Endpoint")
  @Optional
  @Placement(tab = "Advanced", order = 1)
  @Summary("Sets a custom STS endpoint. Useful when a non-standard service endpoint is required, such as a VPC endpoint.")
  protected String customStsEndpoint;

  @Parameter
  @DisplayName("External ID")
  @Optional
  @Expression(ExpressionSupport.SUPPORTED)
  @Summary("A unique identifier that might be required when you assume a role in another account. If the administrator of the " +
      "account to which the role belongs provides an external ID, then provide that value in this field.")
  protected String externalId;

  private final AtomicReference<StsClient> stsClient = new AtomicReference<>();

  public AbstractAssumeRoleConnectionProvider() {}

  @Override
  protected AwsCredentialsProvider getAWSCredentialsProvider(CommonParameters commonParameters) throws ConnectionException {
    if (commonParameters.isTryDefaultAWSCredentialsProviderChain()) {
      return DefaultCredentialsProvider.builder().build();
    }
    return new AssumeRoleCredentialsProvider(this.roleARN, this.getStsClient(), externalId);
  }

  protected synchronized StsClient getStsClient() throws ConnectionException {
    if (this.stsClient.get() == null) {
      this.stsClient.set(StsClientHelper.buildStsClient(
                                                        getCommonParameters(),
                                                        sdkHttpClientFactory,
                                                        customStsEndpoint));
    }
    return stsClient.get();
  }
}
