package ru.itmo;

import com.fastcgi.FCGIInterface;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final String RESPONSE_TEMPLATE = "Content-Type: application/json\r\nContent-Length: %d\r\n\r\n%s";

    public static void main(String[] args) {
        FCGIInterface fcgi = new FCGIInterface();
        while (fcgi.FCGIaccept() >= 0) {
            long startTime = System.nanoTime();
            try {
                String body = readRequestBody();
                JSONObject jsonRequest = new JSONObject(body);

                double x = jsonRequest.getDouble("x");
                double y = jsonRequest.getDouble("y");
                double r = jsonRequest.getDouble("r");

                boolean isInside = calculate(x, y, r);
                long endTime = System.nanoTime();

                String jsonResponse = new JSONObject()
                        .put("result", isInside ? "Попадание" : "Не попадание")
                        .put("x", x)
                        .put("y", y)
                        .put("r", r)
                        .put("currentTime", LocalDateTime.now().toString())
                        .put("executionTime", (endTime - startTime) + "ns")
                        .toString();
                sendJson(jsonResponse);
            } catch (Exception e) {
                logger.severe("Error: " + e.getMessage());
                sendJson(new JSONObject().put("error", e.getMessage()).toString());
            }
        }
    }

    private static boolean calculate(double x, double y, double r) {
        if (x >= 0 && y >= 0 && x <= r && y <= r / 2) {
            return true;
        }
        if (x >= 0 && y <= 0 && (x * x + y * y <= r * r)) {
            return true;
        }
        if (x <= 0 && y >= 0 && y <= (r + x) / 2) {
            return true;
        }
        return false;
    }

    private static void sendJson(String jsonDump) {
        System.out.printf(RESPONSE_TEMPLATE + "%n", jsonDump.getBytes(StandardCharsets.UTF_8).length, jsonDump);
    }

    private static String readRequestBody() throws IOException {
        FCGIInterface.request.inStream.fill();
        int contentLength = FCGIInterface.request.inStream.available();
        var buffer = ByteBuffer.allocate(contentLength);
        var readBytes = FCGIInterface.request.inStream.read(buffer.array(), 0, contentLength);
        var requestBodyRaw = new byte[readBytes];
        buffer.get(requestBodyRaw);
        buffer.clear();
        return new String(requestBodyRaw, StandardCharsets.UTF_8);
    }
}
