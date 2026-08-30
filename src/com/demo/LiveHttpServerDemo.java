package com.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class LiveHttpServerDemo {

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

    public static void main(String[] args) throws Exception {
        int port = 8088;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(32, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);

        server.createContext("/api/workload", new WorkloadHandler());
        server.createContext("/api/health", new HealthHandler());

        System.out.println("🚀 Live Java HTTP Microservice started on http://127.0.0.1:" + port);
        System.out.println("⚡ jHiccup is continuously monitoring runtime latency & GC pauses under live traffic...");

        server.start();

        // Run until 10,000 requests are served or 6 seconds elapse
        long stopTime = System.currentTimeMillis() + 6_000;
        while (System.currentTimeMillis() < stopTime && REQUEST_COUNTER.get() < 10_000) {
            Thread.sleep(200);
        }

        System.out.printf("🏁 Handled %,d total live HTTP requests. Shutting down...\n", REQUEST_COUNTER.get());
        server.stop(0);
        executor.shutdownNow();
        System.exit(0);
    }

    static class WorkloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long reqId = REQUEST_COUNTER.incrementAndGet();

            // Simulate real application workload: JSON parsing, crypto hash, transient memory churn
            byte[] garbage = new byte[32 * 1024]; // 32 KB allocation per request
            garbage[0] = (byte) (reqId % 128);

            String hashResult = "";
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                hashResult = Long.toHexString(digest[0] & 0xFF);
            } catch (Exception ignored) {
            }

            String responseJson = "{\"status\":\"ok\",\"requestId\":" + reqId + ",\"hash\":\"" + hashResult + "\"}";
            byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
