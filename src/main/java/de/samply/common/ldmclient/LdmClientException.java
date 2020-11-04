package de.samply.common.ldmclient;

public class LdmClientException extends Exception {

  public LdmClientException() {
    super();
  }

  public LdmClientException(String s) {
    super(s);
  }

  public LdmClientException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public LdmClientException(Throwable throwable) {
    super(throwable);
  }

  protected LdmClientException(String s, Throwable throwable, boolean b, boolean b1) {
    super(s, throwable, b, b1);
  }
}
