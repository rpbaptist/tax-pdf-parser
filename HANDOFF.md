# Handoff: PDF Parsing Service for Tax Checker RAG Pipeline

## Context

The tax checker app (Next.js on Vercel) is adding RAG using current tax data.
Source data includes tax tables in PDFs uploaded live by users, which need to
be parsed into structured markdown/JSON before being fed into the RAG
ingestion step.

## Decisions made and why

**Parser: OpenDataLoader**, not LlamaParse, Docling, or Marker.

- LlamaParse ruled out: it's a cloud API — PDFs would be sent to a third
  party. Not acceptable for tax documents.
- Docling / Marker ruled out for now: both are Python, both load large
  ML models on cold start (uncertain latency benefit over a JVM), and neither
  clearly beats OpenDataLoader's benchmarked table accuracy. Marker in
  particular is known to merge columns on dense numeric tables — a real risk
  for tax figures.
- OpenDataLoader: open source, self-hostable, ranked #1 in one recent
  cross-parser table-extraction benchmark (0.90–0.907 overall). Supports a
  "fast" deterministic local mode and a "hybrid" mode that can call out to a
  Docling backend for complex/borderless tables — so Docling isn't fully off
  the table, it can be used *within* OpenDataLoader for hard cases only.

**Hosting: AWS Lambda, native Java runtime** (Option B), not the
OpenDataLoader Node.js wrapper, and not Vercel.

- Vercel ruled out: no Java runtime, no JVM in the function environment.
  OpenDataLoader's core is Java; even its Node.js package spawns a JVM
  subprocess under the hood, so it can't run in a standard Vercel function.
- Node wrapper on Lambda (Option A, container image) ruled out as the
  starting point: still pays JVM startup cost, doesn't benefit from Lambda
  SnapStart (which needs the native Java runtime), and its only real
  advantage is staying in TypeScript — not worth the latency tradeoff for a
  live, user-facing upload flow.
- Native Java runtime + OpenDataLoader's Java SDK directly: supports
  SnapStart, the main lever for cutting cold-start time on a live path.

**Transport: PDF goes to S3, not inline in the request.**

Lambda's synchronous payload limit (~6MB via API Gateway or Function URL) is
too small to rely on for tax PDFs. Pattern: Next.js uploads the PDF to S3 via
a presigned URL, then calls the Lambda with just the S3 object key.

## Architecture

```
User uploads PDF
   -> Next.js (Vercel) generates presigned S3 URL
   -> Browser uploads PDF directly to S3
   -> Next.js invokes Lambda (Function URL) with the S3 key
   -> Lambda (Java, OpenDataLoader) fetches from S3, parses, returns markdown/JSON
   -> Next.js feeds result into RAG ingestion
```

## Repo structure

**Separate repo** from the tax checker app. Reasons: different language/
toolchain (Java/Maven vs TS/Next.js), different deploy target (AWS SAM/CDK
vs Vercel), independent versioning and CI, and potential reuse as a
general-purpose parsing service later.

## Build plan

1. **Scaffold the Lambda repo with infra-as-code** — new Java project
   (Maven or Gradle), AWS SAM or CDK defining the Lambda, its Java runtime
   version, memory, and timeout.
2. **Add OpenDataLoader's Java SDK** (Maven dependency, not the Node/Python
   wrapper). Minimal handler: accept an S3 key, return parsed markdown/JSON.
   Start with fast mode.
3. **PDF transport via S3** — presigned upload URL from Next.js, Lambda
   receives only the S3 key.
4. **Enable SnapStart** — supported natively on this runtime. Measure
   cold-start time before/after; this is the main latency lever.
5. **Expose via Lambda Function URL** (simpler than API Gateway to start;
   revisit API Gateway later if you need throttling or more control).
6. **Lock down access** — IAM auth on the Function URL, or a shared-secret
   header, since this handles personal tax documents.
7. **Wire up the Next.js side** — upload to S3 → call Lambda with S3 key →
   receive parsed output → feed into RAG ingestion. Add timeout and a
   fallback error state for parse failures.
8. **Benchmark before adding hybrid mode** — test fast mode against real tax
   PDFs first. Only turn on OpenDataLoader's hybrid (Docling-backed) path for
   documents where fast mode's table accuracy isn't good enough, since hybrid
   adds latency and complexity.

## Open questions / things to verify before or during build

- Confirm OpenDataLoader's Java SDK API surface (Maven artifact coordinates,
  method signatures) directly from its docs when starting — verify against
  current docs rather than assuming.
- Confirm actual SnapStart cold-start improvement for this specific handler
  once built — benchmark, don't assume.
- Decide IAM auth vs shared-secret header for Lambda access based on how the
  rest of the AWS account handles cross-service auth.
- Spot-check parsed output against source PDFs for currency values,
  percentages, and multi-level headers (belastingschijven-style tables) —
  known weak points across all these parsers, not just OpenDataLoader.

## Explicitly ruled out

- LlamaParse — third-party data sharing.
- Docling / Marker as standalone Lambda-hosted parsers — Python cold-start
  uncertainty, no clear accuracy win over OpenDataLoader; may still be used
  indirectly via OpenDataLoader's hybrid mode.
- OpenDataLoader Node.js wrapper on Lambda — JVM subprocess overhead, no
  SnapStart benefit.
- Vercel-hosted parsing — no Java runtime available.
