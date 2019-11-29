package jenkins.plugins.mattermost.workflow;

import hudson.model.TaskListener;
import hudson.util.HistoricalSecrets;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.mattermost.MattermostNotifier;
import jenkins.plugins.mattermost.MattermostService;
import jenkins.security.ConfidentialStore;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/** Traditional Unit tests, allows testing null Jenkins,getInstance() */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, ConfidentialStore.class, MattermostSendStep.class, HistoricalSecrets.class})
@PowerMockIgnore({"javax.crypto.*", "javax.net.ssl.*"}) // https://github.com/powermock/powermock/issues/294
public class MattermostSendStepTest {

  @Mock TaskListener taskListenerMock;
  @Mock PrintStream printStreamMock;
  @Mock PrintWriter printWriterMock;
  @Mock StepContext stepContextMock;
  @Mock MattermostService mattermostServiceMock;
  @Mock Jenkins jenkins;
  @Mock MattermostNotifier.DescriptorImpl mattermostDescMock;
	private MattermostSendStep.MattermostSendStepExecution stepExecution;
	private MattermostSendStep mattermostSendStep;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(Jenkins.class);
	  //when(jenkins.getDescriptorByType(MattermostNotifier.DescriptorImpl.class)) -> null ref
	  when(jenkins.getDescriptor("mattermostNotifier")).thenReturn(mattermostDescMock);
	  mattermostSendStep = new MattermostSendStep("message");
	  stepExecution = spy(new MattermostSendStep.MattermostSendStepExecution(stepContextMock, mattermostSendStep));
	  stepExecution.step = mattermostSendStep;

	  when(Jenkins.getInstance()).thenReturn(jenkins);
	  when(Jenkins.get()).thenReturn(jenkins);
  }

  @Test
  public void testStepOverrides() throws Exception {
    mattermostSendStep.setIcon("icon");
    mattermostSendStep.setEndpoint("endpoint");
    mattermostSendStep.setChannel("channel");
    mattermostSendStep.setColor("good");

    stepExecution.listener = taskListenerMock;

    when(mattermostDescMock.getIcon()).thenReturn("differentIcon");

    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();

    when(stepExecution.getMattermostService(anyString(), anyString(), anyString()))
        .thenReturn(mattermostServiceMock);
    when(mattermostServiceMock.publish(anyString(), anyString())).thenReturn(true);

    stepExecution.run();
    verify(stepExecution, times(1)).getMattermostService("endpoint", "channel", "icon");
    verify(mattermostServiceMock, times(1)).publish("message", "", "good");
    assertFalse(stepExecution.step.isFailOnError());
  }

  @Test
  public void testValuesForGlobalConfig() throws Exception {


    stepExecution.listener = taskListenerMock;

    PowerMockito.mockStatic(ConfidentialStore.class);
    ConfidentialStore csMock = mock(ConfidentialStore.class);
    when(ConfidentialStore.get()).thenReturn(csMock);
    when(csMock.randomBytes(Matchers.anyInt()))
        .thenAnswer(it -> new byte[(Integer) (it.getArguments()[0])]);

    Secret encryptedEndpoint = Secret.fromString("globalEndpoint");
    when(mattermostDescMock.getEndpoint()).thenReturn(encryptedEndpoint);
    when(mattermostDescMock.getIcon()).thenReturn("globalIcon");
    when(mattermostDescMock.getRoom()).thenReturn("globalChannel");

    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();

    when(stepExecution.getMattermostService(anyString(), anyString(), anyString()))
        .thenReturn(mattermostServiceMock);

    stepExecution.run();
    verify(stepExecution, times(1))
        .getMattermostService("globalEndpoint", "globalChannel", "globalIcon");
    verify(mattermostServiceMock, times(1)).publish("message", "", "");
    assertNull(stepExecution.step.getEndpoint());
    assertNull(stepExecution.step.getIcon());
    assertNull(stepExecution.step.getChannel());
    assertNull(stepExecution.step.getColor());
    assertNull(stepExecution.step.getText());
  }

  @Test
  public void testNonNullEmptyColor() throws Exception {
	  PowerMockito.mockStatic(ConfidentialStore.class);
	  ConfidentialStore csMock = mock(ConfidentialStore.class);
	  when(ConfidentialStore.get()).thenReturn(csMock);
	  when(csMock.randomBytes(Matchers.anyInt()))
			  .thenAnswer(it -> new byte[(Integer) (it.getArguments()[0])]);
	  Secret encryptedEndpoint = Secret.fromString("globalEndpoint");
	  when(mattermostDescMock.getEndpoint()).thenReturn(encryptedEndpoint);
    mattermostSendStep.setColor("");
	  mattermostSendStep.setChannel("channel");
	  mattermostSendStep.setIcon("");

    stepExecution.listener = taskListenerMock;

    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();

    when(stepExecution.getMattermostService(anyString(), anyString(), anyString()))
        .thenReturn(mattermostServiceMock);

    stepExecution.run();
    verify(mattermostServiceMock, times(1)).publish("message", "", "");
    assertNull(stepExecution.step.getColor());
  }

	@Deprecated
  public void testNonNullPretext() throws Exception {
		TestListener target = new TestListener(8080);
		Thread thread = new Thread(target);
		thread.start();
		PowerMockito.mockStatic(ConfidentialStore.class);
		ConfidentialStore csMock = mock(ConfidentialStore.class);
		when(ConfidentialStore.get()).thenReturn(csMock);
		when(csMock.randomBytes(Matchers.anyInt()))
				.thenAnswer(it -> new byte[(Integer) (it.getArguments()[0])]);
		Secret encryptedEndpoint = Secret.fromString("http://localhost:8080");
		when(mattermostDescMock.getEndpoint()).thenReturn(encryptedEndpoint);
    mattermostSendStep.setText("@foo @bar");
		mattermostSendStep.setChannel("channel");
    stepExecution.listener = taskListenerMock;

    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();


    stepExecution.run();
    verify(mattermostServiceMock, times(1)).publish("message", "@foo @bar", "");
  }

  @Test
  public void testNullJenkinsInstance() throws Exception {

    when(Jenkins.getInstance()).thenThrow(NullPointerException.class);
    when(Jenkins.get()).thenThrow(NullPointerException.class);

    stepExecution.listener = taskListenerMock;

    when(taskListenerMock.error(anyString())).thenReturn(printWriterMock);
    doNothing().when(printStreamMock).println();

    stepExecution.run();
    verify(taskListenerMock, times(1)).error(anyString(), any(Exception.class));
  }
}
