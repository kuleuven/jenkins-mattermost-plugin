package jenkins.plugins.mattermost;

public class StandardMattermostServiceStub extends StandardMattermostService {

  private HttpClientStub httpClientStub;

  public StandardMattermostServiceStub(String host, String roomId, String icon) {
    super(host, roomId, icon);
  }

  @Override
  public HttpClientStub getHttpClient() {
    return httpClientStub;
  }

  public void setHttpClient(HttpClientStub httpClientStub) {
    this.httpClientStub = httpClientStub;
  }
}
