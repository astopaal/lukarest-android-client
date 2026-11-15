# Android ve NETAPI uyarlaması

12 Kasım 2026. Başlangıçtaki yerel düzenlemeler korunarak uygulandı. Android ana repo, NETAPI kendi Git deposudur; iki depodaki değişiklikler birlikte alınmalıdır.

## Uygulanan davranışlar

- QR: sunucunun expiresAtUtc değeriyle sayaç, anonim aynı oturum renew, claim sonrasında 15 dakikalık exchange penceresi, süre dolunca yeni oturum. QR uçları access token yenileme akışına girmez.
- Görev: Low/Medium/High/Critical, Medium varsayılanı, seçili personele şablonsuz atama; ShiftStaff ve Quota için şablon zorunluluğu.
- Onay: requiresApproval, AwaitingApproval/Rejected durumları, yönetici review, personelde red notu ve yeni kamera kanıtıyla tekrar gönderim. Eski onaylı tanımlarda None kanıt türü PhotoOnly olarak okunur.
- Kanıt: galeri seçicisi kaldırıldı. Fotoğraf TakePicture, video CaptureVideo ile uygulamanın oluşturduğu çıktı URI’sine çekilir. Bekleyen onay ve kapanmış göreve kanıt yükleme engellendi. Kamera kökeni kontrolü Android akışındadır; API medya türü, boyut, yetki ve red sonrası yeni kanıtı denetler.
- Talep: tüm oluşturma girişlerine priority ve mentionedUserIds, personele açık şube kişi seçicisi, etiketlenenlerin talepleri görebilmesi. Başkasının talebini iptal edemezler.
- Bildirim: notificationId ve priority taşınması, teslimata özgü PendingIntent, açılış POST’u, kategori susturma, sunucuda sayfalama ve drawer. Hatırlatma türleri ve yeni görev uyarıları yönlendirilir.
- Vardiya/izin: onay sonrası isteğe bağlı plan yönlendirmesi; saat aralıkları üzerinden gerçek kapsam boşluğu hesabı. İzin uygulanmadan önceki kapsam JSON içinde saklanır. Talep onayı ve manuel hücre değişikliğinde boşluk uyarısı oluşur.
- Rol/çıkış: operasyon defterinde alt menü oturum rolüne bağlıdır. Personel başlığındaki ilk ekrana dönüş yolu kaldırıldı, bildirim açılır. Uygulama öne geldiğinde /me kontrolü yapılır; sunucu oturumu reddederse yerel görevler temizlenip eşleştirmeye dönülür. Merkezi JWT doğrulaması pasif/silinmiş hesabın access token’ını tüm uçlarda reddeder.
- İçerik: talep, not, yorum ve mesaj alan adlarında sunucuda sözlük temelli küfür/argo engelleme. Türkçe büyük İ, diakritik, bazı rakam dönüşümleri ve tekrar eden harfler işlenir. Metin değiştirilmeden profanity_detected ile reddedilir. Liste ContentModerationFilter.cs içindedir.

## NETAPI sözleşme ekleri

Yeni veritabanı kolonu eklenmedi. Mevcut backend şemasının güncel olması gerekir. Açılış hatası sonrasında mevcut `AddRequestPriorityAndMentions` migration'ı düzeltildi: eski taleplerin `mentioned_user_ids` alanı boş UUID dizisiyle dolduruluyor; geçici SQL varsayılanı ardından kaldırılıyor. Yeni migration veya veri silme işlemi yok.

| Uç/yanıt | Ek |
|---|---|
| GET /api/v1/notifications?page=1&pageSize=20 | items ve hasMore; yalnız kullanıcının teslimatları, pageSize en fazla 50 |
| Bildirim items | notificationId, type, id, category, sentAtUtc, openedAtUtc, contextText |
| GET /api/v1/requests/mentionable-users | Aktif şube üyeleri için yalnız userId/fullName; yönetici personel listesinin yetkisi değişmedi |
| GET /requests/my-requests ve /my-requests/{id} | Kendi veya kendisinin etiketlendiği talepler; iptal yetkisi yalnız sahibinde |
| QR status | Claim öncesi oturum bitişi; claim sonrasında ClaimedAtUtc + 15 dakika olarak expiresAtUtc |
| Görev liste/tamamlama/review yanıtı | priority, requiresApproval, submittedAtUtc, reviewedAtUtc, reviewedByUserId, reviewNote |
| Talep coverage ve manuel vardiya hücresi coverage | hasGap, message, staffNames |
| Vardiya boşluk push’ı | shift.coverage_gap |

Android bu eklerle aynı sunucu sürümüne bağlanmalıdır. Canlı sunucuya dağıtım yapılmadı.

## Tasarım kararları

- Yön: mevcut DESIGN.md, operasyon ekranları; ENERGY 1 / RHYTHM 1 / MOTION 1.
- Mavi ve mevcut tipografi ürünün yönetim/personel kimliğini sürdürür; yeni font veya görsel varlık oluşturulmadı.
- Stok alt paneli uzun formu ve klavyeyi kaydırılabilir alanda tutar.
- Drawer, ana görev akışını terk etmeden bildirim okumayı sağlar; sunucu sayfalaması geçmişi parça parça getirir.
- Susturma düğmeleri iki sütundadır; küçük ekranda büyük Türkçe yazı etiketleri bölünmez.
- Kırmızı yalnız hata/boşluk, kehribar onay bekleme durumunda kullanılır; durum metinle de belirtilir.
- Dolu düğmelerde beyaz metin mavi zeminde okunabilirlik için açıkça atanır; öncelik seçimi klavye odağını bırakır.

## Doğrulama

- Android assembleDebug: PASS. APK: app/build/outputs/apk/debug/app-debug.apk.
- JVM: PASS, dört TaskContractTest ve mevcut ExampleUnitTest, toplam beş test.
- Emülatör: PASS, Pixel 10 Pro Android 17; 720x1280, 320 dpi ve font_scale 1.3 ile on senaryo. Önce doğal ekran boyutunda dokuz senaryo da geçti.
- NETAPI: İlk doğrulamada .NET 10 derlemesi ve temiz PostgreSQL 17 veritabanında 32 test geçti. Bu doğrulama mevcut taleplerle migration geçişini kapsamıyordu; açılış hatası bu nedenle kaçırıldı.
- Migration regresyonu: `MigrationUpgradeTests`, eski şemaya bir talep ekleyip yükseltiyor. Düzeltme öncesinde aynı `23502` hatası yeniden üretildi; düzeltme sonrasında eski talep içeriği, boş etiket listesi, Medium öncelik, sonraki kayıt güncellemesi, tekrar migration ve API host açılışı doğrulandı.
- Gerçek Firebase göndericisi ve fiziksel cihazda üretim push teslimatı çalıştırılmadı. API testlerinde mevcut yakalayıcı push göndericisi kullanıldı; teslimat, açma, mute, reminder ve kritik görev verileri sınandı.
- Git diff --check: PASS. AndroidRuntime hata kaydı boştu.

Test kanıtları build/verification-final/ui-tests-small.xml, build/verification/backend/*.trx ve Android build test raporlarındadır. Görüntüler yalnız test verisidir.

### Etkileşim kaydı

| Etkileşim | Gözlenen sonuç |
|---|---|
| Öncelik düğmesi, Kritik seçimi | Görünen değer Kritik oldu |
| Fotoğraf görevi | Kamera eylemi var; galeri ve video seçimi yok |
| Kamera eylemi | Android IMAGE_CAPTURE intent’i açıldı |
| Video görevi | Video çekimi var; galeri seçimi yok |
| Onay bekleyen görev | Tekrar gönderim kapalı |
| Reddedilen görev | Yönetici notu görünüyor; tekrar gönderim açık |
| Patron: operasyon defteri, geri | Yönetici alt menüsü korunuyor; Vardiyalarım görünmüyor |
| Personel başlık eylemi | Bildirim paneli callback’i çağrılıyor; çıkış/QR dönüş eylemi yok |
| Drawer aç/kapat/yenile/sayfala | Panel kapanıp açılıyor; ikinci sayfa geliyor; seçilen notificationId korunuyor |
| Talep kategorisini sustur | request kategorisi gönderiliyor, işlem durumu gösteriliyor |
| Stok formu | Boşken gönderim kapalı, geçerli metinlerle açık; öncelik seçiliyor; Vazgeç kapatıyor |
| Görev review API | Onay, red, red notu, yeni kanıt zorunluluğu ve yeniden onay geçti |
| QR API | Renew ve claim sonrası exchange süresi kontrolleri geçti |
| Vardiya API | Kısmi kapsam boşluğu, onay sonrası kayıt ve manuel değişiklik uyarısı geçti |

## Antislop teslim kontrolü

Kontrol kapsamı bu görevde üretilen arayüzler ve değiştirilen davranışlardır. HTML/CSS’ye özgü maddeler Android Compose için uygulanabilir karşılıklarıyla değerlendirildi.

| Kural | Durum ve kanıt |
|---|---|
| R-02 | PASS: yeni metinlerde em dash kullanılmadı |
| R-03 | PASS: dar ekran/büyük yazı testleri ve görüntü incelemesi; yeni seçim satırları en az 48 dp |
| R-17 | PASS: ürün istatistiği eklenmedi; rapor sayıları test çıktılarından |
| R-18 | PASS: referans, kişi fotoğrafı veya testimonial eklenmedi |
| R-23 | PASS: logo/görsel üretilmedi; drawer ve form kullanıcının istediği akışlar |
| R-24 | PASS: Bildirimler gerçek drawer’a açılıyor; yeni ucu olmayan bağlantı yok |
| R-25 | PASS: TextMain/BgScreenTop 15.33, TextMuted/BgScreenTop 5.53, beyaz/StaffGradEnd 4.63, beyaz/BtnGradEnd 4.53, uyarı 6.24, hata 6.23 kontrast oranları hesaplandı |
| R-26 | PASS: yeni eylemler callback veya Retrofit çağrısına bağlı; etkileşim ve API kayıtları yukarıda |
| R-27 | PASS: drawer/kişi seçicide loading, boş, hata; gönderim ve incelemede busy/hata durumları |
| R-28 | PASS: FAQ eklenmedi |
| R-32 | PASS: Compose native seçim ve menü kontrolleri; drawer geri tuşuyla kapanır; kamera izni geri bildirimi var |
| R-33 | PASS: kaynak düzenlemeleri doğrudan apply_patch ile yapıldı |
| R-34 | PASS: mevcut açık tema kullanılır, yeni tema anahtarı eklenmedi |
| R-35 | PASS: APK/sunucu derlemesi, emülatör etkileşimleri, görüntüler ve yerel API testleri; üretim FCM sınırı yukarıda açık |
| R-36 | PASS: güvenlik/performans/uyumluluk iddiası veya sayı uydurulmadı |
| R-37 | PASS: DESIGN.md okundu, yön ve dials uygulama öncesinde belirtildi |
| R-38 | PASS: ürün metinleri gerçek sözleşmeden; örnekler yalnız test dosyalarında |
| R-39 | PASS: yeni kontroller Material/Compose semantiği, görünür alan etiketleri ve anlamlı ikon açıklamaları kullanır; web ARIA maddeleri uygulanmaz |
| R-40 | PASS: medya DOM’u/hero/üçüncü taraf web betiği eklenmedi; Web Vitals ölçülmedi ve raporlanmadı |
| R-01, R-04, R-06 | PASS: mevcut marka renkleri/tipografisi; kamera ve video ikonları eylemi tanımlar |
| R-07, R-08, R-09 | PASS: dekoratif zemin veya ok eklenmedi; durumlar gerçek uygulama durumu |
| R-10, R-12, R-13 | PASS: yeni blur/glow eklenmedi; panel doğal katman davranışını kullanır |
| R-14, R-19, R-22 | PASS: içerik listesi gerçek teslimatlardır; yalnız native panel geçişleri; illüstrasyon yok |
| Liveliness | PASS: 1/1/1 yönü, görevde gönderim odağı, panelde bildirim odağı, yapısal boşluk, mevcut mavi kimlik |
| C-1..C-5 | PASS: karar gerekçeleri, gerçek eylemler, görev içeriğine bağlı yapı, küçük ekran testleri, gerçek test kanıtı |
| R-05, R-11, R-15, R-16 | PASS: operasyon formu/liste, mevcut radius düzeni ve Talebi gönder gibi eyleme özgü etiketler |
| R-20, R-21, R-29, R-30, R-31 | PASS: mevcut Lukarest açık mavi yönü korunur; ürün klonu veya yeni renk/font sistemi oluşturulmadı; kararlar kaydedildi |
