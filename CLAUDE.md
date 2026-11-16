@AGENTS.md

## Orchestration (Claude)

Sonnet workers implement and Opus reviewers review. Claude orchestrates and does not hand-write feature code.

1. **Brief.** Every phase's brief, `docs/phases/phase-<id>-<slug>.md`, was written in advance (2026-11-16) from the phase's entry in `plan.md`. The briefs stay untracked in the main checkout until their phase starts (0a OQ-0a-19). A brief holds:
   - the deliverables, tasks and acceptance;
   - the cited research sections, quoted;
   - the iOS references, read at `f391a1fa`;
   - the boards;
   - a pointer to AGENTS.md "Tests".

   Before the worker starts, refresh the brief against the stack top. Copy it into the phase's worktree and re-check every fact it states about earlier phases' code (names, signatures, paths and line numbers, usually under "This repo", "Inputs to read" or "Hand-ins to verify"). Patch the brief wherever the stack differs, then commit it on the phase branch as `docs(phases): add the <id> brief`. Tell the worker to repeat those checks first and to stop on any mismatch.
2. **Implement.** Start one worker subagent with model `sonnet` per phase, on the phase's branch. Phases that run in parallel (plan.md "Parallel waves") each get their own worktree, created from the stack top: `git worktree add ../chronoflow-android-wt/phase-<id> -b feat/phase-<id>-<slug> <stack-top>`.

   Subagent shells start in the main checkout on every call. The worker prompt therefore names the phase's absolute path (for a worktree, `/Users/rickgout/Sites/chronoflow-android-wt/phase-<id>`), and every command runs as `cd <path> && …`, `make -C <path> …` or `git -C <path> …`, with absolute file paths. The worker's first command is a preflight: `git -C <path> rev-parse --abbrev-ref HEAD` must print `feat/phase-<id>-<slug>`, or the worker stops and reports it.
3. **Review.** Opus reviewer subagents check the diff against:
   - `plan.md` and the brief;
   - the boards, using `make captures ONLY=<screens>` next to `_design/canvas/<Name>.dc.html`;
   - the device matrix: the JVM profiles, the Robolectric guards and `pixel`;
   - correctness;
   - performance: the budgets, plus `make perf-timeline` for any timeline change from Phase 1e on. Phases 1b–1d predate the target: there the review checks their JVM budgets and the trace checks they record in `docs/perf-timeline.md`.
4. **Verify.** Reproduce each finding before it goes back, with a failing test, a capture beside its board, a trace or a cited line. Only verified findings go back to the worker; drop the rest.
5. **Repeat** steps 3 and 4 until a review comes back clean. Then the phase is done when:
   - `make check` passes;
   - the commits are atomic Conventional Commits;
   - every task is ticked in `plan.md`.
6. **Restack.** Rebase a finished parallel phase onto the stack top, in the order the phases finish (`git rebase --onto`). Run `make check` again. Only then do the phases that depend on it start.

   Conflicts during a restack:
   - **Generated outputs** (anything `make generate` writes) are never merged by hand: resolve their sources by keeping both sides, run `make generate`, and commit the regenerated output with the phase.
   - **Additive wiring** (`AppGraph`/`SessionGraph` members, `RootContent` and entry-provider registrations, Makefile lists, plan.md ticks and open-question rows): Claude may resolve it by keeping both sides' lines, then runs `make check`. An OQ-A number taken twice is renumbered on the later phase.
   - **A duplicate symbol** (two phases declared the same type): keep the declaration of the phase that owns it (the one the other depends on), and send the other branch back to delete its copy.
   - **Anything else** goes back to the phase's worker with the conflict hunks.

The worker sandbox may be unable to run the Gradle daemon, `adb`, the emulator or `.git/index.lock`; Phase 0a records what works. Claude runs those tiers outside the sandbox (`make check`, `make perf`, `make perf-timeline`, emulator journeys) and feeds the exact output back. Claude also makes the commits the worker could not, using the worker's messages. "No emulator was run" never closes UI work.

There is one `pixel` (`emulator-5554`) and one applicationId for every worktree. Device tiers (`make check`'s smoke run, `make captures DEVICE=1`, `make perf`, `make perf-timeline`, `make baseline-profile`, emulator `make targeted`) therefore take the host-wide device lock (`Scripts/device_lock.py`, `~/.android/cf-emulator.lock`; 0a OQ-0a-21) and run one at a time; a second device tier waits for the lock. JVM and Robolectric tiers run in parallel.

Every brief that adds or changes tests points to AGENTS.md "Tests". Reject a test that breaks those rules in review, before it reaches the emulator.

AGENTS.md "Git" forbids commit trailers; that rule overrides any attribution default in the harness.

## Cleanup

On iOS, 23 worktrees and their builds grew past 150 GB (2026-11-10), and simulator clones to 203 GB (2026-11-11). Clean up as you go.

- **Worktrees.** When a phase is rebased into the stack, or abandoned, remove its worktree in the same session: `git worktree remove <path>`, then `git worktree prune`. A branch ref survives worktree removal.
  - A detached worktree's HEAD may not be on any ref. Check `git for-each-ref --contains <sha>` first, and create a `backup/<date>/<name>` branch if nothing contains it.
  - Keep only the main checkout, the stack top and worktrees with active work.
- **Build output.** Delete a finished worktree's `build/`, `.gradle/` and `.kotlin/` along with it. Run `./gradlew --stop` before ending a session.
- **Emulators.** When a branch's emulator work is done, and before ending the session:
  1. Check that no test is running: `Scripts/emulator.sh busy` must exit 1. It exits 0 (busy) while the device lock is held, while `run_tier.py` or `GradleWorkerMain` runs on the host (`run-tier.sh` execs `run_tier.py`, so `run-tier.sh` never shows in `pgrep`), or while `instrument` runs on the emulator.
  2. Stop every emulator with `make emu-down` (`adb emu kill`).
- **AVDs.** Keep `pixel`, and keep the T2 AVDs (`fold`, `small`, `api29`) once they exist. Delete any AVD created for one branch only, with `$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager delete avd -n <name>`. Keep `pixel` on its Quick Boot snapshot, and wipe it only with `make emu-clean`.
- **Report** the output of `make du` with each cleanup.

## Commands

AGENTS.md "Commands" lists every target. The ones this loop uses:
- **Worker fix rounds:** `make targeted ONLY=…` for what changed, then `make quick` once before handing back.
- **Closing a phase:** `make emu`, then `make check`.
- **Timeline or startup changes:** `make perf-timeline` (from Phase 1e on) or `make perf` (from 0e on), with the numbers recorded in `docs/perf-*.md`.
- **Board review:** `make captures ONLY=<screens>`, with `DEVICE=1` for emulator captures.
- **On request only:** `make avd-create AVD=<name>` and `make matrix`.
- **After a phase:** the cleanup above, then `make du`.
