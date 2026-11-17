# Deploying portfolio-api

This is provider-neutral: it works on any host that runs a Docker image or a Java 21 jar. It deliberately does not pick a provider, because plans, free tiers and prices change; check the provider's current pages before you commit to one.

## What is and is not verified

Checked on a real jar: the `prod` profile refusing to start without a database (with a readable message), the health check, nothing else exposed by Actuator, the `PORT` variable, the API under `prod` on an in-memory database in PostgreSQL mode, and rate limiting behind a proxy (below).

**Not checked, because this was written on a machine with neither Docker nor PostgreSQL:** building the Docker image, a real PostgreSQL database, a real SMTP server, and a real Anthropic call. Expect to fix small things on the first real deploy, and treat that first deploy as the test.

## Build and run

```
mvn test                       # run the tests first; the image build skips them
docker build -t portfolio-api .
docker run -p 8080:8080 --env-file my.env portfolio-api
```

Without Docker: `mvn -DskipTests package`, then `java -jar target/portfolio-api-0.0.1-SNAPSHOT.jar` with the same variables in the environment and `SPRING_PROFILES_ACTIVE=prod`.

## Environment variables

Set these in the host's dashboard, never in a file you commit.

| Variable | Needed | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` (the Docker image already sets it) |
| `SPRING_DATASOURCE_URL` | **yes** | JDBC URL of the database, e.g. `jdbc:postgresql://HOST:5432/DATABASE`. The app refuses to start without it |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | if the database needs them | Database login |
| `PORT` | set by the host | The port to listen on; defaults to 8080 |
| `CORS_ALLOWED_ORIGINS` | **yes** | The site's exact origin, e.g. `https://syed-mazhar-hussain.netlify.app` (scheme and host, no trailing slash; comma-separated for several). Without it the browser blocks every call from the site |
| `FORWARD_HEADERS_STRATEGY` | **yes, on a host** | `native`. See "Rate limiting behind a proxy" |
| `ANTHROPIC_API_KEY` | optional | Turns the AI assistant on; without it `/api/ai/chat` answers 503 |
| `CONTACT_NOTIFY_TO`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | optional | Emails each contact message to you; see the README, "Email notifications" |

The rest (`ai.*`, `contact.*`) have defaults in `application.properties`; override them the same way with environment variables (`AI_DAILY_CAP=50`, `AI_ANTHROPIC_MODEL=claude-haiku-5-5`, and so on).

## The database

Local development uses an H2 file. On a host that is the wrong thing: most hosting platforms wipe the container's disk on every deploy or restart, which would silently delete every stored contact message. So `prod` has no default database: start it without `SPRING_DATASOURCE_URL` and it stops with "APPLICATION FAILED TO START" naming the missing variable.

Use PostgreSQL (the driver is included). Things to know:
- Providers often hand you a URL like `postgres://user:password@host:5432/db`. This app needs the JDBC form instead: `jdbc:postgresql://host:5432/db`, with the user and password in their own two variables.
- Tables are created and updated automatically from the entities (`ddl-auto=update`). That is fine for now; add real migrations (Flyway) before the schema changes in ways that matter.
- A free database can expire, sleep or have a size limit. Check the provider's current terms, and remember the contact messages are also emailed to you once mail is configured, which is your backup.

## Health check

`GET /actuator/health` answers `{"status":"UP"}` and nothing more (no details). Point the host's health check at it. Nothing else of Actuator is exposed (no `env`, `beans`, `metrics`). The mail server is deliberately left out of the check, so an SMTP outage never makes the platform restart a healthy app.

## Rate limiting behind a proxy

The rate limiter keys on the connection's remote address. Behind a hosting platform that address is the platform's proxy, so with the default setting **every visitor shares one limit**: after 5 contact messages (or 10 chat questions) from anyone, everyone is refused. Setting `FORWARD_HEADERS_STRATEGY=native` makes the app use the real client address from `X-Forwarded-For`, but only when the request comes from a trusted proxy (loopback and private network ranges by default).

Measured on the real jar, with 6 contact posts each:

| Setting | Different `X-Forwarded-For` each time | Same `X-Forwarded-For` each time |
|---|---|---|
| off (default) | 200 200 200 200 200 **429** (header ignored, so it can't be forged) | not run |
| `native` | 200 ×6 (each client counted separately) | 200 ×5 then **429** |

**Check it once after the first deploy.** If the platform's proxy is not on a private address, `native` cannot trust it and you are back to one shared limit. A gentle check that sends nothing real and costs nothing: from your laptop, POST `{}` to `/api/ai/chat` 11 times in a row (an invalid body is rejected with 400 before any model call, but still counts). The 11th should be 429. Then, from your phone on mobile data, send one: it should be 400, not 429. If the phone is also refused, the limit is shared and the proxy setting needs attention.

## Connecting the site

The site is static and the API address is baked in at build time:

1. In `src/environments/environment.ts` of the portfolio project, set `apiBaseUrl` to the deployed API's URL (no trailing slash), and rebuild and upload as usual (`DEPLOY.md` there).
2. Set `featureFlags.aiAssistant` to `true` only once `ANTHROPIC_API_KEY` is set and tested.
3. Run `npm run export:content` in the portfolio project whenever its content changes, and rebuild this API, or the API will serve stale data.

With `apiBaseUrl` set, the site asks the API for projects and experience and shows its own built-in copy while waiting or if the API is unreachable. A free host that sleeps when idle will make the first request slow; the built-in copy covers that.

## After the first deploy

- `GET /actuator/health` is `UP`.
- `GET /api/projects` returns the 7 projects.
- The site (with `apiBaseUrl` set) loads with no CORS error in the browser console.
- Send one real contact message from the site. It should be stored and, with mail configured, arrive in your inbox.
- Run the rate-limit check above.
- Restart the service and confirm the database still has the message: this is the proof the data survives restarts.
