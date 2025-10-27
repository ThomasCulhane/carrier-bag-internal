import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import handlers.*;

public class Main {
    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v != null ? v : d;
    }
    static final int PORT = Integer.parseInt(env("PORT", "8080"));

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(Main.PORT), 0);
        server.createContext("/", new RejectHandler()); // Reject all requests that do not match an existing context.
        server.createContext("/api/ping", new PingHandler());
        server.createContext("/api/data", new StaticFileHandler("static/example_data.json"));
        server.createContext("/api/rows", new DBHandler());
        server.setExecutor(null);
        System.out.println("Java app listening on port " + Main.PORT);
        server.start();
    }
}