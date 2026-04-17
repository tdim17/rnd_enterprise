package com.linkvalidator.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BlockedHostsProvider {

    private static final Set<String> BLOCKED_HOSTS = new HashSet<>();

    static {
        try (BufferedReader br =
                     new BufferedReader(new FileReader("src/test/resources/blocked-hosts.txt"))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) {
                    BLOCKED_HOSTS.add(line.trim().toLowerCase());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Blocked hosts file not found", e);
        }
    }

    public static boolean isBlocked(String linkNormalized) {
        String url = linkNormalized.toLowerCase();
        return BLOCKED_HOSTS.stream().anyMatch(url::startsWith);
    }

}
