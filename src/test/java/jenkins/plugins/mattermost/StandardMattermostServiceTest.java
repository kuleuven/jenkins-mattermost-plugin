package jenkins.plugins.mattermost;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandardMattermostServiceTest {

    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardMattermostService service = new StandardMattermostService("foo", "token", "#general");
        service.setHost("hostvaluethatwillcausepublishtofail");
        service.publish("message");
    }

    /**
     * Use a valid host, but an invalid team domain
     */
    @Test
    public void invalidHostShouldFail() {
        StandardMattermostService service = new StandardMattermostService("my", "token", "#general");
        service.publish("message");
    }

    /**
     * Use a valid team domain, but a bad token
     */
    @Test
    public void invalidTokenShouldFail() {
        StandardMattermostService service = new StandardMattermostService("tinyspeck", "token", "#general");
        service.publish("message");
    }

    @Test
    public void publishToASingleRoomSendsASingleMessage() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1");
        HttpClientStub httpClientStub = new HttpClientStub();
        service.setHttpClient(httpClientStub);
        service.publish("message");
        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
    }

    @Test
    public void publishToMultipleRoomsSendsAMessageToEveryRoom() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1,#room2,#room3");
        HttpClientStub httpClientStub = new HttpClientStub();
        service.setHttpClient(httpClientStub);
        service.publish("message");
        assertEquals(3, service.getHttpClient().getNumberOfCallsToExecuteMethod());
    }

    @Test
    public void successfulPublishToASingleRoomReturnsTrue() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1");
        HttpClientStub httpClientStub = new HttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        assertTrue(service.publish("message"));
    }

    @Test
    public void successfulPublishToMultipleRoomsReturnsTrue() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1,#room2,#room3");
        HttpClientStub httpClientStub = new HttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        assertTrue(service.publish("message"));
    }

    @Test
    public void failedPublishToASingleRoomReturnsFalse() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1");
        HttpClientStub httpClientStub = new HttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_NOT_FOUND);
        service.setHttpClient(httpClientStub);
        assertFalse(service.publish("message"));
    }

    @Test
    public void singleFailedPublishToMultipleRoomsReturnsFalse() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "#room1,#room2,#room3");
        HttpClientStub httpClientStub = new HttpClientStub();
        httpClientStub.setFailAlternateResponses(true);
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        assertFalse(service.publish("message"));
    }

    @Test
    public void publishToEmptyRoomReturnsTrue() {
        StandardMattermostServiceStub service = new StandardMattermostServiceStub("domain", "token", "");
        HttpClientStub httpClientStub = new HttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        assertTrue(service.publish("message"));
    }
}
