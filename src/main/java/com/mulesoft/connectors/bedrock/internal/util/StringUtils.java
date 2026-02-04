package com.mulesoft.connectors.bedrock.internal.util;

import java.util.Optional;
import java.util.function.Predicate;

public class StringUtils {

  /**
   * Returns false if the String param is null or empty. Otherwise returns true.
   *
   * @param string
   * @return boolean result
   */
  public static boolean isPresent(String string) {
    return Optional.ofNullable(string)
        .filter(Predicate.isEqual("").negate())
        .isPresent();
  }
}
