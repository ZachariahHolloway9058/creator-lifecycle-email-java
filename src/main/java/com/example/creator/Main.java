package com.example.creator;

import java.net.http.HttpClient;

public final class Main {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        if (args.length < 2) throw new IllegalArgumentException("recipient and asset id are required");
        LifecycleMailService service = new LifecycleMailService();
        LifecycleMailService.LifecycleEvent event = new LifecycleMailService.LifecycleEvent(
                LifecycleMailService.Event.ASSET_DELIVERED, args[0], args[1], "");
        LifecycleMailService.EmailDraft draft = service.draft(event);
        String messageId = new InfraiEmailClient(HttpClient.newHttpClient(), key)
                .send(event.recipient(), draft.subject(), draft.html());
        System.out.println("message_id=" + messageId);
    }
}
