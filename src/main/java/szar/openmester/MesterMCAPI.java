package szar.openmester;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MesterMCAPI {
    private static final Gson gson = new GsonBuilder().create();

    public static class MesterMCServer {
        @SerializedName("name")
        public String name;
        @SerializedName("ip")
        public String ip;
        @SerializedName("port")
        public int port;
        @SerializedName("acceptTextures")
        public String acceptTextures;
        @SerializedName("icon")
        public Object icon; // this is always null, so idk what it is
    }

    public static class MesterMCServerListResponse {
        @SerializedName("servers")
        public List<MesterMCServer> servers;
    }

    public static class MesterMCLoginResponse {
        public String success;
        public String username;
        public String display_name;
        public String access_token;
        public String session_uuid;
    }


    private static TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {

            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

            public void checkClientTrusted(X509Certificate[] certs, String authType) { /* no-op */ }

            public void checkServerTrusted(X509Certificate[] certs, String authType) { /* no-op */ }
        }
    };

    private static final HostnameVerifier allHostsValid = (hostname, session) -> true;

    public static MesterMCServerListResponse getServers() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create("http://57.128.198.223:25585/api/minecraft/servers").toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.addRequestProperty("User-Agent", "MesterMC-Launcher/2.1");
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("X-Java-Client", "MesterMC-Launcher-ServerList");

        assert conn.getResponseCode() == 200;

        InputStream in = conn.getInputStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                sb.append(line).append('\n');
            }
        }

        return gson.fromJson(sb.toString(), MesterMCServerListResponse.class);
    }

    public static MesterMCLoginResponse login(String username, String password) throws IOException, IllegalAccessException, KeyManagementException, NoSuchAlgorithmException {
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8)).append("&");
        requestBody.append("session_uuid=").append(URLEncoder.encode(
            UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString(),
            StandardCharsets.UTF_8
        )).append("&");
        requestBody.append("username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8));

        HttpsURLConnection conn = (HttpsURLConnection) URI.create("https://api.mestermc.hu:8443/login.php").toURL().openConnection();
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());

        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setHostnameVerifier((h, s) -> true);

        conn.setRequestMethod("POST");
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.addRequestProperty("X-Java-Client", "MesterMC-Launcher");
        conn.addRequestProperty("User-Agent", "MesterMC-Launcher/1.0");
        conn.addRequestProperty("Accept", "*/*");
        conn.setDoOutput(true);

        conn.getOutputStream().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));

        assert conn.getResponseCode() == 200;

        InputStream in = conn.getInputStream();
        HashMap<String, String> responseMapped = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                // what even is this shitty response format
                // Content-Type is "text/html; charset=UTF-8"
                String[] lineSplit = line.split(":");
                System.out.println(line);
                if (lineSplit.length == 2) {
                    responseMapped.put(lineSplit[0], lineSplit[1].substring(1));
                } else {
                    break;
                }
            }
        }

        MesterMCLoginResponse response = new MesterMCLoginResponse();

        for (Field field : response.getClass().getDeclaredFields()) {
            if (responseMapped.containsKey(field.getName())) {
                field.set(response, responseMapped.get(field.getName()));
            }
        }

        return response;
    }

    public static void sendLoginVerification(String username) throws IOException, NoSuchAlgorithmException {
        long timestamp = System.currentTimeMillis();

        String verify = String.format(
            "%s|%s|%s|%s|mestermc_verification_salt_2024",
            OpenMester.LAUNCHER_SECRET,
            OpenMester.LAUNCHER_IDENTIFIER,
            timestamp,
            OpenMester.LAUNCHER_VERSION
        );

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(verify.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) URI.create("http://57.128.198.223:25581/launcher/verify").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.addRequestProperty("Accept", "application/json");
        conn.addRequestProperty("Content-Type", "application/json");
        conn.addRequestProperty("User-Agent", "MesterMC-Auth-Mod/2.0.0");
        conn.addRequestProperty("X-Launcher-Version", OpenMester.LAUNCHER_VERSION);
        conn.addRequestProperty("X-Client-IP", String.format("127.%d.%d.%d", Math.round(Math.random() * 255), Math.round(Math.random() * 255), Math.round(Math.random() * 255)));
        conn.addRequestProperty("X-Player-Name", username);
        conn.setDoOutput(true);

        JsonObject payload = new JsonObject();
        payload.addProperty("messageType", "LAUNCHER_AUTH");
        payload.addProperty("secret", OpenMester.LAUNCHER_SECRET);
        payload.addProperty("identifier", OpenMester.LAUNCHER_IDENTIFIER);
        payload.addProperty("timestamp", timestamp);
        payload.addProperty("serverHash", Base64.getEncoder().encodeToString(digest.digest()));
        payload.addProperty("version", OpenMester.LAUNCHER_VERSION);
        payload.addProperty("playerName", username);

        conn.getOutputStream().write(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));

        assert conn.getResponseCode() == 200;

        InputStream in = conn.getInputStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                sb.append(line).append('\n');
            }
        }

        System.out.println(sb.toString());
    }

}
