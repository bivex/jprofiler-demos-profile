package com.demo;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;

public class MemoryLeakDemo {

    private static final List<SessionCache> sessionStore = new ArrayList<>();

    public static class SessionCache {
        private final String userId;
        private final byte[] buffer;
        private final Map<String, String> metaData;

        public SessionCache(String userId, int size) {
            this.userId = userId;
            this.buffer = new byte[size];
            this.metaData = new HashMap<>();
            for (int i = 0; i < 50; i++) {
                metaData.put("key_" + i, "val_" + UUID.randomUUID());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Generating Heap Dump scenario with simulated caches...");
        for (int i = 0; i < 500; i++) {
            sessionStore.add(new SessionCache("user_" + i, 16 * 1024)); // 16 KB each
        }

        String dumpPath = "/Volumes/External/Code/jprofiler-demos-profile/heap_demo.hprof";
        java.io.File file = new java.io.File(dumpPath);
        if (file.exists()) file.delete();

        System.out.println("Writing HPROF dump to " + dumpPath + "...");
        dumpHeap(dumpPath, true);
        System.out.println("Heap dump generated successfully!");
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
