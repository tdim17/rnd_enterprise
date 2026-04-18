# rnd_enterprise — Broken Links Checker

## What It Does

An automated tool for detecting broken links and images across the content base of a target website.

The project operates entirely via API — no browser required. It fetches content records from a CMS, parses embedded HTML, validates every link and image URL for availability, and sends a broken links report back to the API.

---

## How It Works

### Full cycle per test run

```
API (getAuthToken)
    → obtain auth token
    → API (exportContentVerify) — fetch a batch of content records
    → parse HTML from the verifyData field (Jsoup)
    → extract all href and src values
    → deduplicate
    → normalize URLs
    → check HTTP status for each URL (HttpURLConnection)
    → write broken links to broken_links.ndjson
    → advance offset, repeat for next batch
    → API (importContentVerify) — submit final broken links report
```

### Iteration logic

The test runs in a loop. Each iteration fetches a batch of records (`limit` + `offset`), validates them, and advances the offset. Batch size and iteration count are controlled via the configuration file, and overridden by Jenkins pipeline parameters at runtime.

---

## Tech Stack

| Component          | Technology                |
|--------------------|---------------------------|
| Language           | Java 21                   |
| Test framework     | JUnit 5                   |
| API testing        | REST Assured              |
| HTML parsing       | Jsoup                     |
| JSON serialization | Jackson                   |
| Reporting          | Allure (JUnit 5 listener) |
| Boilerplate        | Lombok                    |
| Build              | Maven                     |
| CI                 | Jenkins                   |

---

## Project Structure

```
src/test/java/com/linkvalidator/
├── core/
│   ├── ConfigurationReader.java   — loads configuration (CI or local)
│   └── FlowMethods.java           — API scenario layer (@Step for Allure)
├── pojo/
│   ├── LinkCheckItem.java         — single URL entry (id, type, link)
│   ├── ExportItem.java            — API payload record (id + verifyData[])
│   └── VerifyDataItem.java        — verifyData element (type + value)
├── tests/
│   ├── BaseTest.java              — @BeforeAll: baseURI setup + Allure filter
│   └── TestBrokenLinks.java       — main test (iterative validation loop)
└── utilities/
    ├── Utils.java                 — HTTP check, HTML parsing, NDJSON operations
    ├── NdJsonWriter.java          — write / clear broken_links.ndjson
    └── BlockedHostsProvider.java  — host exclusion filter
```

---

## Configuration

`configuration.properties` **is not stored in the repository**. It is provided by Jenkins Managed Files at runtime.

For local execution, create `Configuration.properties` in the project root manually.

`ConfigurationReader` resolves the source automatically:
- **CI** — reads from the path provided via `-DConfiguration.properties` system property
- **Local** — reads `Configuration.properties` from the project root

### Iteration parameters

Three parameters control the scope of each run. In Jenkins they are overridden by pipeline parameters (`LIMITED_ID`, `OFFSET`, `ITERATIONS_LIMIT`) without modifying the config file.

| Parameter                  | Description                        |
|----------------------------|------------------------------------|
| `idNumberInResponseLimit`  | Number of records per batch        |
| `offsetParam`              | Starting offset                    |
| `iterationsLimit`          | Maximum number of iterations       |

---

## Host Exclusion List

The blocked hosts file **is not stored in the repository** — it contains sensitive infrastructure details. It is provided by Jenkins Managed Files alongside the configuration file.

`BlockedHostsProvider` loads the list at startup and skips matching URLs without making any HTTP request.

---

## URL Validation Logic

`normalizeURL()` filters and normalizes each raw URL before validation:

| Input value                        | Result                          |
|------------------------------------|---------------------------------|
| `null`                             | skip                            |
| empty string                       | `__INVALID__` (broken)          |
| `#anchor`, `javascript:`, `mailto:`| skip                            |
| `/relative/path`                   | resolved to absolute URL        |
| `https://...` / `http://...`       | validated as-is                 |
| anything else                      | `__INVALID__` (broken)          |

URLs returning HTTP status ≥ 400 are written to the NDJSON report as broken.

---

## Reporting

- **Allure Report** — generated automatically after each run. Local preview: `mvn allure:serve`. In Jenkins: published via the Allure plugin.
- **broken_links.ndjson** — a newline-delimited JSON file containing all broken link records. Submitted to the API via `importContentVerify` at the end of each run.

---

## CI/CD

`Jenkinsfile` defines the pipeline:

1. **Config file** — injects `configuration.properties` and `blocked-hosts.txt` from Jenkins Managed Files; overrides iteration parameters.
2. **Run tests** — `mvn clean test`.
3. **Post: always** — publishes the Allure Report.
4. **Post: failure / unstable / aborted** — sends a Telegram notification via Bot API. Credentials are stored in Jenkins Credentials Store, not in code.

---

## Quick Start (local)

```bash
# Run the test
mvn clean test

# Open Allure Report
mvn allure:serve
```

Before running, create `Configuration.properties` in the project root with the required parameters.
