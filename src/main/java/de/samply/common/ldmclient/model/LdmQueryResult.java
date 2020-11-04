package de.samply.common.ldmclient.model;

import de.samply.share.model.common.Error;
import de.samply.share.model.common.QueryResultStatistic;

public class LdmQueryResult {

  public static LdmQueryResult EMPTY = new LdmQueryResult();
  private final Error error;
  private final QueryResultStatistic result;

  public LdmQueryResult(QueryResultStatistic result) {
    this.result = result;
    this.error = null;
  }

  public LdmQueryResult(Error error) {
    this.result = null;
    this.error = error;
  }

  private LdmQueryResult() {
    this.result = null;
    this.error = null;
  }

  public Error getError() {
    return error;
  }

  public QueryResultStatistic getResult() {
    return result;
  }

  public boolean hasResult() {
    return this.result != null;
  }

  public boolean hasError() {
    return this.error != null;
  }

  public boolean isEmpty() {
    return !hasResult() && !hasError();
  }
}
