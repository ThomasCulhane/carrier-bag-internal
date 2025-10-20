import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api", new HttpHandler() {
            public void handle(HttpExchange t) {
                try {
                    String response = "Hello from Java";
                    t.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                    t.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                    OutputStream os = t.getResponseBody();
                    os.write(response.getBytes("UTF-8"));
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        server.setExecutor(null);
        System.out.println("Java app listening on 8080");
        server.start();
    }
}