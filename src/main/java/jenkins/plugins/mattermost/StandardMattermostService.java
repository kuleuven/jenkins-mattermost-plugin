package jenkins.plugins.mattermost;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import hudson.ProxyConfiguration;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

public class StandardMattermostService implements MattermostService {

    private static final Logger logger = Logger.getLogger(StandardMattermostService.class.getName());

    private String endpoint;
    private String[] roomIds;
    private String icon;

    public StandardMattermostService(String endpoint, String roomId, String icon) {
        super();
        this.endpoint = endpoint;
        this.roomIds = roomId.split("[,; ]+");
        this.icon = icon;
    }

    public boolean publish(String message) {
        return publish(message, "warning");
    }

    public boolean publish(String message, String color) {
        boolean result = true;
        for (String roomId : roomIds) {
            String url = endpoint;
            logger.info("Posting: to " + roomId + " on " + " using " + url +": " + message + " " + color);
            HttpClient client = getHttpClient();
            PostMethod post = new PostMethod(url);
            JSONObject json = new JSONObject();

            try {
                json.put("channel", roomId);
                json.put("text", message);
                json.put("username", "jenkins");
                json.put("icon_url", icon);

                post.addParameter("payload", json.toString());
                post.getParams().setContentCharset("UTF-8");
                int responseCode = client.executeMethod(post);
                String response = post.getResponseBodyAsString();
                if(responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Mattermost post may have failed. Response: " + response);
                    result = false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Mattermost", e);
                result = false;
            } finally {
                logger.info("Posting succeeded");
                post.releaseConnection();
            }
        }
        return result;
    }

    protected HttpClient getHttpClient() {
        HttpClient client = new HttpClient();
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                client.getHostConfiguration().setProxy(proxy.name, proxy.port);
                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
                    // and
                    // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
                    client.getState().setProxyCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return client;
    }

    void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
