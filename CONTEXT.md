# tax-pdf-parser — Domain Glossary

## Terms

**the Parser**
This service: a Java AWS Lambda that turns a jaaropgave/aangifte PDF into
clean markdown before it reaches an LLM. A pre-processing step, not a
replacement for LLM-based field extraction — it has no concept of tax domain
fields (rekeningnummer, dividendbelasting, etc.). Consumed by
`belastingaangifte-check`'s `lib/extractor.ts`.

**Fast mode**
OpenDataLoader's deterministic, local parsing mode. No external calls, no ML
model cold start. The only mode this service runs in for now.

**Hybrid mode**
OpenDataLoader's mode that can call out to a Docling backend for complex or
borderless tables. Deferred: activating it requires benchmark evidence
(accuracy gain vs added latency) that doesn't exist yet. Wired as a config
option but left off.

**the Handoff PDF**
The S3 object that carries a single PDF from `belastingaangifte-check`'s
Next.js server to this Lambda. Uploaded server-side (never directly by the
browser), referenced by its S3 key in the Lambda invocation payload, and
deleted shortly after processing (explicit delete on success, short bucket
lifecycle rule as a backstop).

**Parse failure**
Any Lambda error or timeout while producing markdown from a Handoff PDF. Not
surfaced to the end user directly — the caller (`belastingaangifte-check`)
falls back to its own pre-existing extraction path. This service has no
retry-to-the-user concept; failure is silent from the taxpayer's perspective.
