package jenkins.plugins.mattermost;

public class StandardMattermostServiceStub extends StandardMattermostService {

    private HttpClientStub httpClientStub;

    public StandardMattermostServiceStub(String host, String token, String roomId) {
        super(host, token, roomId);
    }

    @Override
    public HttpClientStub getHttpClient() {
        return httpClientStub;
    }

    public void setHttpClient(HttpClientStub httpClientStub) {
        this.httpClientStub = httpClientStub;
    }
}
