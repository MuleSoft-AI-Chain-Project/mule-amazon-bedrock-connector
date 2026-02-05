package com.mulesoft.connectors.bedrock.internal.error.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;

public class AWSConnectionException extends MuleRuntimeException {

  public AWSConnectionException(String message) {
    super(I18nMessageFactory.createStaticMessage(message));
  }

  public AWSConnectionException(String message, Throwable cause) {
    super(I18nMessageFactory.createStaticMessage(message), cause);
  }
}
