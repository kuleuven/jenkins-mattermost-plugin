package jenkins.plugins.mattermost.workflow;


import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MattermostSendStepIntegrationTest {
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void configRoundTrip() throws Exception {
    MattermostSendStep step1 = new MattermostSendStep("message");
    step1.setColor("good");
    step1.setChannel("#channel");
    step1.setIcon("icon");
    step1.setEndpoint("teamDomain");
    step1.setFailOnError(true);

    MattermostSendStep step2 = new StepConfigTester(jenkinsRule).configRoundTrip(step1);
    jenkinsRule.assertEqualDataBoundBeans(step1, step2);
  }

  @Test
  public void test_global_config_override() throws Exception {
    WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
    // just define message
    job.setDefinition(
        new CpsFlowDefinition(
            "mattermostSend(message: 'message', endpoint: 'endpoint', icon: 'icon', channel: '#channel', color: 'good');",
            true));
    WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
    // everything should come from step configuration
    jenkinsRule.assertLogContains(
        String.format(
            "Mattermost Send Pipeline step configured values from global config - connector: %s, icon: %s, channel: %s, color: %s",
            false, false, false, false),
        run);
  }

  @Test
  public void test_fail_on_error() throws Exception {
    WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
    // just define message
    job.setDefinition(
        new CpsFlowDefinition(
            "mattermostSend(message: 'message', endpoint: 'endpoint', icon: 'icon', channel: '#channel', color: 'good', failOnError: true);",
            true));
    WorkflowRun run = jenkinsRule.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
    // everything should come from step configuration
    jenkinsRule.assertLogContains(
        "Mattermost notification failed. See Jenkins logs for details.", run);
  }
}
