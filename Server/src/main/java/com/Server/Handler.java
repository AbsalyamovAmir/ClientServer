package com.Server;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Handler extends Thread {

    private static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        put("", "text/plain");
    }};

    private static final String NOT_FOUND_MESSAGE = "NOT FOUND";

    private Socket socket;

    private Connection connection;
    {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/CFT", "root", "203307");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (var input = this.socket.getInputStream(); var output = this.socket.getOutputStream()) {
            var url = this.getRequestUrl(input);
            if ("/contracts".equals(url)) {
                retrieveContracts(output);
            } else {
                var contentType = CONTENT_TYPES.get("text");
                sendHeader(output, 404, "Not Found", contentType, NOT_FOUND_MESSAGE.length());
                output.write(NOT_FOUND_MESSAGE.getBytes());
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void retrieveContracts(OutputStream output) throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT * FROM contract");
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("[");
            boolean first = true;
            while (resultSet.next()) {
                if (!first) {
                    responseBuilder.append(",");
                } else {
                    first = false;
                }
                int id = resultSet.getInt("id");
                Date date = resultSet.getDate("date");
                String number = resultSet.getString("number");
                Date lastUpdatedDate = resultSet.getDate("last_updated_date");

                responseBuilder.append(String.format("{\"id\":%d,\"date\":\"%s\",\"number\":\"%s\",\"lastUpdatedDate\":\"%s\"}",
                        id, date, number, lastUpdatedDate));
            }
            responseBuilder.append("]");
            String response = responseBuilder.toString();
            String contentType = CONTENT_TYPES.get("json");
            sendHeader(output, 200, "OK", contentType, response.length());
            output.write(response.getBytes());
        }
    }

    private String getRequestUrl(InputStream input) {
        var reader = new Scanner(input).useDelimiter("\r\n");
        if (reader.hasNext()) {
            var line = reader.next();
            return line.split(" ")[1];
        } else {
            // Обработка случая, когда входные данные пусты
            return ""; // или любое другое значение по умолчанию
        }
    }

    private String getFileExtension(Path path) {
        var name = path.getFileName().toString();
        var extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);
    }

    private void sendHeader(OutputStream output, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(output);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }
}