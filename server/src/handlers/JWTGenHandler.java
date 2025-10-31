package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JWTGenHandler implements HttpHandler {

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v != null ? v : d;
    }

    private final SecretKey key;

    public JWTGenHandler() {
        // ensure at least 32 bytes for HMAC-SHA256
        String secret = env("JWT_SECRET", "dev-secret-please-change-and-use-32-bytes!!");
        // ensure proper length (hmacShaKeyFor will validate)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    private void send_failure(HttpExchange ex, String message) {
        try {
            byte[] err = ("{\"error\":\"token_generation_failed\",\"message\":\"" + message + "\"}")
                                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(500, err.length);
            OutputStream os = ex.getResponseBody();
            os.write(err);
            os.close();
        } catch (Exception ignore) { }
    }

    private Connection getConnection() throws SQLException {
        String host = env("POSTGRES_HOST", "localhost");
        String port = env("POSTGRES_PORT", "5432");
        String db = env("POSTGRES_DB", "default_name");
        String user = env("POSTGRES_USER", "default_user");
        String pass = env("POSTGRES_PASSWORD", "default_pass");
        String url = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
        return DriverManager.getConnection(url, user, pass);
    }

    public String getUserHash(String username) throws SQLException {
        String sql = "SELECT password_hash FROM authentication WHERE username = ?";
        try (PreparedStatement ps = this.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                } else {
                    throw new SQLException("User " + username + " does not exist.");
                }
            }
        }
    }

    @Override
    public void handle(HttpExchange ex) {
        try (InputStream is = ex.getRequestBody()) {
            ObjectMapper OM = new ObjectMapper();
            Map<String, String> body = OM.readValue(is, new TypeReference<Map<String, String>>() {});

            if(body == null || body.get("username") == null || body.get("password") == null) {
                throw new IllegalArgumentException("Must include username and password");
            }

            String username = body.get("username");
            String pw = body.get("password");

            String hash = getUserHash(username); // Throws SQLException if not exists
            if(!BCrypt.checkpw(pw, hash)) {
                throw new IllegalArgumentException("Password is incorrect for the given user.");
            }

            long now = System.currentTimeMillis();
            Date iat = new Date(now);
            // default token lifetime: 1 hour (configurable via env)
            long ttlMs = Long.parseLong(env("JWT_TTL_MS", String.valueOf(60 * 60 * 1000)));
            Date exp = new Date(now + ttlMs);

            String jwt = Jwts.builder()
                    .subject(username)
                    .issuedAt(iat)
                    .expiration(exp)
                    .signWith(key)
                    .compact();

            Map<String, Object> resp = Map.of(
                    "token", jwt,
                    "expiresAt", exp.getTime(),
                    "sub", username
            );

            byte[] bytes = OM.writeValueAsBytes(resp);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            send_failure(ex, "Must include username and password.");
        } catch (JwtException e) {
            send_failure(ex, "JWT Exception: " + e.getMessage());
        } catch (Exception e) {
            send_failure(ex, e.getMessage());
        }
    }
}