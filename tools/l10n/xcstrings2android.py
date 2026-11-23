#!/usr/bin/env python3
"""Turns the iOS String Catalogs at the pinned commit into Android string resources (en and nl).

Usage: python3 -I tools/l10n/xcstrings2android.py [--ios <path>] [--pin <sha>] [--check] [--root <dir>]

The only code that touches the iOS repo is read_catalogs(): `git cat-file -e` and `git show`.
"""
import argparse
import difflib
import hashlib
import json
import os
import pathlib
import re
import subprocess
import sys
import unicodedata
from collections import namedtuple
from xml.sax.saxutils import escape as xml_escape

REPO = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_IOS = "/Users/rickgout/Sites/chronoflow-ios"
KIT = "Packages/ChronoFlowKit/Sources"

# (catalog path in the iOS repo, resource prefix or None for a catalog that must stay empty, CopyTable name)
TABLES = [
    ("App/Localizable.xcstrings", "app", "App"),
    ("App/InfoPlist.xcstrings", None, None),
    (KIT + "/DesignSystem/Resources/Localizable.xcstrings", None, None),
    (KIT + "/Auth/Resources/Localizable.xcstrings", "auth", "Auth"),
    (KIT + "/Launch/Resources/Localizable.xcstrings", "launch", "Launch"),
    (KIT + "/Welcome/Resources/Localizable.xcstrings", "welcome", "Welcome"),
    (KIT + "/PlanUI/Resources/Localizable.xcstrings", "plan", "Plan"),
    (KIT + "/PlanUI/Resources/Screens.xcstrings", "screens", "Screens"),
    (KIT + "/PlanUI/Resources/Walkthrough.xcstrings", "walkthrough", "Walkthrough"),
    (KIT + "/Reminders/Resources/Reminders.xcstrings", "reminders", "Reminders"),
    (KIT + "/Settings/Resources/Settings.xcstrings", "settings", "Settings"),
    (KIT + "/Settings/Resources/Terms.xcstrings", "terms", "Terms"),
    (KIT + "/TripWizardUI/Resources/Localizable.xcstrings", "wizard", "Wizard"),
]

DEBUG_ONLY = {("app", "Debug route: %@")}
MAXLEN = 48
LOCK_PATH = "tools/l10n/names.lock.json"
OVERLAY_PATH = "tools/l10n/android-only.json"
PAGE_COPY_SOURCE = KIT + "/PlanUI/Resources/Localizable.xcstrings"
PAGE_COPY_PATH = "core/plan/src/main/resources/page_copy.json"
INDEX_PATH = "core/ui/src/main/kotlin/net/chronoflow/core/ui/l10n/generated/CopyIndex.generated.kt"
LINT_IGNORES_PATH = "tools/l10n/lint-ignores.json"
UNTRANSLATABLE_PATH = "core/ui/src/main/res/values/strings_untranslatable.xml"
MAIN = "core/ui/src/main/res"
DEBUG = "core/ui/src/debug/res"

PH = re.compile(r"\{\{(\w+)\}\}|%(\d+\$)?(lld|ld|d|@)")
TOKEN = re.compile(r"%%|\{\{(\w+)\}\}|%(\d+)\$(lld|ld|d|@)|%(lld|ld|d|@)|%")
QUANTITIES = {"one", "other"}
FORBIDDEN = ("PENDING", "rick@example.com")

Result = namedtuple("Result", "files lock")


class PinNotFound(Exception):
    pass


class GenError(Exception):
    pass


def read_catalogs(ios, pin):
    def git(*args):
        return subprocess.run(["git", "-C", str(ios), *args], capture_output=True)

    probe = git("cat-file", "-e", pin + "^{commit}")
    if probe.returncode != 0:
        raise PinNotFound(f"iOS pin {pin} not found in {ios}")
    catalogs = {}
    for path, _, _ in TABLES:
        shown = git("show", f"{pin}:{path}")
        if shown.returncode != 0:
            raise GenError(f"cannot read {path} at {pin[:8]}: {shown.stderr.decode('utf-8', 'replace').strip()}")
        catalogs[path] = json.loads(shown.stdout.decode("utf-8"))
    return catalogs


def slug(prefix, key):
    s = unicodedata.normalize("NFKD", key).encode("ascii", "ignore").decode().lower()
    s = re.sub(r"%(\d+\$)?l{0,2}d", "n", s).replace("%@", "x")
    s = re.sub(r"\{\{(\w+)\}\}", lambda m: re.sub(r"(?<!^)(?=[A-Z])", "_", m[1]).lower(), s)
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_") or "x"
    name = f"{prefix}_{s}"
    if len(name) > MAXLEN:
        name = name[: MAXLEN - 7].rstrip("_") + "_" + hashlib.sha1(key.encode()).hexdigest()[:6]
    return name


def sha6(text):
    return hashlib.sha1(text.encode()).hexdigest()[:6]


def arg_slots(key, en_values):
    """iOS %-args keep their positions; {{name}} args follow, by first use in the key, then in English."""
    pct = 0
    for v in en_values:
        auto = 0
        for m in PH.finditer(v):
            if m[3]:
                auto += 0 if m[2] else 1
                pct = max(pct, int(m[2][:-1]) if m[2] else auto)
    names = []
    for v in [key] + en_values:
        for m in PH.finditer(v):
            if m[1] and m[1] not in names:
                names.append(m[1])
    slots = {}
    if pct and "count" in names:
        names.remove("count")
        slots["count"] = 1
    slots.update({n: pct + i + 1 for i, n in enumerate(names)})
    return pct, slots


class Converted:
    """A value split into literal text and positional arguments."""

    def __init__(self, parts, args):
        self.parts = parts  # list of str (literal) or (idx, conv)
        self.args = args  # {idx: conv}

    @property
    def has_args(self):
        return bool(self.args)


def convert(where, value, slots):
    _, named = slots
    parts, args, auto, i = [], {}, 0, 0
    for m in TOKEN.finditer(value):
        parts.append(value[i : m.start()])
        i = m.end()
        tok = m[0]
        if tok in ("%%", "%"):
            parts.append("%")
            continue
        if m[1]:
            if m[1] not in named:
                raise GenError(f"{where}: unknown named argument {{{{{m[1]}}}}}")
            idx, conv = named[m[1]], "s"
        elif m[2]:
            idx, conv = int(m[2]), "s" if m[3] == "@" else "d"
        else:
            auto += 1
            idx, conv = auto, "s" if m[4] == "@" else "d"
        if args.get(idx, conv) != conv:
            raise GenError(f"{where}: argument {idx} used as both %s and %d")
        args[idx] = conv
        parts.append((idx, conv))
    parts.append(value[i:])
    return Converted(parts, args)


def render(conv, force_format):
    """Android resource text, before escaping."""
    out = []
    percent_literal = force_format or conv.has_args
    for p in conv.parts:
        if isinstance(p, tuple):
            out.append(f"%{p[0]}${p[1]}")
        else:
            out.append(p.replace("%", "%%") if percent_literal else p)
    return "".join(out)


def android_escape(v):
    v = xml_escape(v)
    v = v.replace("\\", "\\\\").replace("\n", "\\n").replace("'", "\\'").replace('"', '\\"')
    if v[:1] in ("@", "?"):
        v = "\\" + v

    def spaces(m):
        run = m.group(0)
        if m.start() == 0 or m.end() == len(v):
            return "\\u0020" * len(run)
        return run[0] + "\\u0020" * (len(run) - 1)

    return re.sub(" +", spaces, v)


def comment_text(text):
    t = xml_escape(text).replace("\n", "\\n")
    t = re.sub(r"-(?=-)", "- ", t)
    return t + " " if t.endswith("-") else t


def units(where, loc):
    if "stringUnit" in loc:
        return "single", loc["stringUnit"]["value"]
    variations = loc.get("variations", {})
    other = set(variations) - {"plural"}
    if other:
        raise GenError(f"{where}: unsupported variation {sorted(other)}")
    plural = variations.get("plural")
    if plural is None:
        raise GenError(f"{where}: no value")
    if set(plural) != QUANTITIES:
        raise GenError(f"{where}: plural quantities {sorted(plural)} are not exactly one and other")
    return "plural", {q: plural[q]["stringUnit"]["value"] for q in ("one", "other")}


def check_values(where, kind, v):
    vals = [v] if kind == "single" else list(v.values())
    for x in vals:
        if not x.strip():
            raise GenError(f"{where}: empty or whitespace-only value")
        for bad in FORBIDDEN:
            if bad in x:
                raise GenError(f"{where}: value contains {bad!r}")


def check_same_args(where, en, nl):
    """en and nl must use the same positions and conversions (union over plural items)."""

    def union(convs):
        merged = {}
        for c in convs:
            for idx, conv in c.args.items():
                if merged.setdefault(idx, conv) != conv:
                    raise GenError(f"{where}: argument {idx} changes conversion between items")
        return merged

    a, b = union(en), union(nl)
    if set(a) != set(b):
        raise GenError(f"{where}: argument positions differ between en {sorted(a)} and nl {sorted(b)}")
    for idx in a:
        if a[idx] != b[idx]:
            raise GenError(f"{where}: argument {idx} is %{a[idx]} in en and %{b[idx]} in nl")


def arg_labels(key, slots, positions):
    """Names the positional arguments in order: {{name}}, count, or %N for unnamed Apple args."""
    by_idx = {idx: name for name, idx in slots[1].items()}
    return [by_idx.get(i, f"%{i}") for i in positions]


def lock_name(prefix, key, lock, used):
    lid = f"{prefix}\t{key}"
    if lid in lock:
        return lock[lid]
    name = slug(prefix, key)
    if name in used:
        name = name[: MAXLEN - 7].rstrip("_") + "_" + sha6(lid)
    lock[lid] = name
    used.add(name)
    return name


class Item:
    def __init__(self, prefix, table, key, name, kind, en, nl, translatable, debug, args):
        self.prefix, self.table, self.key, self.name = prefix, table, key, name
        self.kind, self.en, self.nl = kind, en, nl  # en/nl: Converted or {quantity: Converted}
        self.translatable, self.debug, self.args = translatable, debug, args


def process_tables(catalogs, lock, used):
    items = []
    for path, prefix, table in TABLES:
        strings = catalogs.get(path, {}).get("strings", {})
        if prefix is None:
            if strings:
                raise GenError(f"{path} is no longer empty: decide how Android uses it")
            continue
        for key in sorted(strings):
            if key.endswith("_one") and key[:-4] in strings:
                continue
            where = f"{path}: {key!r}"
            entry = strings[key]
            loc = entry.get("localizations", {})
            translatable = entry.get("shouldTranslate", True) is not False
            if "en" not in loc:
                raise GenError(f"{where}: missing en")
            if translatable and "nl" not in loc:
                raise GenError(f"{where}: missing nl")
            name = lock_name(prefix, key, lock, used)
            langs = ("en", "nl") if translatable else ("en",)
            forms = {l: units(where, loc[l]) for l in langs}
            one = strings.get(key + "_one")
            if one:
                for l in langs:
                    if forms[l][0] != "single":
                        raise GenError(f"{where}: a _one pair needs a plain base value")
                    ol = one.get("localizations", {}).get(l)
                    if ol is None:
                        raise GenError(f"{where}: _one key is missing {l}")
                    forms[l] = ("plural", {"one": units(where, ol)[1], "other": forms[l][1]})
            for l in langs:
                check_values(where + f" [{l}]", *forms[l])
            if translatable and forms["en"][0] != forms["nl"][0]:
                raise GenError(f"{where}: en is {forms['en'][0]} but nl is {forms['nl'][0]}")
            kind = forms["en"][0]
            en_vals = [forms["en"][1]] if kind == "single" else list(forms["en"][1].values())
            slots = arg_slots(key, en_vals)
            conv = {}
            for l in langs:
                v = forms[l][1]
                conv[l] = (
                    convert(f"{where} [{l}]", v, slots)
                    if kind == "single"
                    else {q: convert(f"{where} [{l}/{q}]", t, slots) for q, t in v.items()}
                )
            if translatable:
                as_list = lambda c: [c] if kind == "single" else list(c.values())
                check_same_args(where, as_list(conv["en"]), as_list(conv["nl"]))
            positions = sorted({i for c in ([conv["en"]] if kind == "single" else conv["en"].values()) for i in c.args})
            if positions != list(range(1, len(positions) + 1)):
                raise GenError(f"{where}: argument positions {positions} are not contiguous")
            items.append(
                Item(prefix, table, key, name, kind, conv["en"], conv.get("nl"), translatable,
                     (prefix, key) in DEBUG_ONLY, arg_labels(key, slots, positions))
            )
    return items


OVERLAY_NAME = re.compile(r"^android_[a-z0-9_]+$")


def process_overlay(overlay, items, lock_names, reserved):
    """Validates the Android-only entries and converts them; returns (entries, replaced {(prefix, key): entry})."""
    known = {(i.prefix, i.key) for i in items}
    taken = set(lock_names) | set(reserved)
    seen, replaced, entries = set(), {}, []
    for e in overlay.get("entries", []):
        name = e.get("name", "")
        where = f"{OVERLAY_PATH}: {name!r}"
        if not OVERLAY_NAME.match(name) or len(name) > MAXLEN:
            raise GenError(f"{where}: name must match android_[a-z0-9_]+ and be at most {MAXLEN} characters")
        if name in seen or name in taken:
            raise GenError(f"{where}: name clash")
        seen.add(name)
        for lang in ("en", "nl"):
            if not str(e.get(lang, "")).strip():
                raise GenError(f"{where}: missing or empty {lang}")
            check_values(f"{where} [{lang}]", "single", e[lang])
        if e.get("review") not in ("needs review", "approved"):
            raise GenError(f"{where}: review must be 'needs review' or 'approved'")
        slots = arg_slots(e["en"], [e["en"]])
        en = convert(f"{where} [en]", e["en"], slots)
        nl = convert(f"{where} [nl]", e["nl"], slots)
        check_same_args(where, [en], [nl])
        positions = sorted(en.args)
        if positions != list(range(1, len(positions) + 1)):
            raise GenError(f"{where}: argument positions {positions} are not contiguous")
        for r in e.get("replaces", []):
            ref = (r["table"], r["key"])
            if ref not in known:
                raise GenError(f"{where}: replaces unknown iOS key {ref}")
            if ref in replaced:
                raise GenError(f"{where}: {ref} is already replaced by {replaced[ref]['name']}")
            replaced[ref] = e
        entries.append(Item("android", "Android", e["en"], name, "single", en, nl, True, False,
                            arg_labels(e["en"], slots, positions)))
        entries[-1].review = e["review"]
        entries[-1].replaces = e.get("replaces", [])
    return entries, replaced


def header(pin, source):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"<!-- GENERATED by tools/l10n/xcstrings2android.py from {source}. Do not edit; run make strings. -->\n"
        '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources,Typos">\n'
    )


def render_item(item, lang, comment, lint_ignores):
    ignore = lint_ignores.get(item.name)
    extra = f' tools:ignore="{",".join(ignore["ids"])}"' if ignore else ""
    value = item.en if lang == "en" else item.nl
    out = f"    <!-- {comment_text(comment)} -->\n"
    if item.kind == "single":
        fmt = ""
        if not value.has_args and any("%" in p for p in value.parts if isinstance(p, str)):
            fmt = ' formatted="false"'
        tr = ' translatable="false"' if not item.translatable else ""
        out += f'    <string name="{item.name}"{tr}{fmt}{extra}>{android_escape(render(value, False))}</string>\n'
    else:
        out += f'    <plurals name="{item.name}"{extra}>\n'
        for q in ("one", "other"):
            out += f'        <item quantity="{q}">{android_escape(render(value[q], True))}</item>\n'
        out += "    </plurals>\n"
    return out


def xml_file(pin, source, items, lang, lint_ignores):
    body = "".join(
        render_item(i, lang, f"{i.prefix}: {i.key}", lint_ignores)
        for i in items
        if lang == "en" or i.translatable
    )
    return (header(pin, source) + body + "</resources>\n").encode("utf-8")


def build(catalogs, lock, overlay=None, reserved=frozenset(), pin="0" * 40, lint_ignores=None):
    lint_ignores = lint_ignores or {}
    lock = dict(lock)
    for lid, name in lock.items():
        if name in reserved:
            raise GenError(f'"{name}" is defined in strings_untranslatable.xml and locked for {lid!r}')
    overlay_names = [e.get("name", "") for e in (overlay or {}).get("entries", [])]
    used = set(lock.values()) | set(reserved) | set(overlay_names)
    items = process_tables(catalogs, lock, used)
    android, replaced = process_overlay(overlay or {}, items, set(lock.values()), reserved)
    names = [i.name for i in items]
    if len(names) != len(set(names)):
        raise GenError("duplicate resource name in the output")
    files = {}
    src = f"chronoflow-ios@{pin[:8]}"
    main_items = [i for i in items if not i.debug and (i.prefix, i.key) not in replaced]
    debug_items = [i for i in items if i.debug]
    for lang, folder in (("en", "values"), ("nl", "values-nl")):
        files[f"{MAIN}/{folder}/strings_ios.xml"] = xml_file(pin, src, main_items, lang, lint_ignores)
        files[f"{DEBUG}/{folder}/strings_ios_debug.xml"] = xml_file(pin, src, debug_items, lang, lint_ignores)
    if overlay is not None:
        for lang, folder in (("en", "values"), ("nl", "values-nl")):
            body = "".join(
                render_item(i, lang, f"android: {i.key} (review: {i.review})", lint_ignores) for i in android
            )
            files[f"{MAIN}/{folder}/strings_android.xml"] = (
                header(pin, OVERLAY_PATH) + body + "</resources>\n"
            ).encode("utf-8")
    files[INDEX_PATH] = copy_index(pin, items, android, replaced)
    if PAGE_COPY_SOURCE in catalogs:
        files[PAGE_COPY_PATH] = page_copy(catalogs, pin)
    lock_text = json.dumps(dict(sorted(lock.items())), ensure_ascii=False, indent=1) + "\n"
    files[LOCK_PATH] = lock_text.encode("utf-8")
    return Result(files, lock)


def page_copy(catalogs, pin):
    """The PlanUI catalog as a JVM runtime table for `:core:plan`; a missing en means en = key, a missing nl fails."""
    strings = catalogs[PAGE_COPY_SOURCE]["strings"]
    entries = {}
    for key in sorted(strings):
        loc = strings[key].get("localizations", {})
        if "nl" not in loc:
            raise GenError(f"{PAGE_COPY_SOURCE}: {key!r}: missing nl")
        entry = {}
        for lang in ("en", "nl"):
            if lang not in loc:
                entry[lang] = key
                continue
            kind, value = units(f"{PAGE_COPY_SOURCE}: {key!r} [{lang}]", loc[lang])
            entry[lang] = value
        entries[key] = entry
    doc = {"source": PAGE_COPY_SOURCE, "pin": pin[:8], "entries": entries}
    return (json.dumps(doc, ensure_ascii=False, indent=1, sort_keys=True) + "\n").encode("utf-8")


def kotlin_literal(text):
    out = []
    for ch in text:
        if ch == "\\":
            out.append("\\\\")
        elif ch == '"':
            out.append('\\"')
        elif ch == "$":
            out.append("\\$")
        elif ch == "\n":
            out.append("\\n")
        elif ord(ch) < 0x20 or ord(ch) == 0x7F:
            out.append("\\u%04X" % ord(ch))
        else:
            out.append(ch)
    return '"' + "".join(out) + '"'


def kotlin_entry(item):
    ref = "plurals" if item.kind == "plural" else "string"
    args = "emptyList()" if not item.args else "listOf(" + ", ".join(kotlin_literal(a) for a in item.args) + ")"
    return f"CopyEntry(R.{ref}.{item.name}, {'true' if item.kind == 'plural' else 'false'}, {args})"


def copy_index(pin, items, android, replaced):
    """CopyIndex.generated.kt: every emitted main key, plus single-key overlay redirects."""
    tables = [(t, p) for _, p, t in TABLES if p]
    rows = {t: [] for t, _ in tables}
    for i in items:
        if i.debug:
            continue
        entry = replaced.get((i.prefix, i.key))
        if entry is None:
            rows[i.table].append((i.key, i))
        elif len(entry["replaces"]) == 1:
            rows[i.table].append((i.key, next(a for a in android if a.name == entry["name"])))
    lines = [
        f"// GENERATED by tools/l10n/xcstrings2android.py from chronoflow-ios@{pin[:8]}. Do not edit; run make strings.",
        "package net.chronoflow.core.ui.l10n.generated",
        "",
        "import net.chronoflow.core.ui.R",
        "",
        "enum class CopyTable { " + ", ".join([t for t, _ in tables] + ["Android"]) + " }",
        "",
        "/** [args] names the positional arguments in order: `{{name}}` names, `count` for the count special case, `%N` for unnamed Apple args. */",
        "class CopyEntry(val resId: Int, val isPlural: Boolean, val args: List<String>)",
        "",
        "object CopyIndex {",
        "    fun find(table: CopyTable, key: String): CopyEntry? =",
        "        when (table) {",
    ]
    for t, _ in tables:
        lines.append(f"            CopyTable.{t} -> {t[0].lower() + t[1:]}(key)")
    lines.append("            CopyTable.Android -> android(key)")
    lines.append("        }")
    rows["Android"] = [(a.key, a) for a in android]
    for t in [t for t, _ in tables] + ["Android"]:
        lines += ["", f"    private fun {t[0].lower() + t[1:]}(key: String): CopyEntry? =", "        when (key) {"]
        for key, item in rows[t]:
            lines.append(f"            {kotlin_literal(key)} -> {kotlin_entry(item)}")
        lines += ["            else -> null", "        }"]
    lines += ["}", ""]
    return "\n".join(lines).encode("utf-8")


def count_line(path, data):
    text = data.decode("utf-8")
    if path.endswith(".xml"):
        return f"{path}: {text.count('<string ')} strings, {text.count('<plurals ')} plurals"
    return f"{path}: {len(data)} bytes"


def load_inputs(root):
    lock_file = root / LOCK_PATH
    lock = json.loads(lock_file.read_text(encoding="utf-8")) if lock_file.exists() else {}
    ignores_file = root / LINT_IGNORES_PATH
    ignores = json.loads(ignores_file.read_text(encoding="utf-8")) if ignores_file.exists() else {}
    untranslatable = root / UNTRANSLATABLE_PATH
    reserved = set()
    if untranslatable.exists():
        reserved = set(re.findall(r'name="([^"]+)"', untranslatable.read_text(encoding="utf-8")))
    overlay_file = root / OVERLAY_PATH
    overlay = json.loads(overlay_file.read_text(encoding="utf-8")) if overlay_file.exists() else None
    return lock, reserved, ignores, overlay


def main(argv=None, read=read_catalogs):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--ios", default=os.environ.get("IOS_REPO", DEFAULT_IOS))
    ap.add_argument("--pin")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--root", help="output root (tests only)")
    a = ap.parse_args(argv)
    root = pathlib.Path(a.root) if a.root else REPO
    pin = a.pin or (REPO / "tools/ios-pin.txt").read_text(encoding="utf-8").strip()
    try:
        catalogs = read(a.ios, pin)
        lock, reserved, ignores, overlay = load_inputs(root)
        result = build(catalogs, lock, overlay, reserved=reserved, pin=pin, lint_ignores=ignores)
    except PinNotFound as e:
        print(str(e), file=sys.stderr)
        return 2
    except GenError as e:
        print(f"xcstrings2android: {e}", file=sys.stderr)
        return 1
    if a.check:
        bad = []
        for rel, data in sorted(result.files.items()):
            target = root / rel
            have = target.read_bytes() if target.exists() else b""
            if have != data:
                bad.append(rel)
                diff = difflib.unified_diff(
                    have.decode("utf-8").splitlines(), data.decode("utf-8").splitlines(),
                    fromfile=f"committed/{rel}", tofile=f"generated/{rel}", lineterm="",
                )
                print("\n".join(list(diff)[:20]), file=sys.stderr)
        if bad:
            print("xcstrings2android: stale generated output: " + ", ".join(bad), file=sys.stderr)
            print("run make strings", file=sys.stderr)
            return 1
        return 0
    for rel, data in sorted(result.files.items()):
        target = root / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        with open(target, "wb") as f:
            f.write(data)
        print(count_line(rel, data))
    return 0


if __name__ == "__main__":
    sys.exit(main())
