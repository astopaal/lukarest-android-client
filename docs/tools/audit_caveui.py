"""Read a CaveUI registry snapshot and write the item-level migration inventory.

This tool only writes reports. It never rewrites application sources.
"""

import argparse
import csv
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


COMMIT = "2e724254e9446acc086720d7d74af6a17b67d7f7"
ADAPTED = {
    "button-primary": ("PrimaryButton", "Material Button ile rol, klavye ve basılma davranışı kazanıldı."),
    "button-gradient": ("PrimaryButton", "Mevcut BtnGradient ve BossGradient korundu; CaveUI turuncusu alınmadı."),
    "button-loading": ("PrimaryButton.loading", "Gerçek işlem durumunda ilerleme gösterilir ve yeniden gönderim engellenir."),
    "button-block": ("PrimaryButton", "Tam genişlik korunur, sabit yükseklik yerine büyüyebilen minimum yükseklik kullanılır."),
    "button-fab": ("BossBottomNavWithCenterFab", "QR eylemi gerçek FloatingActionButton kullanır."),
    "button-filter-chip": ("LukaChoiceGroup", "Seçim durumu erişilebilir FilterChip ile gösterilir."),
    "card-basic": ("LukaCard", "Tanımsız CaveCard yardımcısı yerine temalı Material Card kullanılır."),
    "check-card": ("LukaCheckRow", "Personel satırının tamamı tek checkbox hedefidir; iç checkbox ikinci bir eylem oluşturmaz."),
    "choice-segmented-slide": ("LukaChoiceGroup", "Tek seçim ilkesi korundu; kayma animasyonu yerine dar ekranda satıra akış kullanılır."),
    "select-segmented": ("LukaChoiceGroup", "Görev filtreleri, form seçenekleri ve öncelik ortak seçim grubuna taşındı."),
    "select-radio-list": ("LukaChoiceRow", "Vardiya seçenekleri RadioButton rolü ve en az 48 dp hedefle sunulur."),
    "select-bottom-sheet": ("TaskPickerSheet / LukaBottomSheet", "Mevcut vardiya ve personel seçim akışları ortak sheet yüzeyine bağlandı."),
    "input-search": ("LukaSearchField", "Personel atama ve QR eşleştirme aramaları görünür etiketle ortaklaştırıldı."),
    "input-clearable": ("LukaSearchField", "Metin varken çalışan ve adlandırılmış arama temizleme eylemi sunulur."),
    "input-time": ("LukaTimeField", "24 saatlik mevcut sözleşme korunur; dar alanda TimeInput kullanılır."),
    "picker-time": ("LukaTimeField", "Saat seçimi mevcut değerle açılır; seç, temizle ve vazgeç akışları korunur."),
    "disclosure-accordion": ("LukaAccordionHeader", "Panel ve görev akordiyonları açık/kapalı durumunu ve düğme rolünü bildirir."),
    "feedback-alert": ("LukaErrorState", "Hata metni, hata ikonu ve gerçek yeniden deneme eylemi birlikte sunulur."),
    "feedback-empty": ("LukaEmptyState", "Görevler, bildirimler ve personel seçimi gerçek boşluk nedenini açıklar."),
    "feedback-progress": ("LukaProgressBar", "Gerçek ilerleme semantiği eklendi; geçersiz ve sınır dışı değerler güvenle sınırlandırılır."),
    "card-progress": ("LukaProgressBar", "Mevcut iş verisinden hesaplanan ilerleme korunur; örnek depolama sayıları alınmaz."),
    "feedback-snackbar": ("LukaSnackbarHost", "Mevcut eylem/kapat sözleşmesi korunur; başarı ikonunun koyu zemindeki kontrastı düzeltildi."),
    "nav-bottom-bar": ("LukaBottomNav", "Material NavigationBarItem ile gerçek seçili sekme semantiği sağlanır."),
    "nav-fab-dock": ("BossBottomNavWithCenterFab", "Ortadaki QR eylemi ve dört rota korunarak Material navigasyon öğelerine geçildi."),
    "nav-pill-tabs": ("LukaChoiceGroup", "Personel görev filtreleri erişilebilir ve dar ekranda sarılan seçimlere taşındı."),
    "nav-app-bar": ("TopPill", "Mevcut kapsül başlık korunur; eylem IconButton olur ve büyük yazıda yükseklik büyür."),
    "overlay-sheet": ("LukaBottomSheet", "Sekiz kullanım noktasının sheet rengi, şekli ve genişlik sınırı ortaklaştırıldı."),
}
RETAINED = {
    "avatar-initials": "TopPill ve personel listelerindeki gerçek ad baş harfleri korunur.",
    "input-default": "Mevcut OutlinedTextField zaten aynı Material temelini kullanıyor.",
    "input-labeled": "Mevcut görünür etiketli Material alanlar korunur.",
    "input-error": "Mevcut alan doğrulamaları ve supportingText sözleşmeleri korunur.",
    "input-helper": "Ürün bağlamındaki yardımcı metinler korunur.",
    "input-disabled": "Mevcut Material enabled sözleşmesi korunur.",
    "input-textarea": "Görev açıklaması ve not çok satırlı Material alanları korunur.",
    "input-counter": "NoteMentionField gerçek 2000 karakter sınırı ve sayacını zaten sağlıyor.",
    "input-mention": "Mevcut sunucu destekli kişi listesi, imleç ve mention kimliği çözümü örnekten daha kapsamlıdır.",
    "input-date-picker": "Mevcut tarih alanları ve tarih sözleşmeleri korunur.",
    "input-file": "Mevcut Android içerik seçicisi ve kanıt dosyası doğrulaması korunur.",
    "input-loading": "Mevcut kaydetme ve yükleme durumları gerçek istek durumuna bağlıdır.",
    "button-outline": "Mevcut OutlinedButton kullanımları aynı Material örüntüsüdür.",
    "button-ghost": "Mevcut TextButton eylemleri aynı Material örüntüsüdür.",
    "button-icon-leading": "Mevcut ikon ve metinli Material butonlar korunur.",
    "button-icon-only": "Mevcut adlandırılmış IconButton kullanımları korunur.",
    "button-destructive": "Mevcut silme onaylarının işlem ve hata yönetimi korunur.",
    "button-back": "Mevcut Navigation Compose geri dönüşleri korunur.",
    "nav-tabs": "Mevcut Material TabRow ekranları aynı temel örüntüyü kullanır.",
    "nav-wizard": "Görev oluşturmadaki gerçek üç aşamalı form korunur.",
    "nav-badged-tabs": "Gerçek veri sayısı gösteren mevcut filtreler korunur; örnek sayaçlar alınmaz.",
    "overlay-dialog": "Mevcut AlertDialog onayları zaten aynı Material temelini kullanır.",
    "overlay-drawer": "Bildirim ve yönetim menüsündeki mevcut Material drawer ve rotalar korunur.",
    "picker-calendar": "Mevcut vardiya takvimi haftalık iş verisine bağlıdır; demo ay takvimiyle değiştirilmez.",
    "picker-date-field": "Mevcut Material tarih seçimi ve API tarih biçimi korunur.",
    "select-dropdown": "Mevcut öncelik alanı ve Material DropdownMenu korunur.",
    "select-assignee": "Mevcut personel kimlikleri, arama ve çoklu seçim korunur; örnek kimlikler alınmaz.",
    "menu-dropdown": "Mevcut DropdownMenu aynı temel bileşendir.",
    "menu-grouped": "Panel menüsünün gerçek şube ve oturum grupları korunur.",
    "menu-account": "Mevcut şube değişimi ve çıkış eylemleri korunur.",
    "hint-field-error": "Mevcut isError ve supportingText doğrulamaları korunur.",
    "toggle-eye": "Varsa mevcut görünürlük sözleşmesi korunur; yeni parola akışı eklenmez.",
    "toggle-chip-check": "LukaChoiceGroup tarafından karşılanan aynı FilterChip örüntüsünün ikinci kopyası alınmaz.",
    "toggle-radio-segmented": "LukaChoiceGroup ile aynı işlevin ayrı kopyası alınmaz.",
    "choice-cards": "LukaChoiceRow ve LukaCheckRow aynı seçim ihtiyacını karşılar.",
    "card-notification": "Mevcut NotificationDrawer kartları gerçek payload, tarih ve açılma durumunu taşır.",
    "card-empty": "Boş durum ihtiyacı LukaEmptyState ile karşılandı; ikinci örnek kart eklenmez.",
    "card-alert": "Uyarı ihtiyacı mevcut durum renkleri ve LukaErrorState ile karşılanır.",
}


def entries(root):
    for path in sorted(root.glob("*.tsx")):
        raw = path.read_text(encoding="utf-8")
        if path.stem == "icons":
            pattern = r'  icon\(\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*`((?:\\.|[^`])*)`'
            for match in re.finditer(pattern, raw, re.S):
                yield path, raw, match.start(), match[1], match[2], match[4]
        else:
            matches = list(re.finditer(r'^\s+id: "([^"]+)",', raw, re.M))
            for index, match in enumerate(matches):
                block = raw[match.start():matches[index + 1].start() if index + 1 < len(matches) else len(raw)]
                name = re.search(r'\bname:\s*"([^"]*)"', block)[1]
                code = re.search(r'\bcode:\s*`((?:\\.|[^`])*)`', block, re.S)[1]
                yield path, raw, match.start(), match[1], name, code


def decision(slug, category, name, flags):
    if slug in ADAPTED:
        target, reason = ADAPTED[slug]
        return "Uyarlandı", target, reason
    if slug in RETAINED:
        return "Mevcut korundu", "Mevcut Compose uygulaması", RETAINED[slug]
    if category == "charts" or slug.startswith("card-metric-"):
        reason = f"{name}: bu görselleştirmenin cevaplayacağı ayrı bir ürün sorusu ve veri sözleşmesi tanımlı değil."
    elif slug.startswith(("card-finance-", "card-shop-", "card-media-", "card-health-", "card-travel-", "card-social-")):
        reason = f"{name}: finans, alışveriş, medya, sağlık, seyahat veya sosyal ürün akışı uygulamanın vardiya/görev kapsamına girmiyor."
    elif "infinite-motion" in flags:
        reason = f"{name}: sürekli hareketin operasyon ekranlarında bir işlevi yok; mevcut sakin hareket yönü korunuyor."
    elif category in ("sliders", "command"):
        reason = f"{name}: mevcut ekranlarda bu kontrolün yöneteceği değer veya komut akışı yok."
    elif category in ("icons", "badges", "toggles", "avatars"):
        reason = f"{name}: mevcut durum metni ve ikonları aynı ihtiyacı karşılıyor; yeni dekoratif durum/kimlik eklenmiyor."
    else:
        reason = f"{name}: mevcut akışta ayrı bir kullanım ihtiyacı bulunmadı; seçilen ortak bileşenlerin ikinci bir varyantı eklenmiyor."
    return "Alınmadı", "", reason


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("registry", type=Path)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    rows = []
    for path, raw, start, slug, name, code in entries(args.registry):
        checks = {
            "empty-handler": r"(?:onClick|onValueChange|onCheckedChange)\s*=\s*\{\s*\}",
            "fixed-color": r"Color\(0x",
            "infinite-motion": r"rememberInfiniteTransition|infiniteRepeatable|while\s*\(true\)",
            "fixed-width": r"\.width\(\d+\.dp\)",
            "external-helper": r"CaveCard|\.shimmer\(",
        }
        flags = [label for label, pattern in checks.items() if re.search(pattern, code)]
        status, target, reason = decision(slug, path.stem, name, flags)
        line = raw[:start].count("\n") + 1
        rows.append({"id": slug, "category": path.stem, "name": name, "decision": status, "target": target,
                     "reason": reason, "flags": ", ".join(flags), "code_lines": len(code.splitlines()),
                     "sha256": hashlib.sha256(code.encode("utf-8")).hexdigest(),
                     "gallery": f"https://ui.jumnert.dev/components/{path.stem}/{slug}/",
                     "source": f"https://github.com/Jumnert/CaveUI/blob/{COMMIT}/web/src/lib/registry/{path.name}#L{line}"})
    ids = {row["id"] for row in rows}
    assert len(rows) == len(ids) == 821, f"Expected 821 unique entries, got {len(rows)} / {len(ids)}"
    assert not (set(ADAPTED) | set(RETAINED)) - ids, "Policy contains an unknown component"
    args.output.mkdir(parents=True, exist_ok=True)
    with (args.output / "caveui-inventory.csv").open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    summary = {"source_commit": COMMIT, "total": len(rows), "categories": dict(Counter(r["category"] for r in rows)),
               "decisions": dict(Counter(r["decision"] for r in rows)),
               "source_flags": dict(Counter(flag for r in rows for flag in r["flags"].split(", ") if flag))}
    (args.output / "caveui-inventory-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# CaveUI: 821 öğenin karar dökümü", "", f"Kaynak sürümü: `{COMMIT}`.", "",
             "Yöntem: her kayıt için Kotlin kodunun statik taraması, kimlik ve kullanım uygunluğu kararı. "
             "Bu liste 821 önizlemenin cihazda çalıştırıldığı iddiası değildir. Seçilen ortak bileşenlerin cihaz testleri geçiş raporundadır.", "",
             "Kod bayrakları otomatik tarama bulgularıdır; tek başlarına hata veya erişilebilirlik uygunluğu kanıtı değildir.", "",
             "| Bileşen | Karar | Luka karşılığı | Gerekçe | Kaynak kodu bayrakları |", "|---|---|---|---|---|"]
    for row in rows:
        lines.append(f"| [{row['id']}]({row['gallery']}) | {row['decision']} | {row['target']} | {row['reason']} | {row['flags']} |")
    (args.output / "caveui-821-karar-dokumu.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
