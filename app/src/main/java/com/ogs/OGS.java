package com.ogs;

import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

public class OGS {
    private String getURL(String url, String auth)
    {
        try {
            String httpsURL = url;
            URL myurl = new URL(httpsURL);
            HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
            if (auth.length() > 0)
                con.setRequestProperty("Authorization", "Bearer " + auth);
            InputStream ins = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);

            String inputLine;
            StringBuilder sb = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }

            System.out.println(sb.toString());
            JSONObject obj = new JSONObject(sb.toString());
            System.out.println(obj.toString(2));

            in.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String postURL(String url, String auth, String body)
    {
        try {
            String httpsURL = url;
            URL myurl = new URL(httpsURL);
            HttpsURLConnection con = (HttpsURLConnection)myurl.openConnection();
            if (auth.length() > 0)
                con.setRequestProperty("Authorization", "Bearer " + auth);

            con.setRequestMethod("POST");
            con.setDoOutput(true);

            System.out.println("body: " + body);

            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(
                        con.getOutputStream()));

            out.write(body);
            out.close();

            InputStream ins = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader in = new BufferedReader(isr);

            String inputLine;
            StringBuilder sb = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }

            System.out.println(sb.toString());
            JSONObject obj = new JSONObject(sb.toString());
            System.out.println(obj.toString(2));

            in.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public OGS(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void login(String username, String password)
    {
        String body = String.format("client_id=%s&client_secret=%s&grant_type=password&username=%s&password=%s",
                clientId, clientSecret, username, password); // TODO url encode
        String s = postURL("https://online-go.com/oauth2/access_token", "", body);
        try {
            JSONObject obj = new JSONObject(s);
            accessToken = obj.getString("access_token");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("access token = " + accessToken);
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

    public String me() {
        return getURL("https://online-go.com/api/v1/me/?format=json",
                accessToken);
    }

    private String clientId;
    private String clientSecret;
    private String accessToken;
}
