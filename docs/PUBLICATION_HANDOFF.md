# Feature branch publication handoff

## Completed locally

Reviewed original source, retained all third-party licenses, added FEATURE_STATUS.md,
configured branch/PR/manual workflow triggers and APK/checksum/status artifacts,
clean-built both APKs and reran 35 JVM tests plus lint/package/signing checks.
The authorized feature branch is committed locally. A transferable Git bundle and
source/APK zip preserve the exact committed source without credential files.

## Confirmed external blockers

1. HTTPS Git reads succeed, but pushing only
   `feature/jerry-original-phase1` to
   `tanimgaming31-ops/Jerry-AI-Original-Tanim-Boss` returns HTTP 403:
   permission denied to tanimgaming31-ops. The standard Git path and the existing
   GH_TOKEN-backed GitHub CLI credential helper both produce this result. No
   alternative user identity, extracted credential, forced push or interactive
   login was used. User authorization exists; GitHub credential authorization is
   still insufficient. Restore repository write access, including workflow updates,
   through the connected GitHub/environment credential configuration.
2. `api.github.com` is denied by this cloud environment's HTTP policy, so gh API/PR/
   Actions operations cannot run. The attempted additive network draft update
   returned draft_not_editable. Persistence is unconfirmed. Preserve the proposed
   custom-host addition in GITHUB_NETWORK_PROPOSAL.json; existing package-manager
   presets must be retained. Add api.github.com through environment settings (or
   open a new setup chat if that draft can no longer be edited). Never provide
   credentials in chat or commit them to source.

The public GitHub Actions page is readable but there is no new branch push/run to
monitor. No draft PR, hosted run, artifact upload or artifact URL is claimed.
Remote main was verified unchanged at
f38fccd02892223bd2b3c3df8b5ace06722c3b69. Other repositories were not changed.

## Resume after access is restored

From this repository, check the selected branch and destination, then:

```bash
git switch feature/jerry-original-phase1
git push --set-upstream origin HEAD:refs/heads/feature/jerry-original-phase1
gh pr create --repo tanimgaming31-ops/Jerry-AI-Original-Tanim-Boss \
  --draft --base main --head feature/jerry-original-phase1 \
  --title 'Add independent Jerry Android foundation and confirmed commands' \
  --body-file docs/DRAFT_PR_BODY.md
gh run list --repo tanimgaming31-ops/Jerry-AI-Original-Tanim-Boss \
  --branch feature/jerry-original-phase1 --workflow android.yml \
  --json databaseId,headSha,status,conclusion,url
```

Check existing PRs before creating one if another person resumed this handoff.
Inspect the actual workflow run for the pushed HEAD. If it fails, diagnose/fix only
this branch and rerun; do not claim success from configuration or the local build.
On a successful run, use the summary's Download debug APK artifact link. The
artifact contains jerry-ai-phase1-debug.apk, SHA256SUMS.txt and FEATURE_STATUS.md
and is retained for 30 days. Record the real run/artifact URL and whether the
artifact is expired. GitHub sign-in/repository access can be required to download.

The feature-branch push trigger works independently of merging main. Manual
workflow dispatch may not be available while the workflow is absent from the
repository's default branch; do not merge just to enable it. Do not merge main,
push another repository, publish a production release or claim phone testing.
