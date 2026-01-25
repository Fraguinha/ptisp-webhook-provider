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

    public static String toPtispTxtName(final String name, final String domain) {
        if (name.endsWith("." + domain) || name.equals(domain)) {
            return name;
        }
        if (name.endsWith("-" + domain)) {
            return name.substring(0, name.length() - domain.length() - 1) + "." + domain;
        }
        return name;
    }

    public static String fromPtispTxtName(final String name, final String domain) {
        if (!name.endsWith("." + domain)) {
            return name;
        }
        final String relative = name.substring(0, name.length() - domain.length() - 1);
        // PTisp stores _edns.a-{record} TXT ownership records using its own encoding.
        // Both forms are normalised to the external-dns v0.21 a-prefixed canonical name.
        //   PTisp apex form _edns.a.{domain} → _edns.a-{domain}
        if (relative.equals("_edns.a")) {
            return "_edns.a-" + domain;
        }
        //   PTisp subdomain form _edns.a-{sub}.{domain} → unchanged (_edns.a-{sub}.{domain})
        if (relative.startsWith("_edns.a-")) {
            return name;
        }
        // Legacy format migration: old _edns.{domain} / _edns.{sub}.{domain} stored directly in PTisp.
        if (relative.equals("_edns")) {
            return "_edns.a-" + domain;
        }
        if (relative.startsWith("_edns.")) {
            final String sub = relative.substring("_edns.".length());
            if (!sub.contains("-")) {
                return "_edns.a-" + sub + "." + domain;
            }
        }
        // PTisp dash encoding for other TXT records (e.g. DKIM):
        //   _dkim.mail.example.com → _dkim.mail-example.com
        if (relative.matches("_[^.]+\\.[a-z]+")) {
            return relative + "-" + domain;
        }
        return name;
    }
}
