import subprocess
import unittest

from helpers import SCRIPTS, temp_dir

BASELINE = (
    "app/MainActivity", "feature/trip/page/TripPageKt", "feature/trip/page/TripPageScreenKt",
    "feature/trip/page/DayHeaderKt", "feature/trip/page/SummaryCardKt",
    "core/plan/drawing/DayDrawingBuilder", "feature/trip/timeline/DayDrawingReplayKt",
    "feature/trip/wide/WideDayRowKt", "feature/trip/wide/PinnedDayListKt",
    "feature/trip/wide/WideListRowsKt", "feature/trip/wide/TripScrollTracker",
    "feature/trip/list/DayListRowKt", "feature/today/ui/TodayScreenKt", "feature/trips/TripsScreenKt",
    "core/ui/plan/StepSheetContentKt", "feature/wizard/ready/WizardReadyStepKt",
    "feature/launch/welcome/WelcomeScreenKt", "feature/auth/model/SignInModel",
)
STARTUP = ("app/MainActivity", "feature/today/ui/TodayScreenKt", "feature/launch/welcome/WelcomeScreenKt")


def profile(classes):
    return "".join(f"HLnet/chronoflow/{name};\n" for name in classes)


class BaselineProfileCoverageTest(unittest.TestCase):
    def setUp(self):
        self.root = temp_dir(self)
        self.baseline = self.root / "baseline.txt"
        self.startup = self.root / "startup.txt"
        self.baseline.write_text(profile(BASELINE))
        self.startup.write_text(profile(STARTUP))
        source = (SCRIPTS / "baseline-profile.sh").read_text()
        self.validator = source.split("<<'PYTHON'\n", 1)[1].split("\nPYTHON", 1)[0]

    def validate(self):
        return subprocess.run(["python3", "-I", "-", str(self.baseline), str(self.startup)], input=self.validator, text=True, capture_output=True, timeout=5)

    def test_complete_coverage_passes(self):
        result = self.validate()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_missing_wizard_is_rejected(self):
        self.baseline.write_text(profile(n for n in BASELINE if "WizardReady" not in n))
        result = self.validate()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("WizardReadyStepKt", result.stderr)

    def test_missing_sign_in_is_rejected(self):
        self.baseline.write_text(profile(n for n in BASELINE if "SignInModel" not in n))
        result = self.validate()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SignInModel", result.stderr)

    def test_missing_startup_welcome_is_rejected(self):
        self.startup.write_text(profile(n for n in STARTUP if "WelcomeScreen" not in n))
        result = self.validate()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("missing startup coverage", result.stderr)
