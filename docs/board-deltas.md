# Board deltas
## Today (Phase 7)

| Board | Element | Board | Built | Why |
|---|---|---|---|---|
| Today | Top-left glass icon | Decorative 44 dp glass item | No item | No action on pinned iOS; OQ-A328. |
| Today, TodayEmpty, TodayFuture, TodayLoading, TodayCompleted | App bar and tabs | iOS system chrome | Native M3 app bar and navigation | A9; board surfaces retain their tokens. |
| Today | Panel swatch | Default `md`, 36 dp | Dedicated row board's `sm`, 28 dp | OQ-A326; matches TodayRowStates. |
| Today, TodayRowStates | Running subtitle | Start and end time | End time only, prefixed with → | Pinned TodayStepRow copy/behaviour. |
| Today | Hidden categories notice | Sample “Travel steps are hidden” | Generated some-important/all-categories copy | Pinned iOS; Settings tab is the destination. |
| Today | Adaptation label | Sample “Adapting” | Generated “Adjusting” and its Dutch value | Pinned iOS copy. |
| TodayFuture | Reminder sentence | Promises an evening reminder | Omitted | Pinned iOS drops this promise. The rest stays one board-style paragraph. |
| TodayFuture | Upcoming badge | Outline | Outline | Board appearance; iOS uses secondary. |
| Today | Adapting pill text | Foreground | Foreground | Board appearance; iOS uses dawnText. |
| TodayRowStates | Row title | 16/22, medium | 16/22, medium | Board appearance; iOS uses 16/24 regular. |
| TodayLoading | Heading skeleton | 40% × 20 dp line | Included | Board appearance; iOS omits it. |
| TodayEmpty | Primary action | 40 dp visual height | 40 dp visual / ≥48 dp touch target | Native CfButton Lg and minimum target. |
| TodayCompleted | Card padding | 32 dp vertical, 20 dp horizontal | Same | Board appearance; no shadow. |
| TodayRowStates | Documentation headings and notes | Board includes explanations | Only the three app row states | The headings and notes are board documentation, not app copy. |

The Today captures use the requested 390 × 844 board frame, retaining pixel8 density/insets; the functional pixel8 profile stays 411 × 914. Both themes and languages are captured. Local board HTML references a missing `support.js` and blob URLs, so its source geometry and the pinned iOS screenshots supply the visual reference.

| Board | Element | Board | Built | Why |
|---|---|---|---|---|
| Today | Shared hero Next title | Medium 500 | Regular 400 | Preserve pinned iOS and the landed OQ-A240/6a choice. |
| TodayFuture | Optional Next row in capture | Present in sample | Absent with the Swift snapshot fixture | The fixture supplies the active trip detail while filtering trips to upcoming; the required stale-detail guard rejects it. The screen test separately renders the non-null Next branch; the live source observes the selected hero detail. |
| TodayEmpty, TodayFuture, TodayCompleted | Native title ink in JVM captures | Title visible | Missing in these captures | Unresolved after idle/frame/idle; OQ-A336 retains CfMediumTopAppBar and requires device verification. Its structural tag is present in every state. |
