package com.mulesoft.connectors.bedrock.internal.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.Charsets;

public class BedrockModelFactory {

  public static InputStream createInputStream(String inputString) {
    return new ByteArrayInputStream(inputString.getBytes(Charsets.toCharset(StandardCharsets.UTF_8)));
  }

  public static InputStream identity(InputStream inputStream) {
    return inputStream;
  }
}
