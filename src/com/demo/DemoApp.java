package com.demo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class DemoApp {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  JProfiler Demo Profiling Application Started   ");
        System.out.println("=================================================");
        long start = System.currentTimeMillis();

        // 1. String Concatenation & Inefficient Pattern Matching
        System.out.println("[Step 1/4] Running String Manipulation & Regex hotspot...");
        testStringConcatenation();

        // 2. CPU Heavy Compute: Cryptographic Hashing & Inefficient Bubble Sort
        System.out.println("[Step 2/4] Running CPU Heavy Computation (SHA-256 & Bubble Sort)...");
        testCryptoAndSortingHotspot();

        // 3. Memory Allocation Churn & Object Lifecycle
        System.out.println("[Step 3/4] Running High-frequency Memory Allocations...");
        testMemoryAllocations();

        // 4. Multithreaded Concurrent Workloads
        System.out.println("[Step 4/4] Running Multithreaded Math Computations...");
        testMultiThreading();

        long duration = System.currentTimeMillis() - start;
        System.out.println("=================================================");
        System.out.printf("  Execution finished in %d ms\n", duration);
        System.out.println("=================================================");
    }

    private static void testStringConcatenation() {
        String result = "";
        for (int i = 0; i < 30_000; i++) {
            result += "id=" + i + ";val=" + (i * 3) + ";";
            if (i % 2000 == 0) {
                "user_audit_log_record_123456_test".matches(".*[0-9]{4,}.*");
            }
        }
    }

    private static void testCryptoAndSortingHotspot() throws NoSuchAlgorithmException {
        // SHA-256 digest computation
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < 400_000; i++) {
            byte[] hash = digest.digest(("sample_payload_data_block_" + i).getBytes());
        }

        // Inefficient Bubble Sort on random integers
        int[] numbers = new int[12_000];
        Random rnd = new Random(42);
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = rnd.nextInt(1_000_000);
        }
        bubbleSort(numbers);
    }

    private static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }

    private static void testMemoryAllocations() {
        List<Map<String, Object>> records = new ArrayList<>(50_000);
        for (int i = 0; i < 50_000; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("index", i);
            map.put("guid", UUID.randomUUID().toString());
            map.put("blob", new byte[512]);
            records.add(map);
        }
        records.clear();
    }

    private static void testMultiThreading() throws InterruptedException {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    double accumulator = 0.0;
                    for (int i = 0; i < 5_000_000; i++) {
                        accumulator += Math.sin(i) * Math.cos(i) + Math.sqrt(i + threadId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }
}
