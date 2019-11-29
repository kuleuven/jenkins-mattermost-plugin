package jenkins.plugins.mattermost.workflow;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

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
						"mattermostSend(message: 'http tester', endpoint: 'http://127.0.0.1:8080/', icon: 'icon', channel: '#channel', color: 'good');",
						true));
		TestListener target = new TestListener();
		Thread thread = new Thread(target);
		thread.start();
		WorkflowRun run = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
		String poll = target.messages.poll();
		Assert.assertTrue(poll.contains("http tester"));
		thread.stop();
	}

	public static class TestListener implements Runnable
	{
		public BlockingArrayQueue<String> messages = new BlockingArrayQueue<>();

		@Override
		public void run()
		{
			HttpServer server = null;
			try
			{
				server = HttpServer.create(new InetSocketAddress(8080), 0);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
			server.createContext("/", new MyHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
		}

		class MyHandler implements HttpHandler
		{
			@Override
			public void handle(HttpExchange t) throws IOException
			{
				String response = "This is the response";
				t.sendResponseHeaders(200, response.length());
				InputStream requestBody = t.getRequestBody();
				BufferedReader br = new BufferedReader(new InputStreamReader(requestBody, Charset.defaultCharset()));
				messages.addAll(br.lines().map(obj ->
				{
					try
					{
						return URLDecoder.decode(obj, "UTF-8");
					} catch (UnsupportedEncodingException ignored)
					{
					}
					return obj;
				})
						.collect(Collectors.toList()));
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
		}

	}
}
