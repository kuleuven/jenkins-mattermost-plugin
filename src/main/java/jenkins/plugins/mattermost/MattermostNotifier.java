package jenkins.plugins.mattermost;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class MattermostNotifier extends Notifier {

    private static final Logger logger = Logger.getLogger(MattermostNotifier.class.getName());

    private String host;
    private String authToken;
    private String buildServerUrl;
    private String room;
    private String sendAs;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;
    private boolean includeTestSummary;
    private boolean showCommitList;
    private boolean includeCustomMessage;
    private String customMessage;

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getHost() {
        return host;
    }

    public String getRoom() {
        return room;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getBuildServerUrl() {
        if(buildServerUrl == null || buildServerUrl == "") {
            JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
            return jenkinsConfig.getUrl();
        }
        else {
            return buildServerUrl;
        }
    }

    public String getSendAs() {
        return sendAs;
    }

    public boolean getStartNotification() {
        return startNotification;
    }

    public boolean getNotifySuccess() {
        return notifySuccess;
    }

    public boolean getShowCommitList() {
        return showCommitList;
    }

    public boolean getNotifyAborted() {
        return notifyAborted;
    }

    public boolean getNotifyFailure() {
        return notifyFailure;
    }

    public boolean getNotifyNotBuilt() {
        return notifyNotBuilt;
    }

    public boolean getNotifyUnstable() {
        return notifyUnstable;
    }

    public boolean getNotifyBackToNormal() {
        return notifyBackToNormal;
    }

    public boolean includeTestSummary() {
        return includeTestSummary;
    }

    public boolean getNotifyRepeatedFailure() {
        return notifyRepeatedFailure;
    }

    public boolean includeCustomMessage() {
        return includeCustomMessage;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    @DataBoundConstructor
    public MattermostNotifier(final String host, final String authToken, final String room, final String buildServerUrl,
                         final String sendAs, final boolean startNotification, final boolean notifyAborted, final boolean notifyFailure,
                         final boolean notifyNotBuilt, final boolean notifySuccess, final boolean notifyUnstable, final boolean notifyBackToNormal,
                         final boolean notifyRepeatedFailure, final boolean includeTestSummary, final boolean showCommitList,
                         boolean includeCustomMessage, String customMessage) {
        super();
        this.host = host;
        this.authToken = authToken;
        this.buildServerUrl = buildServerUrl;
        this.room = room;
        this.sendAs = sendAs;
        this.startNotification = startNotification;
        this.notifyAborted = notifyAborted;
        this.notifyFailure = notifyFailure;
        this.notifyNotBuilt = notifyNotBuilt;
        this.notifySuccess = notifySuccess;
        this.notifyUnstable = notifyUnstable;
        this.notifyBackToNormal = notifyBackToNormal;
        this.notifyRepeatedFailure = notifyRepeatedFailure;
        this.includeTestSummary = includeTestSummary;
        this.showCommitList = showCommitList;
        this.includeCustomMessage = includeCustomMessage;
        this.customMessage = customMessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public MattermostService newMattermostService(AbstractBuild r, BuildListener listener) {
        String host = this.host;
        if (StringUtils.isEmpty(host)) {
            host = getDescriptor().getHost();
        }
        String authToken = this.authToken;
        if (StringUtils.isEmpty(authToken)) {
            authToken = getDescriptor().getToken();
        }
        String room = this.room;
        if (StringUtils.isEmpty(room)) {
            room = getDescriptor().getRoom();
        }

        EnvVars env = null;
        try {
            env = r.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
            env = new EnvVars();
        }
        host = env.expand(host);
        authToken = env.expand(authToken);
        room = env.expand(room);

        return new StandardMattermostService(host, authToken, room);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (startNotification) {
            Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
            for (Publisher publisher : map.values()) {
                if (publisher instanceof MattermostNotifier) {
                    logger.info("Invoking Started...");
                    new ActiveNotifier((MattermostNotifier) publisher, listener).started(build);
                }
            }
        }
        return super.prebuild(build, listener);
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String host;
        private String token;
        private String room;
        private String buildServerUrl;
        private String sendAs;

        public DescriptorImpl() {
            load();
        }

        public String getHost() {
            return host;
        }

        public String getToken() {
            return token;
        }

        public String getRoom() {
            return room;
        }

        public String getBuildServerUrl() {
            if(buildServerUrl == null || buildServerUrl == "") {
                JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
                return jenkinsConfig.getUrl();
            }
            else {
                return buildServerUrl;
            }
        }

        public String getSendAs() {
            return sendAs;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public MattermostNotifier newInstance(StaplerRequest sr, JSONObject json) {
            String host = sr.getParameter("mattermostHost");
            String token = sr.getParameter("mattermostToken");
            String room = sr.getParameter("mattermostRoom");
            boolean startNotification = "true".equals(sr.getParameter("mattermostStartNotification"));
            boolean notifySuccess = "true".equals(sr.getParameter("mattermostNotifySuccess"));
            boolean notifyAborted = "true".equals(sr.getParameter("mattermostNotifyAborted"));
            boolean notifyNotBuilt = "true".equals(sr.getParameter("mattermostNotifyNotBuilt"));
            boolean notifyUnstable = "true".equals(sr.getParameter("mattermostNotifyUnstable"));
            boolean notifyFailure = "true".equals(sr.getParameter("mattermostNotifyFailure"));
            boolean notifyBackToNormal = "true".equals(sr.getParameter("mattermostNotifyBackToNormal"));
            boolean notifyRepeatedFailure = "true".equals(sr.getParameter("mattermostNotifyRepeatedFailure"));
            boolean includeTestSummary = "true".equals(sr.getParameter("includeTestSummary"));
            boolean showCommitList = "true".equals(sr.getParameter("mattermostShowCommitList"));
            boolean includeCustomMessage = "on".equals(sr.getParameter("includeCustomMessage"));
            String customMessage = sr.getParameter("customMessage");
            return new MattermostNotifier(host, token, room, buildServerUrl, sendAs, startNotification, notifyAborted,
                    notifyFailure, notifyNotBuilt, notifySuccess, notifyUnstable, notifyBackToNormal, notifyRepeatedFailure,
                    includeTestSummary, showCommitList, includeCustomMessage, customMessage);
        }

        @Override
        public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
            host = sr.getParameter("mattermostHost");
            token = sr.getParameter("mattermostToken");
            room = sr.getParameter("mattermostRoom");
            buildServerUrl = sr.getParameter("mattermostBuildServerUrl");
            sendAs = sr.getParameter("mattermostSendAs");
            if(buildServerUrl == null || buildServerUrl == "") {
                JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
                buildServerUrl = jenkinsConfig.getUrl();
            }
            if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
                buildServerUrl = buildServerUrl + "/";
            }
            save();
            return super.configure(sr, formData);
        }

        MattermostService getMattermostService(final String host, final String authToken, final String room) {
            return new StandardMattermostService(host, authToken, room);
        }

        @Override
        public String getDisplayName() {
            return "Mattermost Notifications";
        }

        public FormValidation doTestConnection(@QueryParameter("mattermostHost") final String host,
                                               @QueryParameter("mattermostToken") final String authToken,
                                               @QueryParameter("mattermostRoom") final String room,
                                               @QueryParameter("mattermostBuildServerUrl") final String buildServerUrl) throws FormException {
            try {
                String targetDomain = host;
                if (StringUtils.isEmpty(targetDomain)) {
                    targetDomain = this.host;
                }
                String targetToken = authToken;
                if (StringUtils.isEmpty(targetToken)) {
                    targetToken = this.token;
                }
                String targetRoom = room;
                if (StringUtils.isEmpty(targetRoom)) {
                    targetRoom = this.room;
                }
                String targetBuildServerUrl = buildServerUrl;
                if (StringUtils.isEmpty(targetBuildServerUrl)) {
                    targetBuildServerUrl = this.buildServerUrl;
                }
                MattermostService testMattermostService = getMattermostService(targetDomain, targetToken, targetRoom);
                String message = "Mattermost/Jenkins plugin: you're all set on " + targetBuildServerUrl;
                boolean success = testMattermostService.publish(message, "good");
                return success ? FormValidation.ok("Success") : FormValidation.error("Failure");
            } catch (Exception e) {
                return FormValidation.error("Client error : " + e.getMessage());
            }
        }
    }
}
