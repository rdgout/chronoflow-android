#!/usr/bin/env python3
"""Translate the pinned web trip DTO dependency graph into Kotlin; reject unsupported TS syntax.

Port of the iOS Tools/contracts-gen/generate.py. Only `git show <pin>:<path>` touches the web repo.
"""
import argparse
import difflib
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PIN = (ROOT / 'tools/web-pin.txt').read_text().strip()
TARGET = 'core/model/src/main/kotlin/net/chronoflow/core/model/contracts/TripPlan.generated.kt'
PACKAGE = 'net.chronoflow.core.model.contracts'
PATHS = ['Data/Api', 'Data/Trips', 'Data/Plan', 'Planner/Output', 'Planner/Input', 'Enums', 'Planning/Copy']
RENAMES = {'ApiTripListItem': 'TripListItem', 'ApiTripDetail': 'TripDetail',
           'CheckStateView': 'CheckState', 'CheckMarkView': 'CheckMark', 'CheckKeyView': 'CheckKeyView',
           'TripCardProgress': 'TripProgress', 'GutterTickView': 'TickView'}
OVERRIDES = {
    ('BlockView', 'params'): ('FlightParamsView | LayoverParamsView | TransferParamsView | MovementBreakParamsView | ZoneChangeParamsView | SleepParamsView | NapParamsView | RestParamsView | AvoidLightParamsView | SeekLightParamsView | LightOkParamsView | LightNeutralParamsView | CaffeineServingParamsView | CaffeineOkParamsView | CaffeineRoutineParamsView | NoCaffeineParamsView | CaffeineServingDrinksParamsView | CaffeineOkDrinksParamsView | NoCaffeineDrinksParamsView | NoteBlockParamsView', 'BlockParams'),
    ('CopyArg', 'value'): ('string | number', 'CopyArgValue'),
    ('CopyLine', 'args'): ('(CopyArg| CaffeineCopyArg)[]', 'List<CopyArgument>'),
}
# Types whose serializer delegates to an internal <Name>Wire twin, as the iOS init(from:) hooks.
CUSTOM = {'BlockView', 'PlanView', 'ApiTripReview', 'ApiReviewPrompt'}
ROOT_TYPES = ['ApiTripListItem', 'ApiTripDetail', 'ApiTripInput', 'ApiTripReview',
              'ApiTripSaveResult', 'ApiTripInputSnapshot', 'ApiAirportSearchResults']
REQUEST_ROOT_TYPES = ['ApiTripInput']
SWIFT_KEYWORDS = {'public', 'private', 'internal', 'return', 'case', 'default', 'extension', 'repeat', 'in', 'is', 'switch', 'where', 'throw', 'throws', 'try', 'false', 'true', 'none', 'operator'}
KOTLIN_KEYWORDS = {'as', 'break', 'class', 'continue', 'do', 'else', 'false', 'for', 'fun', 'if', 'in', 'interface', 'is', 'null', 'object', 'package', 'return', 'super', 'this', 'throw', 'true', 'try', 'typealias', 'typeof', 'val', 'var', 'when', 'while'}
# Secrets never reach a log through toString (AGENTS.md "Secrets").
REDACTED = {('ApiReviewIdentity', 'guestSecret')}
RESERVED_CASES = {'Unknown', 'Serializer', 'Companion'}
IMPORTS = {
    'KSerializer': 'kotlinx.serialization.KSerializer',
    'SerialName': 'kotlinx.serialization.SerialName',
    'Serializable': 'kotlinx.serialization.Serializable',
    'SerializationException': 'kotlinx.serialization.SerializationException',
    'SerialDescriptor': 'kotlinx.serialization.descriptors.SerialDescriptor',
    'Decoder': 'kotlinx.serialization.encoding.Decoder',
    'Encoder': 'kotlinx.serialization.encoding.Encoder',
    'Json': 'kotlinx.serialization.json.Json',
    'JsonDecoder': 'kotlinx.serialization.json.JsonDecoder',
    'JsonElement': 'kotlinx.serialization.json.JsonElement',
    'JsonEncoder': 'kotlinx.serialization.json.JsonEncoder',
    'JsonNull': 'kotlinx.serialization.json.JsonNull',
    'JsonObject': 'kotlinx.serialization.json.JsonObject',
    'JsonPrimitive': 'kotlinx.serialization.json.JsonPrimitive',
    'jsonObject': 'kotlinx.serialization.json.jsonObject',
}


def swift_identifier(value):
    words = re.split(r'[_-]', value)
    name = words[0] + ''.join(w[:1].upper() + w[1:] for w in words[1:])
    name = {'lightOk': 'lightOK', 'caffeineOk': 'caffeineOK'}.get(name, name)
    if name[:1].isdigit():
        name = 'value' + name
    return f'`{name}`' if name in SWIFT_KEYWORDS else name


def identifier(value):
    """Kotlin property name: the Swift identifier, backticked for Kotlin hard keywords."""
    name = swift_identifier(value).strip('`')
    return f'`{name}`' if name in KOTLIN_KEYWORDS else name


def case_name(literal):
    if literal == 'unknown':
        return 'UnknownValue'
    name = swift_identifier(literal).strip('`')
    return name[:1].upper() + name[1:]


def read_sources(repo, pin=PIN):
    sources = {}
    for path in PATHS:
        sources[path] = subprocess.check_output(
            ['git', '-C', str(repo), 'show', f'{pin}:resources/js/types/App/{path}/index.ts']).decode()
    return sources


def generate(sources):
    definitions = {}
    for path in PATHS:
        declarations = re.findall(r'export type (\w+) = (.*?);', sources[path], re.S)
        for name, body in declarations:
            if name in definitions:
                raise ValueError(f'duplicate type: {name}')
            definitions[name] = body.strip()
    # Phase 19 leaves the planner enum unchanged; the mobile contract only asks shift work.
    definitions['ScreeningQuestion'] = "'shift_work'"
    selected = set()

    def include(name, graph):
        if name in graph:
            return
        if name not in definitions:
            raise ValueError(f'unsupported type: {name}')
        graph.add(name)
        body = definitions[name]
        for ref in re.findall(r'\b[A-Z]\w*\b', re.sub(r"'[^']*'", '', body)):
            if ref != 'Record':
                include(ref, graph)
    for name in ROOT_TYPES:
        include(name, selected)
    # Check every planner/output and domain enum, including currently unused literals.
    for path in ['Planner/Output', 'Enums']:
        for name in re.findall(r'export type (\w+) =', sources[path]):
            include(name, selected)

    def kotlin_type(ts):
        ts = ts.strip()
        record = re.fullmatch(r'Record<string, (.+)>', ts)
        if record:
            return 'Map<String, ' + kotlin_type(record[1]) + '>'
        if ts == 'boolean | number | string | null':
            return 'JsonPrimitive'
        if ts.endswith(' | null'):
            return kotlin_type(ts[:-7]) + '?'
        if ts.endswith('[]'):
            return 'List<' + kotlin_type(ts[:-2]) + '>'
        primitives = {'string': 'String', 'number': 'Int', 'boolean': 'Boolean', 'object': 'JsonObject'}
        if ts in primitives:
            return primitives[ts]
        if ts in selected:
            return RENAMES.get(ts, ts)
        raise ValueError(f'unsupported field type: {ts}')

    output = []
    for name in sorted(selected):
        body, kname = definitions[name], RENAMES.get(name, name)
        if re.fullmatch(r"'[^']+'(?:\s*\|\s*'[^']+')*", body):
            literals = re.findall(r"'([^']+)'", body)
            cases = [case_name(v) for v in literals]
            if len(set(cases)) != len(cases):
                raise ValueError(f'colliding enum cases: {name}')
            for case in cases:
                if case in RESERVED_CASES:
                    raise ValueError(f'reserved enum case: {name}.{case}')
            serial = f'{PACKAGE}.{kname}'
            lines = [f'@Serializable(with = {kname}.Serializer::class)', f'sealed interface {kname} : RawEnum {{']
            for case, literal in zip(cases, literals):
                lines += [f'    data object {case} : {kname} {{', f'        override val raw: String = "{literal}"', '    }', '']
            lines += [f'    data class Unknown(', '        override val raw: String,', f'    ) : {kname}', '',
                      '    companion object {',
                      f'        val known: List<{kname}> = listOf({", ".join(cases)})', '',
                      f'        fun of(raw: String): {kname} =', '            when (raw) {']
            lines += [f'                "{v}" -> {c}' for v, c in zip(literals, cases)]
            lines += ['                else -> Unknown(raw)', '            }', '    }', '',
                      f'    object Serializer : RawEnumSerializer<{kname}>("{serial}", {{ {kname}.of(it) }})', '}']
        elif body == 'object':
            lines = [f'typealias {kname} = JsonObject']
        elif body.startswith('{') and body.endswith('}'):
            fields = []
            for field in body[1:-1].strip().splitlines():
                match = re.fullmatch(r'readonly (\w+)(\?)?: (.+),', field.strip())
                if not match:
                    raise ValueError(f'unsupported declaration in {name}: {field}')
                key, optional, ts = match.groups()
                if (name, key) in OVERRIDES:
                    expected, kind = OVERRIDES[name, key]
                    if ts != expected:
                        raise ValueError(f'hand-written union changed: {name}.{key}')
                else:
                    kind = kotlin_type(ts)
                if name == 'ApiReviewPromptShape' and key in {'min', 'max'}:
                    if ts != 'number | null':
                        raise ValueError(f'prompt numeric bound changed: {key}')
                    kind = 'Double?'
                if optional and not kind.endswith('?'):
                    kind += '?'
                fields.append((key, kind, bool(optional)))

            def params(wire):
                out = []
                for key, kind, optional in fields:
                    prop = identifier(key)
                    if wire and name == 'BlockView' and key == 'params':
                        kind = 'JsonElement'
                    serial = f'@SerialName("{key}") ' if prop.strip('`') != key else ''
                    out.append(f'    {serial}val {prop}: {kind}{" = null" if optional else ""},')
                return out

            redacted = to_string(name, kname, fields)
            if name in CUSTOM:
                lines = [f'@Serializable(with = {kname}.Serializer::class)', f'data class {kname}(']
                lines += params(False) + [') {']
                lines += redacted + ([''] if redacted else [])
                lines += custom_serializer(name, kname, fields)
                lines += ['}', '', '@Serializable', f'internal data class {kname}Wire(']
                lines += params(True) + [')']
            elif redacted:
                lines = ['@Serializable', f'data class {kname}('] + params(False) + [') {'] + redacted + ['}']
            else:
                lines = ['@Serializable', f'data class {kname}('] + params(False) + [')']
        else:
            raise ValueError(f'unsupported type: {name} = {body}')
        output.append('\n'.join(lines))
    text = '\n\n'.join(output) + '\n'
    imports = sorted(path for symbol, path in IMPORTS.items() if re.search(rf'\b{symbol}\b', text))
    header = (f'// Generated by tools/contracts-gen/generate_kotlin.py from web {PIN}, with the Phase 19 mobile contract overlay. Do not edit.\n'
              f'package {PACKAGE}\n\n')
    return header + ''.join(f'import {i}\n' for i in imports) + '\n' + text


def to_string(name, kname, fields):
    if not any(n == name for n, _ in REDACTED):
        return []
    parts = []
    for key, kind, _ in fields:
        prop = identifier(key).strip('`')
        ref = '${`' + prop + '`}' if prop in KOTLIN_KEYWORDS else '${' + prop + '}'
        if (name, key) in REDACTED:
            ref = '${if (' + prop + ' == null) "null" else "***"}' if kind.endswith('?') else '***'
        parts.append(f'{prop}={ref}')
    return ['    override fun toString(): String =', f'        "{kname}({", ".join(parts)})"']


def custom_serializer(name, kname, fields):
    """The nested KSerializer of a CUSTOM type: decode through the Wire twin, then apply the iOS hook."""
    def decoded(key):
        prop = identifier(key)
        if name == 'BlockView' and key == 'params':
            return 'BlockParams.decode(wire.type, wire.params, json(decoder))'
        if name == 'PlanView' and key == 'prompts':
            return f'wire.{prop}.filter {{ ProfilePreferences.isSupported(it) }}'
        if name == 'ApiTripReview' and key == 'prompts':
            return f'ProfilePreferences.reviewPrompts(wire.{prop})'
        if name == 'ApiReviewPrompt' and key == 'answer':
            return 'ProfilePreferences.promptAnswers(wire.answer.filterKeys { it in wire.fields })'
        return f'wire.{prop}'

    def encoded(key):
        prop = identifier(key)
        if name == 'BlockView' and key == 'params':
            return 'value.params.encode(json(encoder))'
        if name == 'ApiReviewPrompt' and key == 'answer':
            return 'ProfilePreferences.promptAnswers(value.answer)'
        return f'value.{prop}'

    lines = ['    object Serializer : KSerializer<' + kname + '> {',
             '        override val descriptor: SerialDescriptor =',
             f'            SerialDescriptor("{PACKAGE}.{kname}", {kname}Wire.serializer().descriptor)', '',
             f'        override fun deserialize(decoder: Decoder): {kname} {{',
             f'            val wire = decoder.decodeSerializableValue({kname}Wire.serializer())',
             f'            return {kname}(']
    lines += [f'                {identifier(k)} = {decoded(k)},' for k, _, _ in fields]
    lines += ['            )', '        }', '',
              f'        override fun serialize(encoder: Encoder, value: {kname}) {{',
              f'            val wire = {kname}Wire(']
    lines += [f'                {identifier(k)} = {encoded(k)},' for k, _, _ in fields]
    lines += ['            )']
    if name == 'ApiReviewPrompt':
        omitted = ', '.join(f'"{k}"' for k, kind, _ in fields if kind.endswith('?'))
        lines += ['            val json = json(encoder)',
                  f'            val element = json.encodeToJsonElement({kname}Wire.serializer(), wire).jsonObject',
                  f'            val omitted = setOf({omitted})',
                  '            encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(element.filterNot { it.key in omitted && it.value is JsonNull }))']
    else:
        lines += [f'            encoder.encodeSerializableValue({kname}Wire.serializer(), wire)']
    lines += ['        }']
    lines += ['', '        private fun json(coder: Any): Json =',
              '            when (coder) {',
              '                is JsonDecoder -> coder.json',
              '                is JsonEncoder -> coder.json',
              f'                else -> throw SerializationException("{kname} supports JSON only")',
              '            }',
              '    }']
    return lines


def main(argv=None, read=read_sources, root=ROOT):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--web-repo', default=os.environ.get('WEB_REPO'))
    parser.add_argument('--ios-repo', default=os.environ.get('IOS_REPO'))
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args(argv)
    if not args.web_repo:
        print('set WEB_REPO or pass --web-repo', file=sys.stderr)
        return 2
    target = Path(root) / TARGET
    expected = generate(read(args.web_repo))
    if args.check:
        actual = target.read_text() if target.exists() else ''
        if actual != expected:
            sys.stderr.writelines(difflib.unified_diff(actual.splitlines(True), expected.splitlines(True), fromfile=str(target), tofile='regenerated'))
            return 1
        print(f'Contracts match web {PIN}; generated fields and enum literals verified.')
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(expected)
    return 0


if __name__ == '__main__':
    sys.exit(main())
