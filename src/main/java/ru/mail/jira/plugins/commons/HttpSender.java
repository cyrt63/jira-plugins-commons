package ru.mail.jira.plugins.commons;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration")
public class HttpSender {
    private final String url;
    private String user;
    private String password;
    private final Map<String, String> headers = new HashMap<String, String>();

    public HttpSender(String url, Object... params) {
        try {
            for (int i = 0; i < params.length; i++)
                params[i] = URLEncoder.encode(params[i].toString(), "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        this.url = String.format(url, params);
    }

    public HttpSender setAuthenticationInfo(String user, String password) {
        this.user = user;
        this.password = password;
        return this;
    }

    public HttpSender setHeader(String header, String value) {
        headers.put(header, value);
        return this;
    }

    public HttpSender setContentTypeJson() {
        setHeader("Content-Type", "application/json; charset=utf-8");
        return this;
    }

    private String getAuthRealm() {
        return DatatypeConverter.printBase64Binary(user.concat(":").concat(password).getBytes());
    }

    private String send(String method, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setDoInput(true);
            connection.setDoOutput(StringUtils.isNotEmpty(body));
            connection.setAllowUserInteraction(true);
            connection.setRequestMethod(method);
            if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password))
                connection.setRequestProperty("Authorization", "Basic " + getAuthRealm());
            for (Map.Entry<String, String> entry : headers.entrySet())
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            if (StringUtils.isNotEmpty(body))
                IOUtils.write(body, connection.getOutputStream());

            int rc = connection.getResponseCode();
            if (rc == HttpURLConnection.HTTP_OK)
                return IOUtils.toString(connection.getInputStream(), "UTF-8");
            else
                throw new HttpSenderException(rc, body, connection.getErrorStream() != null ? IOUtils.toString(connection.getErrorStream(), "UTF-8") : "");
        } finally {
            connection.disconnect();
        }
    }

    public String sendGet() throws IOException {
        return send("GET", null);
    }

    public String sendGet(String body) throws IOException {
        return send("GET", body);
    }

    public String sendPost(String body) throws IOException {
        return send("POST", body);
    }
}
