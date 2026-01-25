package com.fraguinha.ptisp.webhook.util;

public final class DnsUtils {

    private DnsUtils() {
    }

    public static String removeTrailingDot(final String val) {
        return switch (val) {
            case null -> "";
            case "" -> "";
            case final String s when s.endsWith(".") -> s.substring(0, s.length() - 1);
            case final String s -> s;
        };
    }

    public static String removeQuotes(final String val) {
        return switch (val) {
            case null -> "";
            case "" -> "";
            case final String s when s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2 ->
                s.substring(1, s.length() - 1);
            case final String s -> s;
        };
    }

    public static String ensureTrailingDot(final String val) {
        return switch (val) {
            case null -> "";
            case "" -> "";
            case final String s when s.endsWith(".") -> s;
            case final String s -> s + ".";
        };
    }

    public static String ensureQuotes(final String val) {
        return switch (val) {
            case null -> "\"\"";
            case "" -> "\"\"";
            case final String s when s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2 -> s;
            case final String s -> "\"" + s + "\"";
        };
    }
}
