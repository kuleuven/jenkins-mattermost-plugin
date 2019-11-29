package jenkins.plugins.mattermost;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StandardMattermostService implements MattermostService
{

	private static final Logger logger = Logger.getLogger(StandardMattermostService.class.getName());

	private String endpoint;
	private String[] channelIds;
	private String icon;

	public StandardMattermostService(String endpoint, String channelId, String icon)
	{
		super();
		this.endpoint = endpoint;
		this.channelIds = channelId.split("[,;]+");
		this.icon = icon;
	}

	private static JSONObject createPayload(String message, String text, String color, String roomId, String userId, String icon)
	{
		JSONObject json = new JSONObject();
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
		return json;
	}

	public static String createRegexFromGlob(String glob)
	{
		String out = "^";
		for (int i = 0; i < glob.length(); ++i)
		{
			final char c = glob.charAt(i);
			switch (c)
			{
				case '*':
					out += ".*";
					break;
				case '?':
					out += '.';
					break;
				case '.':
					out += "\\.";
					break;
				case '\\':
					out += "\\\\";
					break;
				default:
					out += c;
			}
		}
		out += '$';
		return out;
	}

	public boolean publish(String message)
	{
		return publish(message, "warning");
	}

	public boolean publish(String message, String color)
	{
		return publish(message, "", color);
	}

	public boolean publish(String message, String text, String color)
	{
		boolean result = true;
		for (String userAndRoomId : channelIds)
		{
			//String url = endpoint;
			URL url = null;
			try
			{
				url = new URL(this.endpoint);

				String roomId = userAndRoomId.trim();
				String userId = "jenkins";
				HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
				ProxyConfiguration proxy = Jenkins.get().proxy;
				HttpClientBuilder clientBuilder = HttpClients.custom();
				RequestConfig.Builder reqconfigconbuilder = RequestConfig.custom();
				if (proxy != null && isProxyRequired(proxy.noProxyHost))
				{//TODO CHECK PROXY URL
					URL proxyURL = new URL(proxy.name + ":" + proxy.port);
					HttpHost proxyHost = new HttpHost(proxyURL.getHost(), proxyURL.getPort());
					DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
					clientBuilder.setRoutePlanner(routePlanner);
					reqconfigconbuilder = reqconfigconbuilder.setProxy(proxyHost);

					String username = proxy.getUserName();
					String password = proxy.getPassword();
					// Consider it to be passed if username specified. Sufficient?
					if (username != null && !username.isEmpty())
					{
						logger.info("Using proxy authentication (user=" + username + ")");
						BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
						basicCredentialsProvider.setCredentials(
								new org.apache.http.auth.AuthScope(proxyHost.getHostName(), proxy.port),
								new org.apache.http.auth.UsernamePasswordCredentials(username, password));

						clientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
					}
				}

				RequestConfig config = reqconfigconbuilder.build();
				CloseableHttpClient client = clientBuilder.build();

				// Supported channel string formats:
				// - user@channel
				// - user@@dmchannel
				// - channel
				// - @dmchannel
				int atPos = userAndRoomId.indexOf("@");
				if (atPos > 0 && atPos < userAndRoomId.length() - 1)
				{
					userId = userAndRoomId.substring(0, atPos).trim();
					roomId = userAndRoomId.substring(atPos + 1).trim();
				}

				String roomIdString = roomId;
				if (roomIdString.isEmpty())
				{
					roomIdString = "(default)";
				}

				logger.info("Posting: to " + roomIdString + "@" + url + ": " + message + " (" + color + ")");
				RequestBuilder requestBuilder = RequestBuilder.post(url.toURI());
				requestBuilder.setConfig(config);


				JSONObject json = createPayload(message, text, color, roomId, userId, icon);

				requestBuilder.addParameter("payload", json.toString());
				requestBuilder.setCharset(StandardCharsets.UTF_8);
				CloseableHttpResponse execute = client.execute(httpHost, requestBuilder.build());
				int responseCode = execute.getStatusLine().getStatusCode();
				if (responseCode != HttpStatus.SC_OK)
				{
					Scanner sc = new Scanner(execute.getEntity().getContent());
					String response = sc.toString();
					logger.log(Level.WARNING, "Mattermost post may have failed. Response: " + response);
					result = false;
				}
			} catch (Exception e)
			{
				logger.log(Level.WARNING, "Error posting to Mattermost", e);
				result = false;
			}
		}
		return result;
	}

	protected boolean isProxyRequired(List<Pattern> noProxyHosts)
	{
		try
		{
			URL url = new URL(endpoint);
			for (Pattern p : noProxyHosts)
			{
				if (p.matcher(url.getHost()).matches()) return false;
			}
		} catch (MalformedURLException e)
		{
			logger.log(
					Level.WARNING,
					"A malformed URL [" + endpoint + "] is defined as endpoint, please check your settings");
			// default behavior : proxy still activated
			return true;
		}
		return true;
	}

	protected boolean isProxyRequired(String... noProxyHost)
	{//
		List<String> lst = Arrays.asList(noProxyHost);
		List<Pattern> collect = lst.stream()
				.map(StandardMattermostService::createRegexFromGlob)
				.map(Pattern::compile)
				.collect(Collectors.toList());
		return isProxyRequired(collect);
	}

	void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}
}