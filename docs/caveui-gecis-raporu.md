# CaveUI bileşen geçişi

Durum: uygulama değişiklikleri yapıldı, teslim doğrulaması henüz kapanmadı. 25 cihaz testinin 24'ü dokunma/klavye turunda geçti. Snackbar eylem ve kapatma callback'leri ayrı semantik testte geçti; dokunma testindeki başarısızlığın nedeni henüz doğrulanmadı. Bu nedenle genel Delivery Gate PASS olarak işaretlenmedi.

Kaynak: https://ui.jumnert.dev/components/ ve https://github.com/Jumnert/CaveUI/tree/2e724254e9446acc086720d7d74af6a17b67d7f7

821 benzersiz katalog kaydı statik olarak tarandı. 27 örnek mevcut ürüne uyarlandı, 38 örüntü mevcut uygulamada korundu, 756 öğe alınmadı. Bu çalışma 821 web önizlemesinin tek tek cihazda çalıştırıldığı anlamına gelmez. Her öğenin kararı, gerekçesi, kaynak satırı ve Kotlin kodunun SHA-256 özeti `caveui-inventory.csv` dosyasında; okunabilir karar listesi `caveui-821-karar-dokumu.md` dosyasındadır.

## Eski ve yeni karşılıklar

| Önce | Sonra | Kaynak örnekler | Kullanım |
|---|---|---|---|
| Tıklanabilir Box birincil butonu | Material Button kullanan PrimaryButton, loading desteği | button-primary, button-gradient, button-loading, button-block | Giriş, kurulum, vardiya, izin, görev oluşturma ve tamamlama |
| Surface ve elle çizilen kart gölgesi | Material Card kullanan LukaCard | card-basic | Ortak kart kullanan ekranlar |
| Tıklanabilir Column navigasyon sekmeleri | Ortak Material NavigationBarItem | nav-bottom-bar | Personel alt navigasyonu |
| Tıklanabilir QR Box ve özel sekmeler | FloatingActionButton ve ortak navigasyon öğeleri | nav-fab-dock, button-fab | Yönetici alt navigasyonu |
| Sabit yükseklikli başlık ve Box eylemi | Büyüyebilen TopPill ve IconButton | nav-app-bar | Ortak ekran başlıkları |
| Özel filtre Box'ları ve yatay kayan öncelikler | Satıra akabilen LukaChoiceGroup | select-segmented, choice-segmented-slide, button-filter-chip, nav-pill-tabs | Personel görevleri, görev formu, öncelik |
| Vardiya seçeneklerinde özel Surface | LukaChoiceRow | select-radio-list | Vardiya planlama seçimleri |
| Personel seçiminde yerel checkbox satırı | LukaCheckRow | check-card | Görev personel ataması |
| Yerel arama alanları | Etiketli ve temizlenebilir LukaSearchField | input-search, input-clearable | Personel atama ve cihaz eşleştirme |
| Sekiz ayrı ModalBottomSheet çağrısı | Ortak LukaBottomSheet | overlay-sheet, select-bottom-sheet | Görev, talep, vardiya, not ve personel panelleri |
| Akordiyonlarda sıradan tıklanabilir Row | Durumunu bildiren LukaAccordionHeader | disclosure-accordion | Yönetici paneli ve görev listesi |
| Çıplak ilerleme çubuğu | Sınırlandırılmış, erişilebilir LukaProgressBar | feedback-progress, card-progress | Mevcut görev ilerlemesi |
| Dağınık yükleniyor, boş ve hata metinleri | LukaLoadingState, LukaEmptyState, LukaErrorState | feedback-empty, feedback-alert | Personel görevleri, görev tanımları, personel seçimi, bildirimler |
| Dar alana sığmayan saat kadranı | Alan uygunken TimePicker, dar alanda TimeInput | picker-time, input-time | LukaTimeField |
| Koyu zeminde koyu başarı ikonu | Açık başarı ikonu | feedback-snackbar | Ortak snackbar |

## Tasarım kararları

- Yön: yönetici ve personel için vardiya/görev takibi; ENERGY 1 / RHYTHM 1 / MOTION 1.
- Renk: Color.kt paleti korunur; kaynak kütüphanenin turuncu/teal teması alınmaz.
- Tipografi: mevcut Android yazı ailesi ve boyut hiyerarşisi korunur; büyük yazı için sabit yükseklikler gevşetilir.
- Yerleşim: mevcut rotalar ve operasyon sırası korunur; seçimler dar alanda sarılır.
- Kartlar: yalnız mevcut içerik grupları için kullanılır; yeni dekoratif bölümler eklenmez.
- Hareket: sürekli dekoratif animasyonlar alınmaz; gerçek işlem sırasında native ilerleme göstergesi kullanılır.
- Kontrast: baş harf zemininde mevcut BossGradStart, ilerleme dolumunda mevcut BtnGradient kullanılır. Ölçüm çıktısı `caveui-contrast.json` dosyasındadır.
- Entegrasyon: CaveUI örnekleri Material 3 üstünde kaynak uyarlaması olarak kullanılır; yeni bağımlılık veya CaveTheme eklenmez. MIT bildirimi APK içindeki `assets/licenses/caveui.txt` dosyasında korunur.

## Ek olarak uygulanan yararlı parçalar

Aramayı temizleme, tek hedefli checkbox satırı, radyo seçim durumu, yeniden deneme paneli, bağlama özgü boş görünüm, işlem sırasında yeniden gönderimi engelleme ve görünür klavye odağı mevcut akışlara eklendi. Yeni ürün verisi veya backend uç noktası gerektirmezler.

Finans, alışveriş, sosyal medya, sağlık ve seyahat kartları ürün kapsamına girmediği için alınmadı. Grafiklerin cevaplayacağı ayrı bir ürün sorusu/veri sözleşmesi olmadığı için grafik ekranı eklenmedi. Demo sayılar, sahte kişiler, boş işlem gövdeleri ve sürekli animasyonlar uygulamaya taşınmadı.

## Doğrulama

Geçiş öncesi ve sonrası debug APK, Android test APK ve birim test derlemeleri başarılıdır. Ortak bileşen tokenları için 24 kontrast kontrolü eşiklerini geçti. Cihaz testi kayıtları `build/caveui-instrumentation-final.txt` dosyasında tutulur; testler yalnız emulator-5580 üzerinde çalıştırılır.

Tam proje lint kontrolü mevcut manifest App Link tanımı, kamera opt-in kullanımı, StateFlow okuması ve eski testlerde ViewModel oluşturma nedeniyle 11 hata bildirdi. Bunlar bu geçişin yeni ortak bileşenlerinden kaynaklanmaz; lint başarılı olarak raporlanmaz. Uygulamanın tüm ekranları için erişilebilirlik sertifikası veya ölçülmemiş performans iddiası verilmez.
