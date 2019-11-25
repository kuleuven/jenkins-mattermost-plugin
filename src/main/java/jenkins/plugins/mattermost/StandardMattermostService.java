package jenkins.plugins.mattermost;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StandardMattermostService implements MattermostService {

  private static final Logger logger = Logger.getLogger(StandardMattermostService.class.getName());

  private String endpoint;
  private String[] roomIds;
  private String icon;

  public StandardMattermostService(String endpoint, String roomId, String icon) {
    super();
    this.endpoint = endpoint;
    this.roomIds = roomId.split("[,;]+");
    this.icon = icon;
  }

  public boolean publish(String message) {
    return publish(message, "warning");
  }

  public boolean publish(String message, String color) {
    return publish(message, "", color);
  }

  public boolean publish(String message, String text, String color) {
    boolean result = true;
    for (String userAndRoomId : roomIds) {
      String url = endpoint;
      String roomId = userAndRoomId.trim();
      String userId = "jenkins";
      // Supported channel string formats:
      // - user@channel
      // - user@@dmchannel
      // - channel
      // - @dmchannel
      int atPos = userAndRoomId.indexOf("@");
      if (atPos > 0 && atPos < userAndRoomId.length() - 1) {
        userId = userAndRoomId.substring(0, atPos).trim();
        roomId = userAndRoomId.substring(atPos + 1).trim();
      }

      String roomIdString = roomId;

      if (StringUtils.isEmpty(roomIdString)) {
        roomIdString = "(default)";
      }

      logger.info("Posting: to " + roomIdString + "@" + url + ": " + message + " (" + color + ")");
      HttpClient client = getHttpClient();
      PostMethod post = new PostMethod(url);
      JSONObject json = new JSONObject();

      try {
        JSONObject field = new JSONObject();
        field.put("short", false);
        field.put("value", message);
        JSONArray fields = new JSONArray();
        fields.put(field);

        JSONObject attachment = new JSONObject();
        attachment.put("fallback", message);
        attachment.put("color", color);
        attachment.put("fields", fields);
        JSONArray mrkdwn = new JSONArray();
        mrkdwn.put("pretext");
        mrkdwn.put("text");
        mrkdwn.put("fields");
        attachment.put("mrkdwn_in", mrkdwn);
        JSONArray attachments = new JSONArray();
        attachments.put(attachment);
        json.put("text", text);
        json.put("attachments", attachments);

        if (!roomId.isEmpty()) json.put("channel", roomId);
        json.put("username", userId);
        json.put("icon_url", icon);

        post.addParameter("payload", json.toString());
        post.getParams().setContentCharset("UTF-8");
        int responseCode = client.executeMethod(post);
        String response = post.getResponseBodyAsString();
        if (responseCode != HttpStatus.SC_OK) {
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
		  if (isProxyRequired(proxy.noProxyHost))
		  {
          client.getHostConfiguration().setProxy(proxy.name, proxy.port);
          String username = proxy.getUserName();
          String password = proxy.getPassword();
          // Consider it to be passed if username specified. Sufficient?
          if (username != null && !"".equals(username.trim())) {
            logger.info("Using proxy authentication (user=" + username + ")");
            // http://hc.apache.org/httpclient-3.x/authentication.html#Proxy_Authentication
            // and
            // http://svn.apache.org/viewvc/httpcomponents/oac.hc3x/trunk/src/examples/BasicAuthenticationExample.java?view=markup
            client
                .getState()
                .setProxyCredentials(
                    AuthScope.ANY, new UsernamePasswordCredentials(username, password));
          }
        }
      }
    }
    return client;
  }

  protected boolean isProxyRequired(List<Pattern> noProxyHosts) {
    try {
      URL url = new URL(endpoint);
      for (Pattern p : noProxyHosts) {
        if (p.matcher(url.getHost()).matches()) return false;
      }
    } catch (MalformedURLException e) {
      logger.log(
          Level.WARNING,
          "A malformed URL [" + endpoint + "] is defined as endpoint, please check your settings");
      // default behavior : proxy still activated
      return true;
    }
    return true;
  }

	protected boolean isProxyRequired(String... noProxyHost)
	{
		List<String> lst = Arrays.asList(noProxyHost);
		List<Pattern> collect = lst.stream().map(Pattern::compile).collect(Collectors.toList());
		return isProxyRequired(collect);
	}


  void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
