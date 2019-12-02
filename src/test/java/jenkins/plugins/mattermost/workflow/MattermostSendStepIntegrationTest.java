package jenkins.plugins.mattermost.workflow;


import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Assert;
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
	  //jenkinsRule.assertLogContains(
	  String format = String.format(
            "Mattermost Send Pipeline step configured values from global config - connector: %s, icon: %s, channel: %s, color: %s",
			  false, false, false, false);
	  String log = JenkinsRule.getLog(run);
	  Assert.assertTrue(log.contains(format));
	  //   run);
  }

  @Test
  public void test_fail_on_error() throws Exception {
    WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
    // just define message
    job.setDefinition(
        new CpsFlowDefinition(
            "mattermostSend(message: 'message', endpoint: 'endpoint', icon: 'icon', channel: '#channel', color: 'good', failOnError: true);",
            true));
	  //noinspection ConstantConditions
	  //job.scheduleBuild(0,new Cause.UserIdCause());
	  QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = job.scheduleBuild2(0);
	  //WorkflowRun workflowRun = workflowRunQueueTaskFuture.getStartCondition().get();
	  //workflowRun.getExecutionPromise().get();
	  WorkflowRun run = jenkinsRule.assertBuildStatus(Result.FAILURE, workflowRunQueueTaskFuture);
	  //jenkinsRule.assertBuildStatusSuccess(workflowRun);
    // everything should come from step configuration
	  String log = JenkinsRule.getLog(run);
	  Assert.assertTrue(log.contains("Mattermost notification failed. See Jenkins logs for details."));
	  //TODO jenkinsRule.assertLogContains(
	  //   "Mattermost notification failed. See Jenkins logs for details.", run);
  }


	public void testJenkinsWorkflow() throws Exception
	{
		WorkflowJob project = jenkinsRule.createProject(WorkflowJob.class);
		project.setDefinition(new CpsFlowDefinition(
				"node {" +
						"  writeFile text: 'hello', file: 'greeting.txt'" +
						"}", true));

		WorkflowRun r = project.scheduleBuild2(0).getStartCondition().get();
		while (true)
		{
			System.out.print(".");
		}
		//jenkinsRule.assertBuildStatusSuccess(r);
	}


	@Test
	public void testHttpPost() throws Exception
	{
		WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
		job.setDefinition(
				new CpsFlowDefinition(
						"mattermostSend(message: 'test please ignore', endpoint: 'https://localhost:8080/hooks/9src4cpiatbz3qpbr76rxrwf7e', icon: 'icon', channel: '#jenkins', color: 'good');",
						true));
		TestListener target = new TestListener(8080, "/hooks/9src4cpiatbz3qpbr76rxrwf7e");
		Thread thread = new Thread(target);
		thread.start();
		WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
		String poll = target.messages.poll();
		Assert.assertTrue(poll.contains("http tester"));
		thread.stop();
	}

}
