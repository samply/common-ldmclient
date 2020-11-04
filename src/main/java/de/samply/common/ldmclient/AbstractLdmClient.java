package de.samply.common.ldmclient;

import de.samply.common.ldmclient.model.LdmQueryResult;
import de.samply.share.model.common.Error;
import de.samply.share.model.common.QueryResultStatistic;
import de.samply.share.model.common.Result;
import java.io.Serializable;
import org.apache.http.impl.client.CloseableHttpClient;

public abstract class AbstractLdmClient<T_RESULT extends Result,
    ResultStatisticsT extends Serializable,
    ErrorT extends Serializable> {


  public static final String REST_PATH_REQUESTS = "requests";
  /**
   * Unclassified error with attached stacktrace.
   */
  public static final int ERROR_CODE_UNCLASSIFIED_WITH_STACKTRACE = 1000;
  /**
   * Function is not adequately implemented.
   */
  public static final int ERROR_CODE_UNIMPLEMENTED = 1001;
  /**
   * Failed to parse a date value.
   */
  public static final int ERROR_CODE_DATE_PARSING_ERROR = 1002;
  /**
   * One or more MDR Keys were not known. List of unknown keys is attached.
   */
  public static final int ERROR_CODE_UNKNOWN_MDRKEYS = 1003;
  protected static final String REST_PATH_RESULT = "result";
  protected static final String REST_PATH_STATS = "stats";
  protected static final String REST_PARAM_PAGE = "?page=";
  protected static final String REST_RESULTS_ONLY_SUFFIX = "?statisticsOnly=true";
  private transient CloseableHttpClient httpClient;
  private String ldmBaseUrl;

  public AbstractLdmClient(CloseableHttpClient httpClient, String ldmBaseUrl) {
    this.httpClient = httpClient;
    this.ldmBaseUrl = ldmBaseUrl;
  }

  protected abstract Class<T_RESULT> getResultClass();

  protected abstract Class<ResultStatisticsT> getStatisticsClass();

  protected abstract Class<ErrorT> getErrorClass();

  protected String getFullPath(boolean statisticsOnly) {
    String path = REST_PATH_REQUESTS;
    if (statisticsOnly) {
      path = path + REST_RESULTS_ONLY_SUFFIX;
    }

    return LdmClientUtil.addTrailingSlash(getLdmBaseUrl()) + path;
  }

  /**
   * Get a combination of LDM name and version number.
   *
   * @return LDM Name and version number. Separated by a single slash.
   */
  public abstract String getUserAgentInfo() throws LdmClientException;

  /**
   * Get the complete result that is found at the given location.
   *
   * @param location The location (URL) where the result can be found
   * @return the query result.
   */
  public abstract T_RESULT getResult(String location) throws LdmClientException;

  /**
   * Get version information.
   *
   * @return String representation of the Version Number
   */
  public abstract String getVersionString() throws LdmClientException;

  public abstract LdmQueryResult getStatsOrError(String location) throws LdmClientException;

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public String getLdmBaseUrl() {
    return ldmBaseUrl;
  }

  /**
   * Get the query result statistics file that is found at the given location under the resource
   * /stats.
   *
   * @param location The location (URL) where the statistics can be found
   * @return the query result statistics file if the request could be processed
   */
  public QueryResultStatistic getQueryResultStatistic(String location) throws LdmClientException {
    LdmQueryResult ldmQueryResult = getStatsOrError(location);

    if (ldmQueryResult == null) {
      throw new LdmClientException("Stats not readable");
    } else if (ldmQueryResult.hasResult()) {
      return ldmQueryResult.getResult();
    } else {
      throw new LdmClientException("Stats not available. Get error file instead.");
    }
  }

  /**
   * Get the error file that is found at the given location under the resource /stats.
   *
   * @param location The location (URL) where the error can be found
   * @return the error file
   */
  public Error getError(String location) throws LdmClientException {
    LdmQueryResult ldmQueryResult = getStatsOrError(location);

    if (ldmQueryResult == null) {
      throw new LdmClientException("Error not readable");
    } else if (ldmQueryResult.hasError()) {
      return ldmQueryResult.getError();
    } else {
      throw new LdmClientException(
          "Error not available. Try to get query result statistics file instead.");
    }
  }


  /**
   * Get the amount of entities in the result.
   *
   * @param location The location (URL) where the result can be found
   * @return the amount of entities (most likely patients) found
   */
  public Integer getResultCount(String location) throws LdmClientException {
    Integer totalSize = null;
    QueryResultStatistic queryResultStatistic = getQueryResultStatistic(location);
    if (queryResultStatistic != null) {
      totalSize = queryResultStatistic.getTotalSize();
    }
    return totalSize;
  }

}
