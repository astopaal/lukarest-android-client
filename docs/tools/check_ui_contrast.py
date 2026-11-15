"""Check the color pairs used by the migrated shared Compose components."""

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
colors_source = (ROOT / "app/src/main/java/com/lukasoft/lukarest/ui/theme/Color.kt").read_text(encoding="utf-8")
colors = {name: value for name, value in re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", colors_source)}
for name, bg, fg in re.findall(r"val (Pill\w+) = PillColor\(bg = Color\(0xFF([0-9A-Fa-f]{6})\), fg = Color\(0xFF([0-9A-Fa-f]{6})\)\)", colors_source):
    colors[name + ".bg"] = bg
    colors[name + ".fg"] = fg


def rgb(value):
    return tuple(int(value[i:i + 2], 16) / 255 for i in (0, 2, 4))


def luminance(value):
    channels = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in value]
    return sum(c * w for c, w in zip(channels, (0.2126, 0.7152, 0.0722)))


def contrast(a, b):
    low, high = sorted((luminance(a), luminance(b)))
    return (high + 0.05) / (low + 0.05)


results = []


def record(name, ratios, minimum=4.5):
    ratio = min(ratios)
    results.append({"pair": name, "ratio": round(ratio, 3), "minimum": minimum, "pass": ratio >= minimum})


for foreground in ("TextMain", "TextSoft", "TextMuted"):
    for background in ("White", "BgBoard", "BgScreenTop"):
        record(f"{foreground} / {background}", [contrast(rgb(colors[foreground]), rgb(colors[background]))])
for pill in ("PillBlue", "PillSky", "PillOk", "PillWarn", "PillErr", "PillNeutral"):
    record(pill, [contrast(rgb(colors[pill + ".fg"]), rgb(colors[pill + ".bg"]))])
for prefix in ("Btn", "Staff", "Boss"):
    start = rgb(colors[prefix + "GradStart"])
    end = rgb(colors[prefix + "GradEnd"])
    points = [tuple(a + (b - a) * i / 100 for a, b in zip(start, end)) for i in range(101)]
    record(f"White / {prefix}Gradient (101 samples)", [contrast(rgb(colors["White"]), point) for point in points])
    initials = [tuple(0.35 * a + 0.65 * b for a, b in zip(rgb(colors["BossGradStart"]), point)) for point in points]
    record(f"TopPill initials / {prefix}Gradient", [contrast(rgb(colors["White"]), point) for point in initials])
record("Progress / track", [contrast(rgb(colors[token]), rgb(colors["ProgTrack"])) for token in ("BtnGradStart", "BtnGradEnd")], minimum=3)
record("Snackbar success icon / surface", [contrast(rgb(colors["PillOk.bg"]), rgb("0F3B6D"))], minimum=3)
record("Focus outline / screen", [contrast(rgb(colors["BossGradStart"]), rgb(colors["BgScreenTop"]))], minimum=3)
payload = {"scope": "Migrated shared component token pairs, not a full-app WCAG certification", "pairs": results}
(ROOT / "docs/caveui-contrast.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
for result in results:
    print(f"{'PASS' if result['pass'] else 'FAIL'} {result['pair']}: {result['ratio']} >= {result['minimum']}")
assert all(result["pass"] for result in results), "A migrated color pairing fails its threshold"
