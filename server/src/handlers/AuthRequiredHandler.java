package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthRequiredHandler implements HttpHandler {
    private final HttpHandler next;
    private final JwtParser parser;
    private final boolean requiresAdmin;

    public AuthRequiredHandler(HttpHandler next, boolean requiresAdmin) {
        this.next = next;
        String secret = env("JWT_SECRET", "dev-secret-please-change-and-use-32-bytes!!");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.parser = Jwts.parser()
                .verifyWith(key)
                .build();
        this.requiresAdmin = requiresAdmin;
    }

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v != null ? v : d;
    }

    private void send_unauthorized(HttpExchange ex, String msg) {
        try {
            byte[] b = ("{\"error\":\"unauthorized\",\"message\":\"" + msg + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            ex.getResponseHeaders().add("WWW-Authenticate", "Bearer realm=\"api\"");
            ex.sendResponseHeaders(401, b.length);
            OutputStream os = ex.getResponseBody();
            os.write(b);
            os.close();
        } catch (Exception ignore) { }
    }

    @Override
    public void handle(HttpExchange ex) {
        try {
            List<String> auth = ex.getRequestHeaders().get("Authorization");
            if (auth == null || auth.isEmpty()) {
                send_unauthorized(ex, "missing Authorization header");
                return;
            }
            String header = auth.get(0);
            if (!header.startsWith("Bearer ")) {
                send_unauthorized(ex, "invalid Authorization scheme");
                return;
            }
            String token = header.substring("Bearer ".length()).trim();
            Claims claims = this.parser
                    .parseSignedClaims(token)
                    .getPayload();
            

            if(this.requiresAdmin && claims.getSubject() != "admin") {
                send_unauthorized(ex, "admin only endpoint");
                return;
            }

            // expose claims to downstream handlers
            ex.setAttribute("jwtClaims", claims);

            // forward to the wrapped handler
            next.handle(ex);
        } catch (JwtException je) {
            send_unauthorized(ex, "token invalid or expired");
        } catch (Exception e) {
            send_unauthorized(ex, "auth failure");
        }
    }
}