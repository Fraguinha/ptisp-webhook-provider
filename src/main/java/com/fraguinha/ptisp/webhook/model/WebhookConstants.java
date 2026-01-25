package com.fraguinha.ptisp.webhook.model;

public final class WebhookConstants {
    public static final String CONTENT_TYPE = "application/external.dns.webhook+json;version=1";
    public static final String PROVIDER_NAME_HEADER = "X-External-Dns-Provider-Name";
    public static final String PROVIDER_NAME = "ptisp";
    public static final long DEFAULT_TTL = 300L;

    private WebhookConstants() {
    }
}
