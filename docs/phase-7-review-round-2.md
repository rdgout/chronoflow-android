# Phase 7 review round 2

Preflight: `feat/phase-7-today`, starting at `6634c0b5`. Existing untracked later-phase briefs are untouched.

## 1. Watchdog warm-up per Robolectric SDK

TestTimingListener now classifies the test at its start, claims the first Robolectric test for each SDK from its `[N]` display-name suffix, and saves that status with its start time. The watchdog and budget classifier consume that same status. Plain JVM tests always get the normal cap; each cold Robolectric SDK gets 3×, and subsequent tests of that SDK get the normal cap. JDWP and timing-tier exceptions remain unchanged. AGENTS.md describes the corrected policy.

The proof worker uses metadata-only fixtures to exercise the real listener without booting Android. Its new regression first starts/finishes a JVM test, then pauses `robo[29]` for 200 ms under a 100 ms configured cap. Before the fix it halts with exit 86 instead of completing (2.67 s command wall). After the fix it exits 0 without a hang file. A second proof adds SDK 36 and proves it also gets an independent cold allowance; the warmed SDK 29 proof still halts its next test at 100 ms.

Commands:

```sh
make targeted ONLY="core:testing-junit:TestHangWatchdogTest#aJvmTestDoesNotSpendTheFirstRobolectricSdkAllowance"
make targeted ONLY="core:testing-junit:TestHangWatchdogTest core:testing-junit:TimingClassifyTest"
make targeted ONLY="core:testing-junit:TestHangWatchdogTest"
```

The two suites pass all 14 cases (5.07 s wall). After making the metadata fixture an interface rather than an annotated abstract test base, the seven watchdog cases pass again (4.32 s wall). Final XML seconds: JVM then Robolectric 0.298; distinct SDKs 0.518; warmed SDK normal cap 0.450; JDWP 0.725; ordinary halt/evidence 0.214; cancellation 0.354; configuration 0.000. No new budget or timeout exemption was added to actual test classes. Logs: `/tmp/phase7-r2-1-{red,green,final}.log`.

## 2. Guard TodayTab's actual key argument

Chose the permitted Konsist guard, not a second Activity/session/database fixture. TodaySessionKeyTest parses the actual TodayTab function, removes comments/string contents with KotlinLexer, isolates the model's viewModel call, and requires `key = todayViewModelKey(session?.snapshot?.epoch)` in that call's arguments. Parsing is class setup, consistent with the existing Konsist rules. The test takes **0.002 s**; the positive baseline command takes 1.87 s wall.

Mutation proof: removed only the key argument, ran `make targeted ONLY="konsist:TodaySessionKeyTest"`, and restored TodayTab in the script's finally block. It fails with `TodayTab must pass its current session epoch as the model key` (1.75 s command wall). TodayTab has no final diff. Initial guard setup used Konsist's direct-variable list, which excludes the model nested in the Box lambda (NoSuchElementException, 2.50 s); selecting the function and lexing its initializer fixes that harness issue before the mutation proof.

Final touched-only command passes **15 cases**, **5.49 s wall**:

```sh
make targeted ONLY="core:testing-junit:TestHangWatchdogTest core:testing-junit:TimingClassifyTest konsist:TodaySessionKeyTest"
```

XML seconds: key guard 0.002; JVM then Robolectric 0.300; separate SDK 36 allowance 0.519; warmed SDK normal cap 0.464; JDWP 0.778; ordinary halt/evidence 0.223; cancellation 0.354. The seven budget-classifier cases also pass. Logs: `/tmp/phase7-r2-2-{baseline-fixed,red}.log`, `/tmp/phase7-r2-targeted.log`.

## Final quick run

The single `make quick` run **passes**, **59.25 s command wall**. No budgets were changed for Low Power Mode and there were **no timing overruns or hangs** in this run.

| Tier | Cases | Wall seconds | Result |
|---|---:|---:|---|
| Main JVM/Robolectric | 3,694 executed | 36.19 | pass; all assertions pass, no hang files |
| Build logic | cached result files reused; 0 re-executed | 1.08 | pass |
| Tooling | 168 Scripts + 25 contracts + 49 l10n + 9 icons | 19.541 + 0.108 + 0.017 + 0.051 test seconds | pass |
| Serial timing | 7 executed | 1.83 | pass |

Quick XML case seconds: TodaySessionKeyTest **0.009**; JVM-before-Robolectric proof **0.404**; second-SDK proof **0.676**; warmed-SDK normal-cap proof **0.488**; JDWP **0.894**; ordinary halt/evidence **0.304**; cancellation **0.472**; configuration **0.001**. All touched cases stay below one second.

Evidence: `/tmp/phase7-r2-quick.log`, `build/test-results/quick/timings.json` and its copied XMLs, plus the build-logic and timing tier result directories. No application runtime source, sibling repository, emulator state, profile artifact or unrelated test budget was changed in this round. The untracked later-phase briefs remain untouched.
