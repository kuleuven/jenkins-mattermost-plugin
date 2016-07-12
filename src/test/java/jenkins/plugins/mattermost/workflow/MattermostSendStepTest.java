package jenkins.plugins.mattermost.workflow;

import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.Messages;
import jenkins.plugins.mattermost.SlackNotifier;
import jenkins.plugins.mattermost.SlackService;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;

/**
 * Traditional Unit tests, allows testing null Jenkins,getInstance()
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class,MattermostSendStep.class})
public class MattermostSendStepTest {

    @Mock
    TaskListener taskListenerMock;
    @Mock
    PrintStream printStreamMock;
    @Mock
    PrintWriter printWriterMock;
    @Mock
    StepContext stepContextMock;
    @Mock
    SlackService slackServiceMock;
    @Mock
    Jenkins jenkins;
    @Mock
    SlackNotifier.DescriptorImpl slackDescMock;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        when(jenkins.getDescriptorByType(SlackNotifier.DescriptorImpl.class)).thenReturn(slackDescMock);
    }

    @Test
    public void testStepOverrides() throws Exception {
        MattermostSendStep.SlackSendStepExecution stepExecution = spy(new MattermostSendStep.SlackSendStepExecution());
        MattermostSendStep mattermostSendStep = new MattermostSendStep("message");
        mattermostSendStep.setToken("token");
        mattermostSendStep.setTeamDomain("teamDomain");
        mattermostSendStep.setChannel("channel");
        mattermostSendStep.setColor("good");
        stepExecution.step = mattermostSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(slackDescMock.getToken()).thenReturn("differentToken");


        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getSlackService(anyString(), anyString(), anyString())).thenReturn(slackServiceMock);
        when(slackServiceMock.publish(anyString(), anyString())).thenReturn(true);

        stepExecution.run();
        verify(stepExecution, times(1)).getSlackService("teamDomain", "token", "channel");
        verify(slackServiceMock, times(1)).publish("message", "good");
        assertFalse(stepExecution.step.isFailOnError());
    }

    @Test
    public void testValuesForGlobalConfig() throws Exception {

        MattermostSendStep.SlackSendStepExecution stepExecution = spy(new MattermostSendStep.SlackSendStepExecution());
        stepExecution.step = new MattermostSendStep("message");

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(slackDescMock.getTeamDomain()).thenReturn("globalTeamDomain");
        when(slackDescMock.getToken()).thenReturn("globalToken");
        when(slackDescMock.getRoom()).thenReturn("globalChannel");

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getSlackService(anyString(), anyString(), anyString())).thenReturn(slackServiceMock);

        stepExecution.run();
        verify(stepExecution, times(1)).getSlackService("globalTeamDomain", "globalToken", "globalChannel");
        verify(slackServiceMock, times(1)).publish("message", "");
        assertNull(stepExecution.step.getTeamDomain());
        assertNull(stepExecution.step.getToken());
        assertNull(stepExecution.step.getChannel());
        assertNull(stepExecution.step.getColor());
    }

    @Test
    public void testNonNullEmptyColor() throws Exception {

        MattermostSendStep.SlackSendStepExecution stepExecution = spy(new MattermostSendStep.SlackSendStepExecution());
        MattermostSendStep mattermostSendStep = new MattermostSendStep("message");
        mattermostSendStep.setColor("");
        stepExecution.step = mattermostSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(stepExecution.getSlackService(anyString(), anyString(), anyString())).thenReturn(slackServiceMock);

        stepExecution.run();
        verify(slackServiceMock, times(1)).publish("message", "");
        assertNull(stepExecution.step.getColor());
    }

    @Test
    public void testNullJenkinsInstance() throws Exception {

        MattermostSendStep.SlackSendStepExecution stepExecution = spy(new MattermostSendStep.SlackSendStepExecution());
        stepExecution.step = new MattermostSendStep("message");

        when(Jenkins.getInstance()).thenThrow(NullPointerException.class);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.error(anyString())).thenReturn(printWriterMock);
        doNothing().when(printStreamMock).println();

        stepExecution.run();
        verify(taskListenerMock, times(1)).error(Messages.NotificationFailedWithException(anyString()));
    }
}
