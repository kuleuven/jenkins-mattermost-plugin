package jenkins.plugins.mattermost;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.triggers.SCMTrigger;
import hudson.util.LogTaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements FineGrainedNotifier {

	private static final Logger logger = Logger.getLogger(MattermostListener.class.getName());

	MattermostNotifier notifier;
	BuildListener listener;

	public ActiveNotifier(MattermostNotifier notifier, BuildListener listener) {
		super();
		this.notifier = notifier;
		this.listener = listener;
	}

	private MattermostService getMattermost(AbstractBuild r) {
		return notifier.newMattermostService(r, listener);
	}

	public void deleted(AbstractBuild r) {
	}

	public void started(AbstractBuild build) {

		AbstractProject<?, ?> project = build.getProject();

		CauseAction causeAction = build.getAction(CauseAction.class);

		if (causeAction != null) {
			Cause scmCause = causeAction.findCause(SCMTrigger.SCMTriggerCause.class);
			if (scmCause == null) {
				MessageBuilder message = new MessageBuilder(notifier, build);
				message.append(causeAction.getShortDescription());
				notifyStart(build, message.appendOpenLink().toString());
			}
		}

		String changes = getChanges(build, notifier.includeCustomMessage());
		if (changes != null) {
			notifyStart(build, changes);
		} else {
			notifyStart(build, getBuildStatusMessage(build, false, notifier.includeCustomMessage()));
		}
	}

	private void notifyStart(AbstractBuild build, String message) {
		AbstractProject<?, ?> project = build.getProject();
		AbstractBuild<?, ?> previousBuild = project.getLastBuild().getPreviousCompletedBuild();
		if (previousBuild == null) {
			getMattermost(build).publish(message, "good");
		} else {
			getMattermost(build).publish(message, getBuildColor(previousBuild));
		}
	}

	public void finalized(AbstractBuild r) {
	}

	public void completed(AbstractBuild r) {
		AbstractProject<?, ?> project = r.getProject();
		Result result = r.getResult();
		AbstractBuild<?, ?> previousBuild = project.getLastBuild();
		do {
			previousBuild = previousBuild.getPreviousCompletedBuild();
		} while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
		Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
		if ((result == Result.ABORTED && notifier.getNotifyAborted())
				|| (result == Result.FAILURE
					&& (previousResult != Result.FAILURE || notifier.getNotifyRepeatedFailure())
					&& notifier.getNotifyFailure())
				|| (result == Result.NOT_BUILT && notifier.getNotifyNotBuilt())
				|| (result == Result.SUCCESS
					&& (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
					&& notifier.getNotifyBackToNormal())
				|| (result == Result.SUCCESS && notifier.getNotifySuccess())
				|| (result == Result.UNSTABLE && notifier.getNotifyUnstable())) {
			getMattermost(r).publish(getBuildStatusMessage(r, notifier.includeTestSummary(),
						notifier.includeCustomMessage()), getBuildColor(r));
			if (notifier.getShowCommitList()) {
				getMattermost(r).publish(getCommitList(r), getBuildColor(r));
			}
				}
	}

	String getChanges(AbstractBuild r, boolean includeCustomMessage) {
		if (!r.hasChangeSetComputed()) {
			logger.info("No change set computed...");
			return null;
		}
		ChangeLogSet changeSet = r.getChangeSet();
		List<Entry> entries = new LinkedList<Entry>();
		Set<AffectedFile> files = new HashSet<AffectedFile>();
		for (Object o : changeSet.getItems()) {
			Entry entry = (Entry) o;
			logger.info("Entry " + o);
			entries.add(entry);
			files.addAll(entry.getAffectedFiles());
		}
		if (entries.isEmpty()) {
			logger.info("Empty change...");
			return null;
		}
		Set<String> authors = new HashSet<String>();
		for (Entry entry : entries) {
			authors.add(entry.getAuthor().getDisplayName());
		}
		MessageBuilder message = new MessageBuilder(notifier, r);
		message.append(":pray: Started by changes from ");
		message.append(StringUtils.join(authors, ", "));
		message.append(" (");
		message.append(files.size());
		message.append(" file(s) changed)");
		message.appendOpenLink();
		if (includeCustomMessage) {
			message.appendCustomMessage();
		}
		return message.toString();
	}

	String getCommitList(AbstractBuild r) {
		ChangeLogSet changeSet = r.getChangeSet();
		List<Entry> entries = new LinkedList<Entry>();
		for (Object o : changeSet.getItems()) {
			Entry entry = (Entry) o;
			logger.info("Entry " + o);
			entries.add(entry);
		}
		if (entries.isEmpty()) {
			logger.info("Empty change...");
			Cause.UpstreamCause c = (Cause.UpstreamCause)r.getCause(Cause.UpstreamCause.class);
			if (c == null) {
				return "No Changes.";
			}
			String upProjectName = c.getUpstreamProject();
			int buildNumber = c.getUpstreamBuild();
			AbstractProject project = Hudson.getInstance().getItemByFullName(upProjectName, AbstractProject.class);
			AbstractBuild upBuild = (AbstractBuild)project.getBuildByNumber(buildNumber);
			return getCommitList(upBuild);
		}
		Set<String> commits = new HashSet<String>();
		for (Entry entry : entries) {
			StringBuffer commit = new StringBuffer();
			commit.append(entry.getMsg());
			commit.append(" [").append(entry.getAuthor().getDisplayName()).append("]");
			commits.add(commit.toString());
		}
		MessageBuilder message = new MessageBuilder(notifier, r);
		message.append("Changes:\n- ");
		message.append(StringUtils.join(commits, "\n- "));
		return message.toString();
	}

	static String getBuildColor(AbstractBuild r) {
		Result result = r.getResult();
		if (result == Result.SUCCESS) {
			return "good";
		} else if (result == Result.FAILURE) {
			return "danger";
		} else {
			return "warning";
		}
	}

	String getBuildStatusMessage(AbstractBuild r, boolean includeTestSummary, boolean includeCustomMessage) {
		MessageBuilder message = new MessageBuilder(notifier, r);
		message.appendStatusMessage();
		message.appendDuration();
		message.appendOpenLink();
		if (includeTestSummary) {
			message.appendTestSummary();
		}
		if (includeCustomMessage) {
			message.appendCustomMessage();
		}
		return message.toString();
	}

	public static class MessageBuilder {

		private StringBuffer message;
		private MattermostNotifier notifier;
		private AbstractBuild build;

		public MessageBuilder(MattermostNotifier notifier, AbstractBuild build) {
			this.notifier = notifier;
			this.message = new StringBuffer();
			this.build = build;
			startMessage();
		}

		public MessageBuilder appendStatusMessage() {
			message.append(this.escape(getStatusMessage(build)));
			return this;
		}

		static String getStatusMessage(AbstractBuild r) {
			if (r.isBuilding()) {
				return ":pray: Starting...";
			}
			Result result = r.getResult();
			Run previousBuild = r.getProject().getLastBuild().getPreviousBuild();
			Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
			if (result == Result.SUCCESS && previousResult == Result.FAILURE) {
				return ":white_check_mark: Back to normal";
			}
			if (result == Result.FAILURE && previousResult == Result.FAILURE) {
				return ":no_entry_sign: Still Failing";
			}
			if (result == Result.SUCCESS) {
				return ":white_check_mark: Success";
			}
			if (result == Result.FAILURE) {
				return ":no_entry_sign: Failure";
			}
			if (result == Result.ABORTED) {
				return ":warning: Aborted";
			}
			if (result == Result.NOT_BUILT) {
				return ":warning: Not built";
			}
			if (result == Result.UNSTABLE) {
				return ":warning: Unstable";
			}
			return ":question: Unknown";
		}

		public MessageBuilder append(String string) {
			message.append(this.escape(string));
			return this;
		}

		public MessageBuilder append(Object string) {
			message.append(this.escape(string.toString()));
			return this;
		}

		private MessageBuilder startMessage() {
			message.append(this.escape(build.getProject().getFullDisplayName()));
			message.append(" - ");
			message.append(this.escape(build.getDisplayName()));
			message.append(" ");
			return this;
		}

		public MessageBuilder appendOpenLink() {
			String url = notifier.getBuildServerUrl() + build.getUrl();
			message.append(" [Open](").append(url).append(")");
			return this;
		}

		public MessageBuilder appendDuration() {
			message.append(" after ");
			message.append(build.getDurationString());
			return this;
		}

		public MessageBuilder appendTestSummary() {
			AbstractTestResultAction<?> action = this.build
				.getAction(AbstractTestResultAction.class);
			if (action != null) {
				int total = action.getTotalCount();
				int failed = action.getFailCount();
				int skipped = action.getSkipCount();
				message.append("\nTest Status:\n");
				message.append("\tPassed: " + (total - failed - skipped));
				message.append(", Failed: " + failed);
				message.append(", Skipped: " + skipped);
			} else {
				message.append("\nNo Tests found.");
			}
			return this;
		}

		public MessageBuilder appendCustomMessage() {
			String customMessage = notifier.getCustomMessage();
			EnvVars envVars = new EnvVars();
			try {
				envVars = build.getEnvironment(new LogTaskListener(logger, INFO));
			} catch (IOException e) {
				logger.log(SEVERE, e.getMessage(), e);
			} catch (InterruptedException e) {
				logger.log(SEVERE, e.getMessage(), e);
			}
			message.append("\n");
			message.append(envVars.expand(customMessage));
			return this;
		}

		public String escape(String string) {
			string = string.replace("&", "&amp;");
			string = string.replace("<", "&lt;");
			string = string.replace(">", "&gt;");

			return string;
		}

		public String toString() {
			return message.toString();
		}
	}
}
