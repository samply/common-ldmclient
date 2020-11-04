package de.samply.common.ldmclient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.samply.common.ldmclient.model.LdmQueryResult;
import de.samply.common.ldmclient.model.QueryResultPageKey;
import de.samply.share.model.common.QueryResultStatistic;
import de.samply.share.model.common.Result;
import de.samply.share.model.common.View;
import de.samply.share.utils.QueryConverter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to communicate with local datamanagement implementations.
 */
public abstract class LdmClientView<
    T_RESULT extends Result,
    ResultStatisticsT extends Serializable,
    ErrorT extends Serializable,
    SpecificViewT extends Serializable> extends
    AbstractLdmClient<T_RESULT, ResultStatisticsT, ErrorT> {

  protected static final int CACHE_DEFAULT_SIZE = 1000;
  private static final Logger logger = LoggerFactory.getLogger(LdmClientView.class);
  private final int cacheSize;
  private LdmClientView<T_RESULT, ResultStatisticsT, ErrorT, SpecificViewT>
      .QueryResultsCacheManager cacheManager;
  private boolean useCaching;
  private Map<String, String> httpHeaders = new HashMap<>();

  /**
   * Create an LdmClientView.
   *
   * @param httpClient the httpClient e.g. with proxy settings
   * @param ldmBaseUrl the url of the ldm
   * @param useCaching if caching should be used
   * @param cacheSize  the cacheSize
   * @throws LdmClientException if httpclient or ldmBaseUrl is null
   */
  public LdmClientView(CloseableHttpClient httpClient, String ldmBaseUrl, boolean useCaching,
      int cacheSize) throws LdmClientException {
    super(httpClient, ldmBaseUrl);
    if (httpClient == null) {
      throw new LdmClientException("No httpclient set");
    }
    if (LdmClientUtil.isNullOrEmpty(ldmBaseUrl)) {
      throw new LdmClientException("No LDM base URL provided");
    }
    this.useCaching = useCaching;
    this.cacheSize = cacheSize;
  }

  public boolean isLdmCentraxx() {
    return false;
  }

  public boolean isLdmSamplystoreBiobank() {
    return false;
  }

  protected abstract Class<SpecificViewT> getSpecificViewClass();

  protected abstract Class<?> getObjectFactoryClassForPostView();

  protected abstract Class<?> getObjectFactoryClassForResult();

  protected abstract SpecificViewT convertCommonViewToSpecificView(View view)
      throws JAXBException;

  protected abstract View convertSpecificViewToCommonView(SpecificViewT specificView)
      throws JAXBException;

  protected abstract LdmQueryResult convertQueryResultStatisticToCommonQueryResultStatistic(
      ResultStatisticsT qrs) throws JAXBException;

  protected abstract LdmQueryResult convertSpecificErrorToCommonError(ErrorT error)
      throws JAXBException;

  private QueryResultsCacheManager getCacheManager() {
    if (this.cacheManager == null) {
      this.cacheManager = new LdmClientView<T_RESULT, ResultStatisticsT, ErrorT, SpecificViewT>
          .QueryResultsCacheManager();
    }
    return this.cacheManager;
  }

  /**
   * Posts a view to local datamanagement and returns the location of the result.
   *
   * @param view a view (query + viewfields) object that is understood by the respective LDM. The
   *             implementing class has to take care of serializing the view
   * @return the location of the result
   */
  public String postView(View view) throws LdmClientException {
    return postView(view, false);
  }

  /**
   * Post the view to the ldm.
   *
   * @param view           the query as view
   * @param statisticsOnly return data or only the count of the result
   * @return url of the result
   * @throws LdmClientException exception which can be thrown while posting the view to the ldm
   */
  public String postView(View view, boolean statisticsOnly) throws LdmClientException {
    if (view == null) {
      throw new LdmClientException("View is null.");
    }

    String location;
    String viewString;

    try {
      SpecificViewT specificView = convertCommonViewToSpecificView(view);
      viewString = QueryConverter
          .marshal(specificView, JAXBContext.newInstance(getSpecificViewClass()));
    } catch (JAXBException e) {
      throw new LdmClientException(e);
    }

    String uri = getFullPath(statisticsOnly);
    HttpPost httpPost = new HttpPost(uri);
    addHttpHeaders(httpPost);
    HttpEntity entity = new StringEntity(viewString, Consts.UTF_8);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType());
    httpPost.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType());
    httpPost.setEntity(entity);

    int statusCode;
    try (CloseableHttpResponse response = getHttpClient().execute(httpPost)) {
      statusCode = response.getStatusLine().getStatusCode();

      Header locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
      if (locationHeader == null) {
        final String noLocationMsg = "No location found in Header";
        logger.error(noLocationMsg);
        throw new LdmClientException(noLocationMsg);
      }

      location = locationHeader.getValue();
    } catch (IOException e) {
      throw new LdmClientException(e);
    }

    if (statusCode != HttpStatus.SC_CREATED) {
      logger.error("Request not created. Received status code " + statusCode);
      throw new LdmClientException("Request not created. Received status code " + statusCode);
    } else if (LdmClientUtil.isNullOrEmpty(location)) {
      throw new LdmClientException("Empty location received");
    }

    return location;
  }

  /**
   * Get the given page of the result that is found at the given location.
   *
   * @param location The location (URL) where the result can be found
   * @param page     the number of the result page to be retrieved
   * @return the query result page
   */
  public T_RESULT getResultPage(String location, int page)
      throws LdmClientException, IndexOutOfBoundsException {
    if (useCaching) {
      try {
        return getCacheManager().getCache().get(new QueryResultPageKey(location, page));
      } catch (ExecutionException e) {
        logger.warn("Error when trying to use cache. Querying LDM Client directly.", e);
        return getResultPageWithoutCache(location, page);
      }
    } else {
      return getResultPageWithoutCache(location, page);
    }
  }

  /**
   * Get a single page of a query result from LDM Client.
   *
   * @param location the location of the result, as retrieved from LDM Client via the POST of the
   *                 request
   * @param page     the page index
   * @return the partial query result
   */
  private T_RESULT getResultPageWithoutCache(String location, int page)
      throws LdmClientException, IndexOutOfBoundsException {
    if (page < 0) {
      throw new IndexOutOfBoundsException("Requested page index < 0: " + page);
    }
    LdmQueryResult ldmQueryResult = getStatsOrError(location);

    if (ldmQueryResult != null && ldmQueryResult.hasResult()) {
      QueryResultStatistic queryResultStatistic = ldmQueryResult.getResult();
      if (queryResultStatistic.getNumberOfPages() < (page + 1)) { // pages start at 0
        throw new IndexOutOfBoundsException(
            "Requested page: " + page + " , number of pages is " + queryResultStatistic
                .getNumberOfPages());
      }
    } else {
      throw new LdmClientException("No QueryResultStatistics found at stats location.");
    }

    HttpGet httpGet = new HttpGet(
        LdmClientUtil.addTrailingSlash(location) + REST_PATH_RESULT + REST_PARAM_PAGE + page);
    addHttpHeaders(httpGet);

    if (isLdmCentraxx()) {
      // Apparently, it may take a bit longer to reply when a new user session has to be created...
      // so use an extensive timeout of 1 minute
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000)
          .setConnectTimeout(60000).setConnectionRequestTimeout(60000).build();
      httpGet.setConfig(requestConfig);
    }

    try {
      CloseableHttpResponse response = getHttpClient().execute(httpGet);
      int statusCode = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      String queryResultString = EntityUtils.toString(entity, Consts.UTF_8);
      EntityUtils.consume(entity);
      if (HttpStatus.SC_OK == statusCode) {
        return QueryConverter.unmarshal(queryResultString,
            JAXBContext.newInstance(getObjectFactoryClassForResult()), getResultClass());
      } else {
        throw new LdmClientException(
            "While trying to get Result page " + page + " statuscode " + statusCode
                + " was received from LDM client");
      }
    } catch (IOException | JAXBException e) {
      throw new LdmClientException(e);
    }
  }

  /**
   * Get the object that is found at the given location under the resource /stats.
   *
   * @param location The location (URL) where the statistics or error can be found
   * @return the query result statistics file if the request could be processed, an error object
   *        otherwise
   */
  public LdmQueryResult getStatsOrError(String location) throws LdmClientException {
    HttpGet httpGet = new HttpGet(LdmClientUtil.addTrailingSlash(location) + REST_PATH_STATS);
    addHttpHeaders(httpGet);

    if (isLdmCentraxx()) {
      // Apparently, it may take a bit longer to reply when a new user session has to be created...
      // so use an extensive timeout of 1 minute
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000)
          .setConnectTimeout(60000).setConnectionRequestTimeout(60000).build();
      httpGet.setConfig(requestConfig);
    }

    try {
      CloseableHttpResponse response = getHttpClient().execute(httpGet);
      int statusCode = response.getStatusLine().getStatusCode();
      HttpEntity entity = response.getEntity();
      String entityOutput = EntityUtils.toString(entity, Consts.UTF_8);
      if (statusCode == HttpStatus.SC_OK) {
        JAXBContext jaxbContext = JAXBContext.newInstance(getStatisticsClass());
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ResultStatisticsT qrs = (ResultStatisticsT) jaxbUnmarshaller
            .unmarshal(new StringReader(entityOutput));
        EntityUtils.consume(entity);
        response.close();
        return convertQueryResultStatisticToCommonQueryResultStatistic(qrs);
      } else if (statusCode == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
        JAXBContext jaxbContext = JAXBContext.newInstance(getErrorClass());
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ErrorT error = (ErrorT) jaxbUnmarshaller.unmarshal(new StringReader(entityOutput));
        EntityUtils.consume(entity);
        response.close();
        return convertSpecificErrorToCommonError(error);
      } else if (statusCode == HttpStatus.SC_ACCEPTED) {
        response.close();
        logger
            .debug("Statistics not written yet. LDM client is probably busy with another request.");
        return LdmQueryResult.EMPTY;
      } else {
        response.close();
        throw new LdmClientException("Unexpected response code: " + statusCode);
      }
    } catch (IOException | JAXBException e) {
      throw new LdmClientException("While trying to read stats/error", e);
    }
  }

  /**
   * Check if a given result page is available. This is not implemented here (contrary to
   * isQueryPresent()) because different LDMs might have different structures in the REST path.
   *
   * @param location  where to check
   * @param pageIndex the page to try to get
   * @return true if 200 OK is returned, false otherwise and on errors
   */
  public boolean isResultPageAvailable(String location, int pageIndex) {
    if (pageIndex < 0) {
      return false;
    }

    HttpHead httpHead = new HttpHead(
        LdmClientUtil.addTrailingSlash(location) + REST_PATH_RESULT + REST_PARAM_PAGE + pageIndex);
    addHttpHeaders(httpHead);

    if (isLdmCentraxx()) {
      // Apparently, it may take a bit longer to reply when a new user session has to be created...
      // so use an extensive timeout of 1 minute
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000)
          .setConnectTimeout(60000).setConnectionRequestTimeout(60000).build();
      httpHead.setConfig(requestConfig);
    }

    try {
      CloseableHttpResponse response = getHttpClient().execute(httpHead);
      int statusCode = response.getStatusLine().getStatusCode();
      return HttpStatus.SC_OK == statusCode;
    } catch (IOException e) {
      return false;
    }
  }

  private int getCacheSize() {
    return cacheSize;
  }

  public void cleanQueryResultsCache() {
    getCacheManager().cleanCache();
  }

  public void addHttpHeader(String httpHeader, String value) {
    this.httpHeaders.put(httpHeader, value);
  }

  /**
   * ToDo.
   *
   * @param httpRequestBase ToDo.
   */
  public void addHttpHeaders(HttpRequestBase httpRequestBase) {

    for (String header : httpHeaders.keySet()) {

      String value = httpHeaders.get(header);
      httpRequestBase.setHeader(header, value);

    }

  }

  private class QueryResultsCacheManager {

    private Logger logger = LoggerFactory.getLogger(LdmClientView.QueryResultsCacheManager.class);

    private LoadingCache<QueryResultPageKey, T_RESULT> queryResultCache;

    LoadingCache<QueryResultPageKey, T_RESULT> getCache() {
      if (queryResultCache == null) {
        logger.debug("query result cache is null...creating loader");
        CacheLoader<QueryResultPageKey, T_RESULT> loader = new CacheLoader<QueryResultPageKey,
            T_RESULT>() {
          public T_RESULT load(final QueryResultPageKey resultPageKey) throws LdmClientException {
            logger.debug(
                "QueryResult page was not in cache: " + resultPageKey.getLocation() + " page "
                    + resultPageKey.getPageIndex());
            return getResultPageWithoutCache(resultPageKey.getLocation(),
                resultPageKey.getPageIndex());
          }
        };
        queryResultCache = CacheBuilder.newBuilder().maximumSize(getCacheSize()).build(loader);
      }

      return queryResultCache;
    }

    void cleanCache() {
      if (queryResultCache != null) {
        queryResultCache.invalidateAll();
        logger.debug("Cache cleaned.");
      } else {
        logger.debug("Cache was null. Nothing to do.");
      }
    }
  }
}
