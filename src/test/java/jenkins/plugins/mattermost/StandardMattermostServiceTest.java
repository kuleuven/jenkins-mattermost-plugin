package jenkins.plugins.mattermost;

import hudson.ProxyConfiguration;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class StandardMattermostServiceTest {

	/**
	 * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
	 */
	@Test
	public void publishWithBadHostShouldNotRethrowExceptions() {
		StandardMattermostService service = new StandardMattermostService("foo", "#general", "");
		service.setEndpoint("hostvaluethatwillcausepublishtofail");
		service.publish("message");
	}

	/**
	 * Use a valid host, but an invalid team domain
	 */
	@Test
	public void invalidHostShouldFail() {
		StandardMattermostService service = new StandardMattermostService("my", "#general", "");
		service.publish("message");
	}

	@Test
	public void publishToASingleRoomSendsASingleMessage() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		service.setHttpClient(httpClientStub);
		service.publish("message");
		assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
	}

	@Test
	public void publishToMultipleRoomsSendsAMessageToEveryRoom() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1,#room2,#room3", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		service.setHttpClient(httpClientStub);
		service.publish("message");
		assertEquals(3, service.getHttpClient().getNumberOfCallsToExecuteMethod());
	}

	@Test
	public void successfulPublishToASingleRoomReturnsTrue() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		httpClientStub.setHttpStatus(HttpStatus.SC_OK);
		service.setHttpClient(httpClientStub);
		assertTrue(service.publish("message"));
	}

	@Test
	public void successfulPublishToMultipleRoomsReturnsTrue() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1,#room2,#room3", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		httpClientStub.setHttpStatus(HttpStatus.SC_OK);
		service.setHttpClient(httpClientStub);
		assertTrue(service.publish("message"));
	}

	@Test
	public void failedPublishToASingleRoomReturnsFalse() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		httpClientStub.setHttpStatus(HttpStatus.SC_NOT_FOUND);
		service.setHttpClient(httpClientStub);
		assertFalse(service.publish("message"));
	}

	@Test
	public void singleFailedPublishToMultipleRoomsReturnsFalse() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "#room1,#room2,#room3", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		httpClientStub.setFailAlternateResponses(true);
		httpClientStub.setHttpStatus(HttpStatus.SC_OK);
		service.setHttpClient(httpClientStub);
		assertFalse(service.publish("message"));
	}

	@Test
	public void publishToEmptyRoomReturnsTrue() {
		StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "", "");
		HttpClientStub httpClientStub = new HttpClientStub();
		httpClientStub.setHttpStatus(HttpStatus.SC_OK);
		service.setHttpClient(httpClientStub);
		assertTrue(service.publish("message"));
	}

	@Test
	public void isProxyRequiredEmtyNoProxyHostsReturnsTrue() {
		StandardMattermostService service = new StandardMattermostService("http://mymattermost.endpoint.com","roomid","icon");
		assertTrue(service.isProxyRequired(Collections.<Pattern>emptyList()));
	}

	@Test
	public void isProxyRequiredNoProxyHostsDoesNotMatchReturnsTrue() {
		StandardMattermostService service = new StandardMattermostService("http://mymattermost.endpoint.com","roomid","icon");
		assertTrue(service.isProxyRequired(ProxyConfiguration.getNoProxyHostPatterns("*.internal.com")));
	}

	@Test
	public void isProxyRequiredNoProxyHostsMatchReturnsFalse() {
		StandardMattermostService service = new StandardMattermostService("http://mymattermost.endpoint.com","roomid","icon");
		assertFalse(service.isProxyRequired(ProxyConfiguration.getNoProxyHostPatterns("*.endpoint.com")));
	}

	@Test
	public void isProxyRequiredInvalidEndPointReturnsTrue() {
		StandardMattermostService service = new StandardMattermostService("htt://mymattermost.endpoint.com","roomid","icon");
		assertTrue(service.isProxyRequired(ProxyConfiguration.getNoProxyHostPatterns("*.internal.com")));
	}


}
