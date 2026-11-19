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
