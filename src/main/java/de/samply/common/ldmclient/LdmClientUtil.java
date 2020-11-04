package de.samply.common.ldmclient;

import java.util.Collection;
import java.util.Map;

public class LdmClientUtil {

  private LdmClientUtil() {

  }

  /**
   * Modifies a String, so that it ends with exactly one slash.
   *
   * @param source the source string
   * @return the string, ending with a slash
   */
  public static String addTrailingSlash(String source) {
    if (source == null) {
      return "";
    }
    return source.replaceAll("/+$", "") + "/";
  }

  public static boolean isNullOrEmpty(final String s) {
    return s == null || s.isEmpty();
  }

  public static boolean isNullOrEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public static boolean isNullOrEmpty(final Map<?, ?> m) {
    return m == null || m.isEmpty();
  }
}
