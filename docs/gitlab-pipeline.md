# The GitLab pipeline

`.gitlab-ci.yml` runs the same three gates as `.github/workflows/security.yml`,
on a self-hosted runner, against a mirror of this repository on GitLab.

It exists for two reasons. The gates are demonstrably not a GitHub Actions
trick, and the pipeline runs on hardware that costs nothing to run it on.

GitHub is the source of truth. GitLab holds a read-only copy that nothing ever
commits to, so there is no divergence to reconcile.

## How the copy gets there

GitLab **pull** mirroring (GitLab fetching from GitHub) is a Premium feature,
verified 2026-08-20. On the free tier the push has to come from this side, so
`.github/workflows/mirror-gitlab.yml` force-pushes `main` and its tags to
GitLab after every push to `main`. GitLab then runs `.gitlab-ci.yml` on the g7
runner.

The mirror **skips with a log line rather than failing** when its two secrets
are unset. A mirror that breaks every push to `main` is worse than no mirror,
because a permanently red default branch teaches people to ignore the colour.

## One-time setup

Four of these are credential or account work. Two are host work.

1. **Create the GitLab project, empty.** No README, no `.gitignore`, no
   licence.

2. **Unprotect `main` on the GitLab side**, under Settings, Repository,
   Protected branches.

   This is the step the cube-store runbook got wrong, and the wrong reason is
   worth recording because it sounds right. That runbook said not to initialise
   with a README because the first mirror push would be a non-fast-forward.
   **That reasoning is false**: the mirror already uses `git push --force`,
   which absorbs a non-fast-forward without complaint. What actually refuses
   the push is that GitLab protects the default branch at creation with force
   push disabled, and the pre-receive hook rejects it outright:
   `You are not allowed to force push code to a protected branch`.

   Fix it by unprotecting the branch, **not by deleting and recreating the
   project**, because deleting a project takes its project runners with it.
   Leaving it unprotected is correct anyway: nothing commits to this copy and
   the mirror force-pushes on every run by design.

3. **Enable the existing g7 runner on this project.** It does **not** need to
   be registered again. The runner registered for cube-store is a project
   runner with `locked: false`, which means it can be enabled on further
   projects owned by the same account, under Settings, CI/CD, Runners.

   Registering a second runner would also work and is strictly worse: two
   runners on one machine competing for the same Docker daemon and the same
   Trivy cache, for no gain.

4. **Create a project access token** with the `write_repository` scope at
   Maintainer role. GitLab caps the expiry at one year and offers no way to
   disable it, so **the token dies and the mirror starts failing on a date
   nobody will remember**. Write the date down when you create it.

5. **Add two GitHub repository secrets** to `85ip9gh/car-sale-application`:
   - `GITLAB_MIRROR_URL`: `https://gitlab.com/<namespace>/car-sale-application.git`
   - `GITLAB_TOKEN`: the token from step 4

6. **Create the Maven cache directory on the runner host**, alongside the Trivy
   cache the other project already needs:

   ```
   sudo mkdir -p /var/cache/maven
   sudo chown gitlab-runner:gitlab-runner /var/cache/maven
   ```

   The runner host also needs `docker`, `curl`, `git`, `tar`, `openssl` and
   `python3`. It has all six.

### A failure mode that reads like a config error and is not one

GitLab.com **refuses to run any CI job for an unverified free account**, a
self-hosted runner included. The pipeline fails instantly with zero jobs and
valid YAML, which looks exactly like a broken pipeline file. Owning the runner
does not route around it.

The reason is not visible in the REST pipeline object. It surfaces only through
GraphQL:

```
failureReason:  "The pipeline failed due to the user not being verified."
errorMessages:  "Identity verification is required in order to run CI jobs"
```

Phone verification was enough on 2026-08-23; no card was required. This account
is already verified, so this should not recur, and it is recorded because the
symptom points somewhere else entirely.

Do not read `data-identity-verification-required` off the rendered pipeline
page to rule this out. That attribute describes the **viewing** user, not the
pipeline, and unauthenticated it says nothing. Ask the API what happened.

## Five ways this file differs from the GitHub workflow

Each one was forced by a rehearsal on the real runner, not chosen.

### 1. Reports are written by shell redirects, never by `--output`

Every scanner runs in a container as root. `trivy --output <file>` makes that
container create the file, so it lands root-owned inside a workspace owned by
`gitlab-runner`. With `GIT_STRATEGY: clone` the **next** job then wipes that
directory, cannot `chmod` a file it does not own, and dies in `get_sources`
before its script runs.

The gate that gets blamed is not the gate that caused it, and because it only
bites whichever job is scheduled after `deps`, it looks intermittent. Proved on
cube-store 2026-08-23, where the `iac` job failed and checkov was never
invoked.

A plain `>` redirect is performed by the runner's own shell, so the file is
created as `gitlab-runner`. It does not weaken the gate: there is no pipe, so
`--exit-code 1` still reaches `|| STATUS=$?`, and trivy logs to stderr, so the
file holds the report and nothing else.

This does not apply to GitHub Actions, where every job gets a fresh VM and
there is no persistent workspace to poison.

### 2. Pipeline-scoped names and ephemeral loopback ports

Containers, networks and image tags all carry `$CI_PIPELINE_ID`, and the two
smoke tests bind `--publish 127.0.0.1::8080` instead of a fixed `18080` and
`18081`.

On a throwaway Actions VM a hardcoded port is free by definition and private by
definition. On g7 it is neither: **the production car-sale stack runs on that
same machine**, and a fixed port would publish an unauthenticated test API on
the host's LAN address for as long as the job runs.

The runner is currently `concurrent = 1`, so pipelines queue rather than
overlap. That setting is one edit away from being untrue, which is why the
scoping is here rather than left to luck.

### 3. The dependency scan is offline, cached, and asserts its own coverage

This is the largest difference and the one worth reading.

**Trivy's pom analyzer cannot resolve this project over the network.** It
fetches parent POMs and BOMs in a burst that Maven Central throttles. Measured
2026-08-23: three consecutive filesystem scans died with
`429 Too Many Requests ... Retry-After: 1800`, each producing a **zero-byte
report and exit 1**. That is the worst possible shape, because exit 1 is also
what a real HIGH finding produces, so an outage is indistinguishable from a
vulnerability.

It is not a bad-IP problem. The same scan failed the same way from a second,
unrelated address, while a plain `curl` to the very POM trivy could not fetch
returned 200 from both. Maven's own client resolves this project fine, which is
why the image build in the same job works.

`--offline-scan` stops the requests. **On its own it also guts the scan.**
Measured on the same tree:

| Configuration | Java packages resolved | Exit code |
|---|---|---|
| `--offline-scan`, no local Maven repository | 8 | 0 |
| `--offline-scan`, populated Maven repository | 77 | 0 |

Eight packages is not a smaller answer. It is a gate that found nothing to look
at, exiting zero and saying so only in a `WARN` buried in a noisy log.

So the job populates `/var/cache/maven` with `mvn dependency:go-offline`
first, mounts it into trivy, and then **asserts that the scan examined a
plausible tree**. The floors are `MIN_JAVA_PACKAGES` and `MIN_NPM_PACKAGES`,
set well below the measured 77 and 36 so ordinary dependency changes never trip
them. They exist to catch a collapse, not a drift.

That assertion was proved to block, not asserted to work: emptying the Maven
cache and re-running produced `COVERAGE FAILURE: pom resolved 8 packages, floor
is 50` and exit 1, on a scan trivy itself had exited 0 on.

The number was also cross-checked by an independent route. The built API image
carries **75 jars**, read out of the shipped artefact with no Maven Central
involved at all, against the pom scan's 77.

**The frontend tree is covered here and nowhere else.** The web image is nginx
plus compiled assets and carries no `node_modules`, so its image scan sees 71
Alpine packages and no npm at all. If the filesystem gate stops working, the
React dependency tree is unscanned.

### 4. Image scans carry `--timeout 15m`

The **first** Java image scan on a host downloads Trivy's Java DB, which is
separate from the vulnerability DB and is never fetched by a Node project.
Measured 2026-08-23: that download took **14 minutes**, and trivy's default
5-minute timeout killed the scan with `context deadline exceeded` inside a
layer walk. That reads like a corrupt image and is not one. Warm, the same scan
takes **6 seconds**.

The DB is cached for three days, so this recurs rather than happening once.

### 5. No step uses `exit 0` to mean "this check passed"

GitHub runs each step in its own shell, so `exit 0` at the end of a successful
check ends that step. **GitLab runs the whole job as one shell script**, so the
same line would end the entire job green with every later gate unrun.

Both smoke tests carry a success flag and `break` out of their polling loops
instead. Anything copied across from the GitHub file needs this check.

## What must not be changed casually

- **`GIT_DEPTH: "0"`.** GitLab shallow-clones by default. `SECURITY.md` records
  credential material that is permanent in this repository's history, and a
  truncated clone lets the secret gate pass while that material is still
  reachable in a public repository. This is the line most likely to be deleted
  by someone tidying up.
- **`GIT_STRATEGY: clone`.** A shell runner keeps its build directory between
  jobs. A scan that reads leftover files is reporting on something other than
  the commit under test.
- **The shell redirects.** See difference 1.
- **The coverage floors.** Lowering them to clear a red pipeline converts the
  gate back into decoration. If a real dependency removal drops the count below
  a floor, move the floor and say why in the commit.
- **The severity threshold.** A CVE with no upstream fix goes in `.trivyignore`
  with a dated reason, so the judgement is recorded. Lowering the threshold
  erases it.

## What this pipeline does not cover

- **Compose files.** Checkov 3.3.13 has no compose framework, verified against
  its own valid-framework list, so `deploy/compose.yaml` and both
  `docker-compose.yml` files stay a manual review.
- **The two sides are not equally covered.** The `gitlab_ci` checkov framework
  contributes 14 checks here against 144 from `github_actions`, and one of its
  four rules can never fire on a GitLab file at all: `CKV_GITLABCI_1` is a
  CircleCI class bound to `jobs.*.steps[]` looking for a `run` key, which a
  GitLab job does not have. It is a permanent pass, not a clean bill of health.
- **This is supply-chain scanning in CI.** It is not a security audit and not
  penetration testing.

## Verified

Rehearsed end to end on the g7 runner host on 2026-08-23, against a full clone
of `main`, before this file was committed:

| Job | Result |
|---|---|
| `secrets` | exit 0, 153 commits scanned, no leaks |
| `deps` | exit 0, 77 Java and 36 npm packages examined, 0 findings, API up on Spring Boot 3.5.16, web image serving `/healthz` as `nginx` |
| `iac` | exit 0, dockerfile 207 passed, `github_actions` 144 passed, `gitlab_ci` 14 passed, 0 failed |
| coverage assertion | **proved to block**: exit 1 on a cold Maven cache that trivy exited 0 on |
| workspace after `deps` | no root-owned files, wiped cleanly as the runner user |

`.gitlab-ci.yml` also passes GitLab's own lint endpoint with no errors and no
warnings.
