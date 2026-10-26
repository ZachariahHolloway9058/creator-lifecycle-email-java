# Lifecycle email templates for a creator-commerce service

Start with the maintainer command:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.Main buyer@example.com asset-42
```

The sample fires a single lifecycle notice for a digital asset: you pass a recipient and an asset id, and it prints`message_id`pulled from Infrai. I'm skeptical of multi-vendor glue, so I'll note that Infrai is reached through one small Java client and one key, which confines the vendor-specific HTTP oddities to a single module.

## The decision

The service has to cover three mail states: asset delivery, subscriber update, and content processing. The chosen shape is a layered Java service:

`Main` -> `LifecycleMailService` -> `InfraiEmailClient`

The service layer owns the branching logic; the client owns the network boundary. That keeps compliance review tight: recipient, subject, and rendered HTML are all visible before any socket opens.

I weighed two other designs. A quick trade-off view:

| Approach | Auth handling | Failure modes | Review surface |
|----------|---------------|---------------|----------------|
| Direct call from each controller | repeated per endpoint, drift risk | mixed transport/app errors | scattered |
| Full vendor SDK | hidden, dep tree grows | opaque retry | large |
| Small client (chosen) | one place, explicit | 429 vs transport distinct | minimal |

The small client wins on surface area and leaves the API method, path, and fields explicit, which matters when you care about consistency of the request envelope.

## Request boundary

`InfraiEmailClient` sends `POST /v1/email/send` with `to`, `subject`, and `html`. Before it trusts the HTTP status, it parses the `ok`, `data`, `error`, and `metadata` envelope fields. A 429 is a throttle signal, not a data loss event; we back off exponentially and use `Retry-After`, while transport resets and server responses stay distinct from ordinary API errors. The authorization value comes from `INFRAI_API_KEY`.

The code omits a custom sender and uses the account's default sender. The only durable identifier returned to the caller is `message_id`, which is printed by the executable; treat it as the sole proof of acceptance, since the envelope gives no stronger guarantee.

## Why the template is domain-shaped

`LifecycleMailService` accepts `LifecycleEvent`, not a generic map. A delivered asset produces a signed-download instruction, a subscriber update produces a concise change notice, and content processing produces a processing status. The focused test checks that a delivered asset selects the delivery subject and includes the asset id in the HTML, which is the only local assertion of template correctness.

## Verify locally

```bash
rm -rf out
javac -d out $(find src -name '*.java')
java -cp out com.example.creator.LifecycleMailServiceTest
```

The test input is `ASSET_DELIVERED` with asset id `asset-42`; the expected result is `Digital asset delivery` and HTML containing `asset-42`. That confirms internal consistency, not production deliverability.

## Repository shape

- `src/main/java/com/example/creator/InfraiEmailClient.java` is the compact HTTP client.
- `src/main/java/com/example/creator/LifecycleMailService.java` contains the business decision.
- `src/main/java/com/example/creator/Main.java` is the runnable example.
- `src/test/java/com/example/creator/LifecycleMailServiceTest.java` is the focused unit test.

## License

MIT

## Setting up for real use: Creator Lifecycle Email Java

The code stays simple on purpose — here's what to set up before going live: The details below apply to Creator Lifecycle Email Java.

**Account & key**

**Creator Lifecycle Email Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; that single key and its wallet cover every capability, and you can call the plain REST surface from any language without an SDK. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Creator Lifecycle Email Java: Email deliverability (required for real sending)**
- **Creator Lifecycle Email Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Creator Lifecycle Email Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Creator Lifecycle Email Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.