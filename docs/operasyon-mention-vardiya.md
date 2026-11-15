# Operasyon defteri ve vardiya ekranı, 14 Kasım 2026

## Bildirimlerin mevcut davranışı

Backend `NotificationDelivery` üzerinde `SentAtUtc`, `OpenedAtUtc` ve `RemindedAtUtc` tutuyor. Mobil bildirim veya bildirim panelindeki kayıt seçilince `POST /api/v1/notifications/{id}/open` çağrılıyor. Bu işlem kullanıcıya ait kaydı bir kez işaretliyor; içeriğin gerçekten okunup anlaşılması ölçülmüyor.

`NotificationScheduler` her dakika çalışıyor. Türkiye takviminde aynı gün gönderilmiş, en az 30 dakikadır açılmamış ve henüz hatırlatılmamış bildirim bir kez `.reminder` türüyle tekrar gönderiliyor. Hatırlatma bildirimleri yeniden hatırlatılmıyor. Kategori o gün susturulmuşsa push çalışanı gönderimi atlıyor.

Diğer kurallar: istenen görev hatırlatması planlanan bitişe (yoksa başlangıca) 10 dakika kala; 24 saat onay bekleyen görevler için yönetici uyarısı; vardiya boşaldığında açık yüksek/kritik görev uyarısı. Bunlar açılmamış bildirim hatırlatmasından ayrı.

Sınırlar: cihazda push kaydı olmayan kullanıcı için mevcut push çalışanı teslim kaydı oluşturmuyor. Mobil açılış isteği bağlantı nedeniyle başarısız olursa kalıcı çevrimdışı yeniden deneme kuyruğu yok; backend kaydı açılmamış kalabilir. Bu çalışma bu iki davranışı değiştirmedi.

## Mention sözleşmesi

- Taleplerin mobil oluşturma ekranlarında kişi seçimi kaldırıldı. Mobil talep komutu artık `mentionedUserIds` göndermiyor. Eski istemciler ve veriler için backend talep sözleşmesi korundu.
- `GET /api/v1/operational-notes/mentionable-users?search=...`: aynı şirketin tüm aktif, silinmemiş kullanıcıları; patron ve personel birlikte. Alanlar: `userId`, `fullName`, `handle`, `isOwner`. Telefon/e-posta dönmez.
- Sistemde ayrı kullanıcı adı alanı olmadığı için `handle`, adın sadeleştirilmiş biçimi ve kullanıcı kimliğinin son 8 hanesiyle üretilir (ör. `ayse.yilmaz_ab123456`). Bu bir kullanıcı adı ayar ekranı değildir; ad değiştiğinde üretilen etiket de değişir.
- Not oluşturma/güncelleme gövdesi isteğe bağlı `mentionedUserIds` kabul eder, en fazla 50 kimlik. Kimlikler tekilleştirilir; farklı şirket, pasif veya bulunamayan kullanıcı 400 ile reddedilir.
- Mobil düzenleyici imleçte `@` ile öneri açar. Seçilen tam etiketler metne eklenir. Kaydederken yalnız metinde kalan tam etiketlerin kimlikleri gönderilir; kısmi eşleşmeler ve e-posta adresleri alıcı üretmez.
- Yeni notta etiketlenenlere `notice.mentioned` gönderilir. Günlük duyuruda aynı kişi ayrıca `notice.created` almaz. Güncellemede yalnız yeni eklenen alıcılara bildirim gider. Kişinin kendisini etiketlemesi kendisine bildirim göndermez.
- `GET /api/v1/operational-notes/{id}` bildirimin ilgili notunu açar. Etiketlenen kişi başka şubede olsa da notu okuyabilir; bu erişim düzenleme/silme yetkisi vermez. Tarihli listede aktif şube notları ve kişinin başka şubelerde etiketlendiği notlar yer alır.
- Bildirim seçimi ilgili notun tarihini/kategorisini açar. Kayıt üstte vurgulanır. Operasyon defteri kategori sayıları, tarih seçimi, silme onayı ve taslak kaybı uyarısı içerir.

## Vardiyalar ve tasarım

Mevcut mavi tema, Android tipografisi ve beyaz yüzeyler sürdürüldü. ENERGY 1 / RHYTHM 2 / MOTION 1. Başlık gradyanı mevcut ürün kimliğini taşır; rozetler kategori, etiketlenme, izin, güncellenmiş plan ve gerçek haftalık sayıları gösterir. Oklar yalnız hafta değiştirmede, çöp kutusu yalnız silmede kullanılır.

Vardiyalarda üstte tek bir vardiya kartı bulunur. İlk açılışta bugünü, gün seçildiğinde seçilen tarihin vardiyasını/iznini gösterir. Altta ikinci bir vardiya ayrıntısı kartı yoktur. Gün seçimi üstteki kartı görünür alana getirir. Gün ayrıntısı gece vardiyasının ertesi gün bittiğini belirtir. Haftalık özet ve vardiya/izin talebi kısayolları korunur. Küçük ekranlarda gün seçici yatay kayar ve en az 48 dp dokunma genişliğini korur.

Görev kartları personelin Bugün/Görevler ekranlarında ve patronun personel gruplarındaki görevlerinde aynı bileşeni kullanır. Başlık ilk satırda, renkli öncelik etiketi sağdadır; saat ve durum altında yer alır. Açıklama, kanıt gereksinimi ve gerçek ortak görev sayıları ayrı satırlarda gösterilir. Kartın tamamı ayrıntıyı açar; kanıt ekleme ve kısa hatırlatıcı eylemi personelin görev listesinde ayrı dokunma hedefleridir. Yeni yüzeyler gölgesiz ve 16 dp köşe yarıçaplıdır; saat simgesi zaman bilgisini, yön oku ayrıntıya geçişi belirtir.

Backendin `completedHours` alanı gerçek giriş/çıkış süresini değil, geçmiş günlerde planlanan vardiya saatlerini toplar. Ekrandaki metin bu nedenle “geçmiş günlerin planı” olarak düzeltildi; fiilî çalışma tamamlanmış gibi gösterilmez.

## Yayına alma

Backend ayrı `NETAPI` Git deposundadır; Android deposunun `.gitignore` dosyası bu dizini dışlar. Backend değişiklikleri ayrıca sürümlenmelidir.

Önce backendde `20261114190107_AddOperationalNoteMentions` migration'ı uygulanmalı ve API yayınlanmalı, ardından yeni Android sürümü kullanılmalıdır. Migration mevcut notlara boş UUID dizisi ekler. Canlı veritabanına bu oturumda migration uygulanmadı.

Kontroller: 8 Android birim testi ve APK derlemesi; izole PostgreSQL üzerinde operasyon mention, mevcut talep mention ve bildirim akışı için 6 entegrasyon testi; EF model/migration uyumu. Bağlı Android cihazda 5 arayüz testi: not oluşturma/mention seçimi, etiketi metinden çıkarma, bildirimden doğru tarih ve kategoriye gitme, kaydedilmemiş taslağı koruma, vardiya gün seçimi ve talep kısayolları. Vardiya ekranı ayrıca 360 dp genişlik ve %130 yazı boyutuyla kontrol edildi. Yeni metin/rozet renklerinin hesaplanan kontrastı en az 5,88:1. Canlı FCM gönderimi bu testlerde kullanılmaz.
