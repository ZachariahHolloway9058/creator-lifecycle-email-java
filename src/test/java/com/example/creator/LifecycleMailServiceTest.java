package com.example.creator;

public final class LifecycleMailServiceTest {
    public static void main(String[] args) {
        LifecycleMailService service = new LifecycleMailService();
        LifecycleMailService.EmailDraft draft = service.draft(new LifecycleMailService.LifecycleEvent(
                LifecycleMailService.Event.ASSET_DELIVERED, "buyer@example.com", "asset-42", ""));
        if (!draft.subject().equals("Digital asset delivery")) throw new AssertionError(draft.subject());
        if (!draft.html().contains("asset-42")) throw new AssertionError(draft.html());
        System.out.println("PASS asset delivery decision");
    }
}
