package org.mule.extension.aws.commons.internal.connection.provider;

import static org.mule.extension.bedrock.internal.util.StringUtils.isPresent;

import org.mule.connectors.commons.template.connection.ConnectorConnection;
import org.mule.extension.aws.commons.internal.connection.provider.parameters.CommonParameters;
import org.mule.extension.bedrock.internal.error.exception.AWSConnectionException;
import org.mule.runtime.api.connection.ConnectionException;

/**
 * Abstract base class for Bedrock connection providers. Extracts common connection logic to reduce code duplication.
 *
 * @param <CONNECTION> The type of connection this provider creates
 */
public abstract class AbstractBedrockConnectionProvider<CONNECTION extends ConnectorConnection>
    extends AWSConnectionProvider<CONNECTION> {

  /**
   * Creates a connection using the provided common parameters. This template method implements the common connection logic.
   *
   * @return The created connection
   * @throws ConnectionException if connection cannot be established
   */
  @Override
  public CONNECTION connect() throws ConnectionException {
    try {
      CommonParameters commonParams = getCommonParameters();

      if (commonParams.isTryDefaultAWSCredentialsProviderChain() ||
          isPresent(commonParams.getAccessKey()) &&
              isPresent(commonParams.getSecretKey())) {

        CONNECTION connection = buildConnection(commonParams);
        this.onConnect(connection);
        return connection;
      } else {
        throw new AWSConnectionException("Access Key or Secret Key is blank");
      }
    } catch (AWSConnectionException e) {
      throw new ConnectionException(e.getMessage(), e);
    }
  }

  /**
   * Builds the connection with the given common parameters. Subclasses implement this to create their specific connection type.
   *
   * @param commonParams The common connection parameters
   * @return The connection instance
   * @throws AWSConnectionException if connection cannot be built
   * @throws ConnectionException if connection cannot be established
   */
  protected abstract CONNECTION buildConnection(CommonParameters commonParams) throws AWSConnectionException, ConnectionException;
}
