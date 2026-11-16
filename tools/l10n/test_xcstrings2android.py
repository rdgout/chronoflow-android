import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path

import xcstrings2android as g

APP = "App/Localizable.xcstrings"
AUTH = g.KIT + "/Auth/Resources/Localizable.xcstrings"
PLAN = g.KIT + "/PlanUI/Resources/Localizable.xcstrings"
SCREENS = g.KIT + "/PlanUI/Resources/Screens.xcstrings"
SETTINGS = g.KIT + "/Settings/Resources/Settings.xcstrings"
WIZARD = g.KIT + "/TripWizardUI/Resources/Localizable.xcstrings"
INFOPLIST = "App/InfoPlist.xcstrings"


def single(en, nl, **extra):
    unit = lambda v: {"stringUnit": {"state": "translated", "value": v}}
    entry = {"localizations": {"en": unit(en), "nl": unit(nl)}}
    entry.update(extra)
    return entry


def plural(en_one, en_other, nl_one, nl_other, quantities=("one", "other")):
    def loc(one, other):
        unit = lambda v: {"stringUnit": {"state": "translated", "value": v}}
        values = {"one": one, "other": other, "few": other}
        return {"variations": {"plural": {q: unit(values[q]) for q in quantities}}}

    return {"localizations": {"en": loc(en_one, en_other), "nl": loc(nl_one, nl_other)}}


def cat(**strings):
    return {"sourceLanguage": "en", "version": "1.0", "strings": strings}


def run(catalogs, lock=None, **kw):
    return g.build(catalogs, lock or {}, pin="f391a1fac33eb0f0ea9e3f565206b91118e793d0", **kw)


def en_xml(result, path=None):
    return result.files[path or "core/ui/src/main/res/values/strings_ios.xml"].decode("utf-8")


def nl_xml(result):
    return result.files["core/ui/src/main/res/values-nl/strings_ios.xml"].decode("utf-8")


class GeneratorTest(unittest.TestCase):
    def test_unpositioned_args_become_positional(self):
        r = run({AUTH: cat(**{"%@ and %lld": single("%@ and %lld", "%@ en %lld")})})
        self.assertIn(">%1$s and %2$d<", en_xml(r))
        self.assertIn(">%1$s en %2$d<", nl_xml(r))

    def test_positioned_args_keep_order(self):
        r = run({AUTH: cat(**{"k": single("%1$@ then %2$@", "%2$@ dan %1$@")})})
        self.assertIn(">%1$s then %2$s<", en_xml(r))
        self.assertIn(">%2$s dan %1$s<", nl_xml(r))

    def test_named_args_follow_percent_args(self):
        r = run({AUTH: cat(**{"k": single("%lld for {{name}}", "%lld voor {{name}}")})})
        self.assertIn(">%1$d for %2$s<", en_xml(r))

    def test_named_args_order_by_first_use_in_key(self):
        r = run({AUTH: cat(**{"{{b}} then {{a}}": single("{{a}} {{b}}", "{{a}} {{b}}")})})
        self.assertIn(">%2$s %1$s<", en_xml(r))

    def test_count_key_with_lld_value_is_position_one(self):
        key = "Your {{count}} preparation days start on {{date}}."
        r = run({SCREENS: cat(**{key: plural(
            "Your preparation day starts on {{date}}.", "Your %lld preparation days start on {{date}}.",
            "Je voorbereidingsdag begint op {{date}}.", "Je %lld voorbereidingsdagen beginnen op {{date}}.")})})
        xml = en_xml(r)
        self.assertIn('<item quantity="one">Your preparation day starts on %2$s.</item>', xml)
        self.assertIn('<item quantity="other">Your %1$d preparation days start on %2$s.</item>', xml)

    def test_plurals_one_and_other(self):
        r = run({SETTINGS: cat(**{"%lld left": plural("%lld left", "%lld left", "%lld over", "%lld over")})})
        xml = en_xml(r)
        self.assertLess(xml.index('quantity="one"'), xml.index('quantity="other"'))
        self.assertIn("<plurals ", xml)

    def test_wizard_one_pair_folds_into_plurals(self):
        r = run({WIZARD: cat(**{
            "{{count}} nights": single("{{count}} nights", "{{count}} nachten"),
            "{{count}} nights_one": single("{{count}} night", "{{count}} nacht"),
            "orphan_one": single("Lonely", "Alleen"),
        })})
        xml = en_xml(r)
        self.assertIn('<plurals name="wizard_count_nights">', xml)
        self.assertIn('<item quantity="one">%1$s night</item>', xml)
        self.assertIn('<item quantity="other">%1$s nights</item>', xml)
        self.assertNotIn("nights_one", xml)
        self.assertNotIn("wizard\t{{count}} nights_one", r.lock)
        self.assertIn('<string name="wizard_orphan_one">Lonely</string>', xml)

    def test_unsupported_quantity_fails(self):
        with self.assertRaisesRegex(g.GenError, "exactly one and other"):
            run({SETTINGS: cat(k=plural("a", "b", "c", "d", quantities=("one", "few", "other")))})
        device = single("a", "b")
        device["localizations"]["en"] = {"variations": {"device": {"iphone": {}}}}
        with self.assertRaisesRegex(g.GenError, "unsupported variation"):
            run({SETTINGS: cat(k=device)})

    def test_escaping(self):
        cases = {
            "It's": "It\\'s",
            'Say "hi"': 'Say \\"hi\\"',
            "a\\b": "a\\\\b",
            "Plan & credits": "Plan &amp; credits",
            "<terms>x</terms>": "&lt;terms&gt;x&lt;/terms&gt;",
            "@home": "\\@home",
            "?what": "\\?what",
            " · lead": "\\u0020· lead",
            "trail ": "trail\\u0020",
            "a  b": "a \\u0020b",
            "line\nbreak": "line\\nbreak",
        }
        for en, expected in cases.items():
            r = run({AUTH: cat(**{en: single(en, en)})})
            self.assertIn(f">{expected}</string>", en_xml(r), en)

    def test_escaping_with_time_placeholder_edges(self):
        r = run({SCREENS: cat(**{" · {{time}} ": single(" · {{time}} ", " · {{time}} ")})})
        self.assertIn(">\\u0020· %1$s\\u0020</string>", en_xml(r))

    def test_percent_rules(self):
        r = run({AUTH: cat(**{"50%": single("50%", "50%"), "%lld%": single("%lld%", "%lld%")})})
        xml = en_xml(r)
        self.assertIn('formatted="false">50%</string>', xml)
        self.assertIn(">%1$d%%</string>", xml)

    def test_comment_has_full_key_and_no_double_dash(self):
        key = "A -- B & " + "x" * 200 + "-"
        r = run({AUTH: cat(**{key: single("v", "w")})})
        comment = [l for l in en_xml(r).splitlines() if "<!-- auth:" in l][0]
        self.assertIn("A - - B &amp; " + "x" * 200 + "- ", comment)
        self.assertNotIn("--", comment.replace("<!--", "").replace("-->", ""))

    def test_collision_suffix_goes_to_later_sorted_key(self):
        r = run({AUTH: cat(**{"Check your inbox": single("a", "b"), "Check your inbox.": single("c", "d")})})
        self.assertEqual(r.lock["auth\tCheck your inbox"], "auth_check_your_inbox")
        self.assertEqual(r.lock["auth\tCheck your inbox."], "auth_check_your_inbox_78cde4")

    def test_truncation_vector(self):
        key = "Adding a return flight costs 1 credit, because credits are per direction. Other changes are free."
        r = run({WIZARD: cat(**{key: single("a", "b")})})
        self.assertEqual(r.lock["wizard\t" + key], "wizard_adding_a_return_flight_costs_1_cre_474862")

    def test_lock_names_are_honoured(self):
        r = run({AUTH: cat(Hello=single("a", "b"))}, {"auth\tHello": "auth_custom"})
        self.assertIn('name="auth_custom"', en_xml(r))

    def test_removed_key_keeps_its_lock_entry(self):
        r = run({AUTH: cat(New=single("a", "b"))}, {"auth\tOld": "auth_new"})
        self.assertEqual(r.lock["auth\tOld"], "auth_new")
        self.assertEqual(r.lock["auth\tNew"], "auth_new_" + g.sha6("auth\tNew"))

    def test_same_key_differs_per_table(self):
        r = run({SETTINGS: cat(**{"Early bird": single("Early bird", "Vroege vogel")}),
                 WIZARD: cat(**{"Early bird": single("Early bird", "Ochtendmens")})})
        self.assertNotEqual(r.lock["settings\tEarly bird"], r.lock["wizard\tEarly bird"])
        self.assertIn("Vroege vogel", nl_xml(r))
        self.assertIn("Ochtendmens", nl_xml(r))

    def test_missing_or_empty_dutch_fails(self):
        missing = single("a", "b")
        del missing["localizations"]["nl"]
        with self.assertRaisesRegex(g.GenError, "missing nl"):
            run({AUTH: cat(k=missing)})
        with self.assertRaisesRegex(g.GenError, "empty"):
            run({AUTH: cat(k=single("a", "  "))})

    def test_position_mismatch_fails(self):
        with self.assertRaisesRegex(g.GenError, "positions differ"):
            run({AUTH: cat(k=single("%@ and %@", "%@"))})

    def test_conversion_mismatch_fails(self):
        with self.assertRaisesRegex(g.GenError, "%s in en and %d in nl"):
            run({AUTH: cat(k=single("%1$@", "%1$lld"))})

    def test_unknown_named_arg_fails(self):
        with self.assertRaisesRegex(g.GenError, "unknown named argument"):
            run({AUTH: cat(k=single("{{a}}", "{{b}}"))})

    def test_pending_text_fails(self):
        with self.assertRaisesRegex(g.GenError, "PENDING"):
            run({AUTH: cat(k=single("PENDING review", "x"))})
        with self.assertRaisesRegex(g.GenError, "rick@example.com"):
            run({AUTH: cat(k=single("a", "mail rick@example.com"))})

    def test_non_empty_infoplist_fails(self):
        with self.assertRaisesRegex(g.GenError, "no longer empty"):
            run({INFOPLIST: cat(k=single("a", "b"))})

    def test_should_translate_false(self):
        r = run({AUTH: cat(**{"xxxx-xxxx": single("xxxx-xxxx", "xxxx-xxxx", shouldTranslate=False)})})
        self.assertIn('<string name="auth_xxxx_xxxx" translatable="false">xxxx-xxxx</string>', en_xml(r))
        self.assertNotIn("auth_xxxx_xxxx", nl_xml(r))

    def test_debug_only_key_goes_to_debug_file(self):
        r = run({APP: cat(**{"Debug route: %@": single("Debug route: %@", "Debugroute: %@"),
                             "Today": single("Today", "Vandaag")})})
        debug = en_xml(r, "core/ui/src/debug/res/values/strings_ios_debug.xml")
        debug_nl = r.files["core/ui/src/debug/res/values-nl/strings_ios_debug.xml"].decode("utf-8")
        self.assertIn('<string name="app_debug_route_x">Debug route: %1$s</string>', debug)
        self.assertIn("Debugroute: %1$s", debug_nl)
        self.assertNotIn("app_debug_route_x", en_xml(r))
        self.assertNotIn("app_today", debug)
        self.assertIn('name="app_today"', en_xml(r))
        self.assertIn("app\tDebug route: %@", r.lock)
        self.assertIn("app\tToday", r.lock)

    def test_untranslatable_names_are_reserved(self):
        r = run({PLAN: cat(Sleep=single("Sleep", "Slapen"))}, reserved={"plan_sleep"})
        self.assertEqual(r.lock["plan\tSleep"], "plan_sleep_" + g.sha6("plan\tSleep"))
        with self.assertRaisesRegex(g.GenError, "plan_sleep.*strings_untranslatable.xml.*locked"):
            run({PLAN: cat(Sleep=single("Sleep", "Slapen"))}, {"plan\tSleep": "plan_sleep"}, reserved={"plan_sleep"})

    def test_lint_ignores_applied_per_element(self):
        r = run({AUTH: cat(A=single("a", "b"), B=single("c", "d"))},
                lint_ignores={"auth_a": {"ids": ["PluralsCandidate"], "reason": "x"}})
        self.assertEqual(en_xml(r).count('tools:ignore="PluralsCandidate"'), 1)
        self.assertEqual(nl_xml(r).count('tools:ignore="PluralsCandidate"'), 1)

    def test_output_is_deterministic(self):
        catalogs = {AUTH: cat(A=single("a", "b"), B=single("c", "d"))}
        self.assertEqual(run(catalogs).files, run(catalogs).files)

    def test_file_header_and_format(self):
        r = run({AUTH: cat(A=single("a", "b"))})
        xml = en_xml(r)
        self.assertTrue(xml.startswith('<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERATED by tools/l10n/xcstrings2android.py from chronoflow-ios@f391a1fa. Do not edit; run make strings. -->\n'))
        self.assertTrue(xml.endswith("</resources>\n"))


def overlay_entry(**kw):
    e = {"name": "android_x", "en": "Hello", "nl": "Hallo", "replaces": [], "review": "needs review"}
    e.update(kw)
    return {"entries": [e]}


class OverlayTest(unittest.TestCase):
    def test_overlay_written_in_both_languages(self):
        r = run({AUTH: cat(A=single("a", "b"))}, overlay=overlay_entry())
        self.assertIn('<string name="android_x">Hello</string>', r.files["core/ui/src/main/res/values/strings_android.xml"].decode())
        self.assertIn("<!-- android: Hello (review: needs review) -->", r.files["core/ui/src/main/res/values-nl/strings_android.xml"].decode())
        self.assertIn(">Hallo</string>", r.files["core/ui/src/main/res/values-nl/strings_android.xml"].decode())

    def test_overlay_missing_dutch_fails(self):
        with self.assertRaisesRegex(g.GenError, "missing or empty nl"):
            run({}, overlay=overlay_entry(nl=" "))

    def test_overlay_unknown_replaced_key_fails(self):
        with self.assertRaisesRegex(g.GenError, "replaces unknown iOS key"):
            run({AUTH: cat(A=single("a", "b"))}, overlay=overlay_entry(replaces=[{"table": "auth", "key": "Gone"}]))

    def test_overlay_name_clash_fails(self):
        with self.assertRaisesRegex(g.GenError, "name clash"):
            run({AUTH: cat(A=single("a", "b"))}, {"auth\tA": "android_x"}, overlay=overlay_entry())
        with self.assertRaisesRegex(g.GenError, "name clash"):
            run({}, reserved={"android_x"}, overlay=overlay_entry())
        with self.assertRaisesRegex(g.GenError, "name must match"):
            run({}, overlay=overlay_entry(name="plain"))

    def test_overlay_validation_rules(self):
        with self.assertRaisesRegex(g.GenError, "positions differ"):
            run({}, overlay=overlay_entry(en="%@ %@", nl="%@"))
        with self.assertRaisesRegex(g.GenError, "review must be"):
            run({}, overlay=overlay_entry(review="maybe"))
        twice = overlay_entry(replaces=[{"table": "auth", "key": "A"}])
        twice["entries"].append(dict(twice["entries"][0], name="android_y"))
        with self.assertRaisesRegex(g.GenError, "already replaced"):
            run({AUTH: cat(A=single("a", "b"))}, overlay=twice)

    def test_replaced_keys_are_not_emitted_but_stay_locked(self):
        r = run({AUTH: cat(A=single("a", "b"), B=single("c", "d"))},
                overlay=overlay_entry(replaces=[{"table": "auth", "key": "A"}]))
        self.assertNotIn("auth_a", en_xml(r))
        self.assertIn('name="auth_b"', en_xml(r))
        self.assertEqual(r.lock["auth\tA"], "auth_a")


class MainTest(unittest.TestCase):
    def fake_read(self, ios, pin):
        return {AUTH: cat(A=single("a", "b"))}

    def test_missing_pin_exits_2(self):
        def read(ios, pin):
            raise g.PinNotFound(f"iOS pin {pin} not found in {ios}")

        with tempfile.TemporaryDirectory() as tmp:
            err = io.StringIO()
            with contextlib.redirect_stderr(err):
                code = g.main(["--root", tmp, "--ios", "/nowhere", "--pin", "0" * 40], read=read)
            self.assertEqual(code, 2)
            self.assertIn("iOS pin " + "0" * 40 + " not found in /nowhere", err.getvalue())
            self.assertEqual(list(Path(tmp).rglob("*")), [])

    def test_check_mode_detects_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(g.main(["--root", tmp, "--pin", "0" * 40], read=self.fake_read), 0)
            self.assertEqual(g.main(["--root", tmp, "--pin", "0" * 40, "--check"], read=self.fake_read), 0)
            target = Path(tmp) / "core/ui/src/main/res/values-nl/strings_ios.xml"
            target.write_bytes(target.read_bytes().replace(b">b<", b">c<"))
            err = io.StringIO()
            with contextlib.redirect_stderr(err):
                code = g.main(["--root", tmp, "--pin", "0" * 40, "--check"], read=self.fake_read)
            self.assertEqual(code, 1)
            self.assertIn("values-nl/strings_ios.xml", err.getvalue())
            self.assertIn("run make strings", err.getvalue())


if __name__ == "__main__":
    unittest.main()
