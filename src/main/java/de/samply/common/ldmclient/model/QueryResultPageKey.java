package de.samply.common.ldmclient.model;

public class QueryResultPageKey {

  private String location;
  private int pageIndex;

  public QueryResultPageKey(String location, int pageIndex) {
    this.location = location;
    this.pageIndex = pageIndex;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public int getPageIndex() {
    return pageIndex;
  }

  public void setPageIndex(int pageIndex) {
    this.pageIndex = pageIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueryResultPageKey that = (QueryResultPageKey) o;

    if (pageIndex != that.pageIndex) {
      return false;
    }
    return location.equals(that.location);
  }

  @Override
  public int hashCode() {
    int result = location.hashCode();
    result = 31 * result + pageIndex;
    return result;
  }

  @Override
  public String toString() {
    return "QueryResultPageKey{"
        + "location='" + location + '\''
        + ", pageIndex=" + pageIndex
        + '}';
  }
}
