package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class StaticFileHandler implements HttpHandler {
    String filePath;

    public StaticFileHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            // Read backend.json from working directory (/app when built)
            Path dataPath = Path.of(this.filePath);
            System.out.println("Reading data from: " + dataPath.toAbsolutePath().toString());
            byte[] response;
            int status;

            t.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            if (Files.exists(dataPath)) {
                response = Files.readAllBytes(dataPath);
                status = 200;
            } else {
                // Return error
                response = "{\"error\":\"data not found\"}".getBytes(StandardCharsets.UTF_8);
                status = 404;
            }
            t.sendResponseHeaders(status, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                String err = "{\"error\":\"internal\"}";
                t.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = err.getBytes(StandardCharsets.UTF_8);
                t.sendResponseHeaders(500, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }   
}