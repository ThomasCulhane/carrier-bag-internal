package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

public class DBHandler implements HttpHandler {

    private String env(String k, String d) {
        String v = System.getenv(k);
        return v != null ? v : d;
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

    private void sendJson(HttpExchange ex, int status, byte[] bytes) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath(); // e.g. /api/rows or /api/rows/1
        String base = "/api/rows";
        String idStr = null;
        if (path.length() > base.length()) {
            String tail = path.substring(base.length());
            if (tail.startsWith("/")) {
                String[] segs = tail.substring(1).split("/");
                if (segs.length > 0 && segs[0].length() > 0) idStr = segs[0];
            }
        }

        ObjectMapper om = new ObjectMapper();

        try (Connection conn = getConnection()) {
            if ("GET".equalsIgnoreCase(method)) {
                if (idStr == null) {
                    // list all
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, author, title, genre, to_char(submit_date,'YYYY-MM-DD') as submit_date, status, emailed FROM submissions ORDER BY id");
                    ResultSet rs = ps.executeQuery();
                    ArrayNode arr = om.createArrayNode();
                    while (rs.next()) {
                        ObjectNode o = om.createObjectNode();
                        o.put("id", rs.getInt("id"));
                        o.put("author", rs.getString("author"));
                        o.put("title", rs.getString("title"));
                        o.put("genre", rs.getString("genre"));
                        o.put("submitDate", rs.getString("submit_date"));
                        o.put("status", rs.getString("status"));
                        o.put("emailed", rs.getBoolean("emailed"));
                        arr.add(o);
                    }
                    sendJson(ex, 200, om.writeValueAsBytes(arr));
                    rs.close();
                    ps.close();
                } else {
                    // single
                    PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, author, title, genre, to_char(submit_date,'YYYY-MM-DD') as submit_date, status, emailed FROM submissions WHERE id = ?");
                    ps.setInt(1, Integer.parseInt(idStr));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        ObjectNode o = om.createObjectNode();
                        o.put("id", rs.getInt("id"));
                        o.put("author", rs.getString("author"));
                        o.put("title", rs.getString("title"));
                        o.put("genre", rs.getString("genre"));
                        o.put("submitDate", rs.getString("submit_date"));
                        o.put("status", rs.getString("status"));
                        o.put("emailed", rs.getBoolean("emailed"));
                        sendJson(ex, 200, om.writeValueAsBytes(o));
                    } else {
                        sendJson(ex, 404, "{\"error\":\"not found\"}".getBytes("UTF-8"));
                    }
                    rs.close();
                    ps.close();
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                // create
                JsonNode node = om.readTree(ex.getRequestBody());
                String author = node.path("author").asText("");
                String title = node.path("title").asText("");
                String genre = node.path("genre").asText("");
                String submitDate = node.path("submitDate").asText(null);
                String status = node.path("status").asText("UNASSIGNED");
                boolean emailed = node.path("emailed").asBoolean(false);

                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO submissions (author, title, genre, submit_date, status, emailed) VALUES (?,?,?,?,?,?) RETURNING id");
                ps.setString(1, author);
                ps.setString(2, title);
                ps.setString(3, genre);
                if (submitDate != null && !submitDate.isEmpty()) {
                    ps.setDate(4, Date.valueOf(LocalDate.parse(submitDate)));
                } else {
                    ps.setNull(4, Types.DATE);
                }
                ps.setString(5, status);
                ps.setBoolean(6, emailed);
                ResultSet rs = ps.executeQuery();
                int newId = -1;
                if (rs.next()) newId = rs.getInt(1);
                ObjectNode resp = om.createObjectNode();
                resp.put("id", newId);
                sendJson(ex, 201, om.writeValueAsBytes(resp));
                rs.close();
                ps.close();
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (idStr == null) {
                    sendJson(ex, 400, "{\"error\":\"missing id\"}".getBytes("UTF-8"));
                } else {
                    JsonNode node = om.readTree(ex.getRequestBody());
                    String author = node.path("author").asText(null);
                    String title = node.path("title").asText(null);
                    String genre = node.path("genre").asText(null);
                    String submitDate = node.path("submitDate").asText(null);
                    String status = node.path("status").asText(null);
                    Boolean emailed = node.has("emailed") ? node.path("emailed").asBoolean() : null;

                    StringBuilder sql = new StringBuilder("UPDATE submissions SET ");
                    boolean first = true;
                    if (author != null) { sql.append("author=?"); first=false; }
                    if (title != null) { if(!first) sql.append(","); sql.append("title=?"); first=false;}
                    if (genre != null) { if(!first) sql.append(","); sql.append("genre=?"); first=false;}
                    if (submitDate != null) { if(!first) sql.append(","); sql.append("submit_date=?"); first=false;}
                    if (status != null) { if(!first) sql.append(","); sql.append("status=?"); first=false;}
                    if (emailed != null) { if(!first) sql.append(","); sql.append("emailed=?"); first=false;}
                    sql.append(" WHERE id=?");

                    PreparedStatement ps = conn.prepareStatement(sql.toString());
                    int idx = 1;
                    if (author != null) ps.setString(idx++, author);
                    if (title != null) ps.setString(idx++, title);
                    if (genre != null) ps.setString(idx++, genre);
                    if (submitDate != null) ps.setDate(idx++, Date.valueOf(LocalDate.parse(submitDate)));
                    if (status != null) ps.setString(idx++, status);
                    if (emailed != null) ps.setBoolean(idx++, emailed);
                    ps.setInt(idx, Integer.parseInt(idStr));
                    int updated = ps.executeUpdate();
                    ObjectNode resp = om.createObjectNode();
                    resp.put("updated", updated);
                    sendJson(ex, 200, om.writeValueAsBytes(resp));
                    ps.close();
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (idStr == null) {
                    sendJson(ex, 400, "{\"error\":\"missing id\"}".getBytes("UTF-8"));
                } else {
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM submissions WHERE id = ?");
                    ps.setInt(1, Integer.parseInt(idStr));
                    int deleted = ps.executeUpdate();
                    ps.close();
                    if (deleted > 0) {
                        ex.sendResponseHeaders(204, -1);
                    } else {
                        sendJson(ex, 404, "{\"error\":\"not found\"}".getBytes("UTF-8"));
                    }
                }
            } else {
                sendJson(ex, 405, "{\"error\":\"method not allowed\"}".getBytes("UTF-8"));
            }
        } catch (SQLException se) {
            se.printStackTrace();
            sendJson(ex, 500, ("{\"error\":\"db error\",\"message\":\"" + se.getMessage() + "\"}").getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, ("{\"error\":\"internal\",\"message\":\"" + e.getMessage() + "\"}").getBytes("UTF-8"));
        }
    }
}