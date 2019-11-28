package jenkins.plugins.mattermost;

import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.junit.Assert.*;

public class StandardMattermostServiceTest {
	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();

  /**
   * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
   */
  @Test
  public void publishWithBadHostShouldNotRethrowExceptions() {
	  StandardMattermostService service = new StandardMattermostService("http://foo", "#general", "");
    service.setEndpoint("hostvaluethatwillcausepublishtofail");
    service.publish("message");
  }

  /** Use a valid host, but an invalid team domain */
  @Test
  public void invalidHostShouldFail() {
	  StandardMattermostService service = new StandardMattermostService("http://my", "#general", "");
    service.publish("message");
  }

  @Test
  public void publishToASingleRoomSendsASingleMessage() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    service.setHttpClient(httpClientStub);
    service.publish("message");
    assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
  }

  @Test
  public void publishToMultipleRoomsSendsAMessageToEveryRoom() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1,#room2,#room3", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    service.setHttpClient(httpClientStub);
    service.publish("message");
    assertEquals(3, service.getHttpClient().getNumberOfCallsToExecuteMethod());
  }

  @Test
  public void successfulPublishToASingleRoomReturnsTrue() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    httpClientStub.setHttpStatus(HttpStatus.SC_OK);
    service.setHttpClient(httpClientStub);
    assertTrue(service.publish("message"));
  }

  @Test
  public void successfulPublishToMultipleRoomsReturnsTrue() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1,#room2,#room3", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    httpClientStub.setHttpStatus(HttpStatus.SC_OK);
    service.setHttpClient(httpClientStub);
    assertTrue(service.publish("message"));
  }

  @Test
  public void failedPublishToASingleRoomReturnsFalse() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    httpClientStub.setHttpStatus(HttpStatus.SC_NOT_FOUND);
    service.setHttpClient(httpClientStub);
    assertFalse(service.publish("message"));
  }

  @Test
  public void singleFailedPublishToMultipleRoomsReturnsFalse() {
    StandardMattermostServiceStub service =
			new StandardMattermostServiceStub("http://endpoint", "#room1,#room2,#room3", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    httpClientStub.setFailAlternateResponses(true);
    httpClientStub.setHttpStatus(HttpStatus.SC_OK);
    service.setHttpClient(httpClientStub);
    assertFalse(service.publish("message"));
  }

  @Test
  public void publishToEmptyRoomReturnsTrue() {
	  StandardMattermostServiceStub service = new StandardMattermostServiceStub("http://endpoint", "", "");
    HttpClientStub httpClientStub = new HttpClientStub();
    httpClientStub.setHttpStatus(HttpStatus.SC_OK);
    service.setHttpClient(httpClientStub);
    assertTrue(service.publish("message"));
  }

  @Test
  public void isProxyRequiredEmtyNoProxyHostsReturnsTrue() {
    StandardMattermostService service =
        new StandardMattermostService("http://mymattermost.endpoint.com", "roomid", "icon");
	  assertTrue(service.isProxyRequired(Collections.emptyList()));
  }

  @Test
  public void isProxyRequiredNoProxyHostsDoesNotMatchReturnsTrue() {
    StandardMattermostService service =
        new StandardMattermostService("http://mymattermost.endpoint.com", "roomid", "icon");
    assertTrue(
            service.isProxyRequired("*.internal.com"));
  }

  @Test
  public void isProxyRequiredNoProxyHostsMatchReturnsFalse() {
    StandardMattermostService service =
        new StandardMattermostService("http://mymattermost.endpoint.com", "roomid", "icon");
    assertFalse(
            service.isProxyRequired("*.endpoint.com"));
  }

  @Test
  public void isProxyRequiredInvalidEndPointReturnsTrue() {
    StandardMattermostService service =
			new StandardMattermostService("http://mymattermost.endpoint.com", "roomid", "icon");
    assertTrue(
            service.isProxyRequired("*.internal.com"));
  }
}
