package de.samply.common.ldmclient;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.samply.share.model.common.Result;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LdmClientCqlQuery<
    T_RESULT extends Result,
    ResultStatisticsT extends Serializable,
    ErrorT extends Serializable> extends
    AbstractLdmClient<T_RESULT, ResultStatisticsT, ErrorT> {

  private static final Logger logger = LoggerFactory.getLogger(LdmClientCqlQuery.class);

  public LdmClientCqlQuery(CloseableHttpClient httpClient, String ldmBaseUrl) {
    super(httpClient, ldmBaseUrl);
  }

  private static String resourceLocation(String location) {
    Iterator<String> partIter = Splitter.on("/_history").split(location).iterator();
    return partIter.next();
  }

  private static JsonObject loadLibraryStub() {
    return loadJson("library-stub.json");
  }

  private static JsonObject loadMeasureStub(String entityType) {
    return loadJson(String.format("measure-%s-stub.json", entityType.toLowerCase()));
  }

  private static JsonObject loadJson(String name) {
    InputStream in = LdmClientCqlQuery.class.getResourceAsStream(name);
    return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
        .getAsJsonObject();
  }

  /**
   * Post query to the ldm.
   * @param query the query as String
   * @param entityType the entityType (patient or specimen)
   * @param statisticsOnly return data or only the count of the result
   * @return url of the result
   * @throws LdmClientException exception which can be thrown while posting the query to the ldm
   */
  public String postQuery(String query, String entityType, boolean statisticsOnly)
      throws LdmClientException {
    if (query == null) {
      throw new LdmClientException("Query is null.");
    }
    if (!statisticsOnly) {
      throw new LdmClientException("Currently only statistics are supported.");
    }

    String libraryUrl = "urn:uuid:" + UUID.randomUUID();
    JsonObject library = createLibrary(libraryUrl, query);
    postLibrary(library);
    JsonObject measure = createMeasure(libraryUrl, entityType);
    return postMeasure(measure);
  }

  private void postLibrary(JsonObject library) throws LdmClientException {
    String uri = getLdmBaseUrl() + "Library";
    HttpPost httpPost = new HttpPost(uri);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
    HttpEntity entity = new StringEntity(library.toString(), Consts.UTF_8);
    httpPost.setEntity(entity);

    try (CloseableHttpResponse response = getHttpClient().execute(httpPost)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_CREATED) {
        logger.error(String.format("Library not created. Status code: %d, Response: %s",
            statusCode, EntityUtils.toString(response.getEntity(), Consts.UTF_8)));
        throw new LdmClientException("Request not created. Received status code " + statusCode);
      }
    } catch (IOException e) {
      throw new LdmClientException(e);
    }
  }

  private String postMeasure(JsonObject measure) throws LdmClientException {
    String uri = getLdmBaseUrl() + "Measure";
    HttpPost httpPost = new HttpPost(uri);
    httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
    httpPost.setHeader(HttpHeaders.ACCEPT, "application/fhir+json");

    HttpEntity entity = new StringEntity(measure.toString(), Consts.UTF_8);
    httpPost.setEntity(entity);

    try (CloseableHttpResponse response = getHttpClient().execute(httpPost)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_CREATED) {
        logger.error(String.format("Measure not created. Status code: %d, Response: %s",
            statusCode, EntityUtils.toString(response.getEntity(), Consts.UTF_8)));
        throw new LdmClientException("Request not created. Received status code " + statusCode);
      }

      Header locationHeader = response.getFirstHeader("Location");
      if (locationHeader == null) {
        throw new LdmClientException("Location header is missing");
      }

      String measureUrl = resourceLocation(locationHeader.getValue());
      if (measureUrl.equals("")) {
        throw new LdmClientException("Location header is empty");
      }

      return measureUrl;
    } catch (IOException e) {
      throw new LdmClientException(e);
    }
  }

  private JsonObject createLibrary(String url, String query) {
    JsonObject library = loadLibraryStub();
    library.addProperty("url", url);
    String encodedQuery = Base64.getEncoder().encodeToString(query.getBytes());
    library.getAsJsonArray("content").get(0).getAsJsonObject().addProperty("data", encodedQuery);
    return library;
  }

  private JsonObject createMeasure(String libraryUrl, String entityType) {
    JsonObject measure = loadMeasureStub(entityType);
    measure.getAsJsonArray("library").add(libraryUrl);
    return measure;
  }
}
