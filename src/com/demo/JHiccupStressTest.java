package com.demo;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.util.Random;

public class JHiccupStressTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting jHiccup Profiling & Stress Test ===");
        long start = System.currentTimeMillis();

        // Simulate intensive multithreaded and memory workload while jHiccup is active
        int iterations = 1500;
        Random rnd = new Random(42);

        for (int i = 0; i < iterations; i++) {
            byte[] garbage = new byte[1024 * 64]; // 64 KB
            for (int j = 0; j < garbage.length; j += 64) {
                garbage[j] = (byte) rnd.nextInt();
            }
            if (i % 50 == 0) {
                Thread.sleep(15); // allow jHiccup to measure interval transitions
            }
        }

        long duration = System.currentTimeMillis() - start;
        System.out.printf("=== Workload finished in %d ms, dumping heap... ===\n", duration);

        String heapFile = "/Volumes/External/Code/jprofiler-demos-profile/jhiccup_heap.hprof";
        java.io.File f = new java.io.File(heapFile);
        if (f.exists()) f.delete();

        dumpHeap(heapFile, true);
        System.out.println("=== Heap dump saved to " + heapFile + " ===");
    }

    public static void dumpHeap(String filePath, boolean live) throws Exception {
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                ManagementFactory.getPlatformMBeanServer(),
                "com.sun.management:type=HotSpotDiagnostic",
                HotSpotDiagnosticMXBean.class
        );
        mxBean.dumpHeap(filePath, live);
    }
}
