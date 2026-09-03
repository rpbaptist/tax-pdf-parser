# tax-pdf-parser

A Java AWS Lambda that turns a jaaropgave/aangifte PDF into clean markdown
via [OpenDataLoader](https://github.com/opendataloader-project/opendataloader-pdf)
(fast mode), ahead of field extraction in
[belastingaangifte-check](https://github.com/rpbaptist/belastingaangifte-check).

Claude currently reads these PDFs natively and does layout parsing and field
mapping in one pass — a real source of extraction errors on dense,
differently-laid-out tables. This service does the layout parsing
deterministically first, so Claude only has to do semantic field mapping over
already-correctly-parsed text. It's strictly additive: if a parse fails or
times out, the caller falls back to its original Claude-native read
unchanged — see `CONTEXT.md` for the full domain model and
[belastingaangifte-check#96](https://github.com/rpbaptist/belastingaangifte-check/pull/97)
for the caller-side integration.

## Architecture

```
belastingaangifte-check (Next.js server)
  -> uploads the PDF to S3 (HandoffBucket)
  -> invokes this Lambda's Function URL (IAM/SigV4-signed) with the S3 key
tax-pdf-parser (this repo)
  -> fetches the PDF from S3
  -> parses it with OpenDataLoader (fast mode only — see CONTEXT.md)
  -> deletes the S3 object on success
  -> returns { "markdown": "..." }
```

See `template.yaml` for the full resource definitions (S3 bucket with a
1-day lifecycle backstop, least-privilege IAM role) and `HANDOFF.md` for the
original architecture decisions.

## Prerequisites

This repo pins its own Java and Maven versions via [mise](https://mise.jdx.dev/):

```
mise install
```

This installs Java 21 and the latest Maven, scoped to this project only.

## Build & test

```
mvn test      # unit tests — a real synthetic table PDF through OpenDataLoader,
              # the S3-fetch -> parse -> delete flow, a malformed-PDF-throws guard
mvn package   # builds target/tax-pdf-parser.jar (shaded, ~40MB — bundles
              # OpenDataLoader, veraPDF, PDFBox, the AWS SDK S3 client)
```

(Prefix with `mise exec --` if `java`/`mvn` aren't already on your `PATH`.)

## Development

CI (`.github/workflows/ci.yml`) runs on every push to `master` and every PR,
and fails the build on any violation — run the same checks locally before
pushing:

```
mvn spotless:apply   # auto-formats (google-java-format, AOSP style)
mvn verify            # tests + Checkstyle (checkstyle.xml) + SpotBugs
cfn-lint template.yaml
```

Checkstyle's ruleset is deliberately narrow — unused/star imports, line
length, brace/whitespace consistency, naming — and does not require
Javadoc, matching this project's low-comment style. SpotBugs findings can
be suppressed in `spotbugs-exclude.xml` when they're a known false
positive for a deliberate pattern (e.g. `EI_EXPOSE_REP2` on
dependency-injected constructors) — document the why there, don't just
delete the finding.

Dependabot (`.github/dependabot.yml`) opens weekly PRs for outdated Maven
and GitHub Actions dependencies — `opendataloader-pdf-core` in particular
moves fast.

## Deploy

Requires the [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html)
and AWS credentials with permission to create the resources in
`template.yaml` (S3, Lambda, IAM).

```
mvn package
sam build
sam deploy --guided   # first time — creates a samconfig.toml
```

`sam deploy` prints the `HandoffBucketName` and `ParserFunctionUrl` outputs —
belastingaangifte-check needs both.

## Configuration for the caller

belastingaangifte-check resolves this service from five environment
variables (`lib/pdf-parser-client.ts`); the integration is a no-op until all
five are set:

| Variable | Value |
| --- | --- |
| `PDF_PARSER_BUCKET` | the `HandoffBucketName` output |
| `PDF_PARSER_REGION` | the region this stack is deployed to |
| `PDF_PARSER_FUNCTION_URL` | the `ParserFunctionUrl` output |
| `PDF_PARSER_ACCESS_KEY_ID` | an IAM user/role scoped to `s3:PutObject` on the bucket and invoking the Function URL |
| `PDF_PARSER_SECRET_ACCESS_KEY` | — |

## Status

Fast mode only. Hybrid mode (OpenDataLoader's Docling-backed path for
complex tables) is wired off (`Config.HYBRID_OFF`) pending benchmark
evidence — see CONTEXT.md's **Hybrid mode** entry and the deferred
follow-ups listed in [issue #1](https://github.com/rpbaptist/tax-pdf-parser/issues/1).

Deployed to `eu-central-1`. SnapStart was benchmarked and dropped: it added
~400ms to a cold invocation instead of cutting it, because OpenDataLoader's
parsing classes load lazily on first real invocation, a cost the SnapStart
checkpoint doesn't capture — see `template.yaml` history for the config and
git history for the benchmark numbers.

A smoke test against real jaaropgave PDFs is still pending.
