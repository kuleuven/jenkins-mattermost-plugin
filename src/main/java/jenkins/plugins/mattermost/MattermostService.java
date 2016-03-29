package jenkins.plugins.mattermost;

public interface MattermostService {
    boolean publish(String message);

    boolean publish(String message, String color);
}
