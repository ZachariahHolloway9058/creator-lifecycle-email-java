package com.example.creator;

public final class LifecycleMailService {
    public enum Event { ASSET_DELIVERED, SUBSCRIBER_UPDATED, CONTENT_PROCESSED }

    public record LifecycleEvent(Event event, String recipient, String assetId, String detail) {}
    public record EmailDraft(String subject, String html) {}

    public EmailDraft draft(LifecycleEvent event) {
        return switch (event.event()) {
            case ASSET_DELIVERED -> new EmailDraft(
                    "Digital asset delivery",
                    "<p>Your digital asset is ready.</p><p>Asset: " + event.assetId() + "</p>");
            case SUBSCRIBER_UPDATED -> new EmailDraft(
                    "Subscriber update",
                    "<p>Your subscription changed.</p><p>" + event.detail() + "</p>");
            case CONTENT_PROCESSED -> new EmailDraft(
                    "Content processing update",
                    "<p>Content processing status: " + event.detail() + "</p>");
        };
    }
}
