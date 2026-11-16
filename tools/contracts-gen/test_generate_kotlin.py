import contextlib
import io
import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

import generate_kotlin as g

SWIFT_PATH = 'Packages/ChronoFlowKit/Sources/AppCore/Contracts/Generated/TripPlan.swift'


def sources(api=None, **others):
    """Synthetic web sources: every PATHS key, Data/Api defines every root type."""
    definitions = {name: '{\nreadonly value: string,\n}' for name in g.ROOT_TYPES}
    definitions.update(api or {})
    result = {path: '' for path in g.PATHS}
    result['Data/Api'] = ''.join(f'export type {n} = {b};\n' for n, b in definitions.items())
    result.update(others)
    return result


def run_main(root, web, *extra):
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        code = g.main(['--web-repo', 'x', *extra], read=lambda repo: web, root=root)
    return code, out.getvalue(), err.getvalue()


class GeneratorTests(unittest.TestCase):
    def test_output_is_deterministic(self):
        web = sources()
        first = g.generate(web)
        self.assertEqual(first, g.generate(web))
        previous = Path.cwd()
        try:
            with tempfile.TemporaryDirectory() as directory:
                os.chdir(directory)
                self.assertEqual(first, g.generate(web))
        finally:
            os.chdir(previous)
        self.assertIn('    val value: String,', first)
        self.assertTrue(first.endswith('\n') and not first.endswith('\n\n'))

    def test_wizard_records_request_keys_and_fractional_bounds(self):
        result = g.generate(sources({
            'ApiTripInput': '{\nreadonly trip_end_kind: string,\nreadonly seat_lie_flat: boolean,\n}',
            'ApiTripReview': '{\nreadonly shape: ApiReviewPromptShape,\nreadonly answer: Record<string, boolean | number | string | null>,\n}',
            'ApiReviewPromptShape': '{\nreadonly min: number | null,\nreadonly max: number | null,\n}',
            'ApiTripInputSnapshot': '{\nreadonly airports: Record<string, ApiAirportSuggestion>,\n}',
            'ApiAirportSuggestion': '{\nreadonly timezone: string,\n}',
        }))
        self.assertIn('@SerialName("trip_end_kind") val tripEndKind: String', result)
        self.assertIn('@SerialName("seat_lie_flat") val seatLieFlat: Boolean', result)
        self.assertIn('val min: Double?', result)
        self.assertIn('val max: Double?', result)
        self.assertIn('val answer: Map<String, JsonPrimitive>', result)
        self.assertIn('val airports: Map<String, ApiAirportSuggestion>', result)

    def test_request_graph_encodes_nulls_but_omits_optional_keys(self):
        result = g.generate(sources({
            'ApiTripInput': '{\nreadonly child: RequestChild,\n}',
            'RequestChild': ('{\nreadonly required: string | null,\n'
                             'readonly optional?: string,\nreadonly optional_nullable?: string | null,\n}'),
            'ApiTripReview': '{\nreadonly response: string | null,\n}',
        }))
        self.assertIn('    val required: String?,\n', result)
        self.assertIn('    val optional: String? = null,\n', result)
        self.assertIn('@SerialName("optional_nullable") val optionalNullable: String? = null,', result)
        self.assertIn('    val response: String?,\n', result)

    def test_unknown_enum_fallback(self):
        result = g.generate(sources(Enums="export type CheckStatus = 'done' | 'skipped';\n"))
        self.assertIn('sealed interface CheckStatus : RawEnum {', result)
        self.assertIn('data class Unknown(', result)
        self.assertIn('else -> Unknown(raw)', result)
        self.assertIn('val known: List<CheckStatus> = listOf(Done, Skipped)', result)

    def test_unknown_literal_becomes_unknown_value(self):
        result = g.generate(sources(Enums="export type Mode = 'unknown' | 'light_ok' | '12' | 'return' | 'none';\n"))
        for case in ['UnknownValue', 'LightOK', 'Value12', 'Return', 'None']:
            self.assertIn(f'data object {case} : Mode', result)

    def test_redacted_field_prints_stars(self):
        result = g.generate(sources({
            'ApiTripReview': '{\nreadonly identity: ApiReviewIdentity,\n}',
            'ApiReviewIdentity': '{\nreadonly token: string,\nreadonly guestSecret: string | null,\n}',
        }))
        self.assertIn('override fun toString(): String =', result)
        self.assertIn('"ApiReviewIdentity(token=${token}, guestSecret=${if (guestSecret == null) "null" else "***"})"', result)

    def test_enum_case_collision_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'colliding enum cases'):
            g.generate(sources(Enums="export type Mode = 'a_b' | 'aB';\n"))

    def test_reserved_enum_case_is_rejected(self):
        for literal in ['Unknown', 'Serializer', 'Companion']:
            with self.assertRaisesRegex(ValueError, 'reserved enum case'):
                g.generate(sources(Enums=f"export type Mode = '{literal}' | 'x';\n"))

    def test_keyword_field_is_escaped(self):
        result = g.generate(sources({'ApiTripInput': '{\nreadonly return: string | null,\n}'}))
        self.assertIn('    val `return`: String?,\n', result)

    def test_rejects_unsupported_syntax(self):
        cases = {
            'generic': {'ApiTripInput': '{\nreadonly a: Array<string>,\n}'},
            'intersection': {'ApiTripInput': '{\nreadonly a: string,\n} & {\nreadonly b: string,\n}'},
            'union': {'ApiTripInput': '{\nreadonly a: string | number,\n}'},
            'params': {'ApiTripInput': '{\nreadonly b: BlockView,\n}', 'FlightParamsView': '{\nreadonly value: string,\n}',
                       'BlockView': '{\nreadonly params: FlightParamsView,\n}'},
            'bound': {'ApiTripInput': '{\nreadonly b: ApiReviewPromptShape,\n}',
                      'ApiReviewPromptShape': '{\nreadonly min: number,\n}'},
        }
        for label, api in cases.items():
            with self.subTest(label), self.assertRaises(ValueError):
                g.generate(sources(api))

    def test_duplicate_type_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'duplicate type'):
            g.generate(sources(Enums="export type Mode = 'a';\n", **{'Planner/Input': "export type Mode = 'b';\n"}))

    def test_screening_overlay(self):
        result = g.generate(sources(Enums="export type ScreeningQuestion = 'shift_work' | 'bipolar' | 'pregnancy';\n"))
        self.assertIn('data object ShiftWork : ScreeningQuestion', result)
        self.assertNotIn('Bipolar', result)
        self.assertNotIn('Pregnancy', result)

    def test_planted_change_fails_check(self):
        before = sources({'ApiTripInput': '{\nreadonly old_name: string,\n}'})
        after = sources({'ApiTripInput': '{\nreadonly new_name: string,\n}'})
        with tempfile.TemporaryDirectory() as root:
            self.assertEqual(run_main(root, before)[0], 0)
            self.assertEqual(run_main(root, before, '--check')[0], 0)
            code, _, err = run_main(root, after, '--check')
        self.assertEqual(code, 1)
        self.assertIn('-    @SerialName("old_name") val oldName: String,', err)
        self.assertIn('+    @SerialName("new_name") val newName: String,', err)

    def test_missing_repo_exits_two(self):
        saved = os.environ.pop('WEB_REPO', None)
        try:
            with contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(g.main([], read=lambda repo: {}), 2)
        finally:
            if saved:
                os.environ['WEB_REPO'] = saved

    def test_pin_comes_from_the_pin_file(self):
        self.assertEqual(g.PIN, (g.ROOT / 'tools/web-pin.txt').read_text().strip())
        self.assertRegex(g.PIN, r'^[0-9a-f]{40}$')


def swift_declarations(text):
    declarations = {}
    kind = name = None
    for line in text.splitlines():
        top = re.match(r'public (struct|enum|typealias) (\w+)', line)
        if top:
            kind, name = top.group(1), top.group(2)
            declarations[name] = (kind, [])
        elif kind == 'struct' and name:
            member = re.match(r'    public var `?(\w+)`?:', line)
            if member:
                declarations[name][1].append(member.group(1))
        elif kind == 'enum' and name:
            member = re.match(r'    case `?(\w+)`?(\(String\))?$', line)
            if member:
                declarations[name][1].append(member.group(1))
    return declarations


def kotlin_declarations(text):
    declarations = {}
    for match in re.finditer(r'^data class (\w+)\(\n(.*?)^\)', text, re.S | re.M):
        props = re.findall(r'^    (?:@SerialName\("[^"]+"\) )?val `?(\w+)`?:', match.group(2), re.M)
        declarations[match.group(1)] = ('struct', props)
    for match in re.finditer(r'^sealed interface (\w+) : RawEnum \{\n(.*?)^\}', text, re.S | re.M):
        cases = re.findall(r'^    data object (\w+) :', match.group(2), re.M)
        declarations[match.group(1)] = ('enum', cases)
    for match in re.finditer(r'^typealias (\w+) =', text, re.M):
        declarations[match.group(1)] = ('typealias', [])
    return declarations


class IosParityTests(unittest.TestCase):
    def test_matches_ios_generated_swift(self):
        ios, web = os.environ.get('IOS_REPO'), os.environ.get('WEB_REPO')
        if not ios or not web:
            self.fail('set IOS_REPO and WEB_REPO to run the iOS parity test (make exports them)')
        pin = (g.ROOT / 'tools/ios-pin.txt').read_text().split()[0]
        swift = subprocess.check_output(['git', '-C', ios, 'show', f'{pin}:{SWIFT_PATH}']).decode()
        expected = swift_declarations(swift)
        actual = kotlin_declarations(g.generate(g.read_sources(web)))
        self.assertEqual(sorted(actual), sorted(expected))
        counts = [sum(1 for k, _ in actual.values() if k == kind) for kind in ('struct', 'enum', 'typealias')]
        self.assertEqual(counts, [79, 50, 1])
        for name, (kind, members) in expected.items():
            self.assertEqual(actual[name][0], kind, name)
            if kind == 'struct':
                self.assertEqual(actual[name][1], members, name)
            elif kind == 'enum':
                cases = [c[:1].upper() + c[1:] if c != 'unknown' else 'Unknown' for c in members]
                self.assertEqual(actual[name][1] + ['Unknown'], cases, name)


if __name__ == '__main__':
    unittest.main()
