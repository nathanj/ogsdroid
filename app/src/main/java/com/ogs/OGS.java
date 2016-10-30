package com.ogs;

import android.util.Log;

import java.net.URISyntaxException;
import java.net.URL;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class OGS {
    private String getURL(String url) {
        try {
            Log.e("myApp", String.format("Getting %s", url));
            String httpsURL = url;
            URL myurl = new URL(httpsURL);
            HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
            if (accessToken.length() > 0)
                con.setRequestProperty("Authorization", "Bearer " + accessToken);
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

    private String postURL(String url, String auth, String body) {
        try {
            Log.e("myApp", String.format("Posting to %s with body=%s", url, body));
            String httpsURL = url;
            URL myurl = new URL(httpsURL);
            HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
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

    public void login(String username, String password) {
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

    public JSONObject me() throws JSONException {
        String str = getURL("https://online-go.com/api/v1/me/?format=json");
        JSONObject obj = new JSONObject(str);
        userId = obj.getInt("id");
        username = obj.getString("username");
        ranking = obj.getInt("ranking");
        return obj;
    }

    public JSONObject listServerChallenges() throws JSONException {
        String str = getURL("https://online-go.com/api/v1/challenges/?format=json");
        return new JSONObject(str);
    }

    public JSONObject listGames() throws JSONException {
        String str = getURL("https://online-go.com/api/v1/me/games/?started__isnull=False&ended__isnull=True&format=json");
        Log.w("myApp", str);
        return new JSONObject(str);
    }

    public JSONObject getGameDetails(int id) throws JSONException {
        String str = getURL("https://online-go.com/api/v1/games/" + id + "?format=json");
        Log.w("myApp", str);
        return new JSONObject(str);
    }

    public JSONObject gameMove(int id, String move) throws JSONException {
        Log.w("myApp", "doing game move " + move);
        String str = postURL("https://online-go.com/api/v1/games/" + id + "/move/?format=json", accessToken,
                "{\"move\": \"" + "aa" + "\"}");
        Log.w("myApp", str);
        return new JSONObject(str);
    }

    /** Opens the real time api socket. */
    public void openSocket() {
	    try {
		    socket = IO.socket("https://ggs.online-go.com");
	    } catch (URISyntaxException e) {
		    e.printStackTrace();
		    return;
	    }
	    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

		    @Override
		    public void call(Object... args) {
			    Log.w("myApp", "socket connect");
		    }

	    }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

		    @Override
		    public void call(Object... args) {
			    Log.w("myApp", "socket disconnect");
		    }

	    });
	    socket.connect();
    }

    /** Uses the real time api to connect to a game. */
    public OGSGameConnection openGameConnection(int gameId) {
	    return new OGSGameConnection(this, socket, gameId, userId);
    }

    private String clientId;
    private String clientSecret;
    private String accessToken;
    private String username;
    private int userId;
    private int ranking;
    private Socket socket;
}
