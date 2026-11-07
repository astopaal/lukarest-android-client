# Lukarest Android tasarım rehberi

Bu rehber, paylaşılan “Vardiya ve Görev Takip Uygulaması — Mavi Tema Tasarım Panosu” referansını Android arayüzüne uyarlar. Renklerin uygulamadaki kaynağı `ui/theme/Color.kt` dosyasıdır.

## Renk ve yüzey

- Yönetim başlığında `BossGradient` (#0B2956 → #174B94), personel başlığında `StaffGradient` kullan.
- Açık mavi ekran zemini üzerinde beyaz kartlar kullan. Ayırıcılar için `BorderSoft`, ana metin için `TextMain`, yardımcı metin için `TextMuted` kullan.
- Mavi planlanan vardiyayı, nötr gri izin veya boş durumu gösterir. Kehribarı dikkat gerektiren durumlara, kırmızıyı hatalara ayır. Durumu renk yanında metinle de belirt.

## Eylemler

- Ekranın birincil işlemi için mevcut `PrimaryButton` bileşenini kullan. İkincil işlemlerde mavi çerçeveli, ekran geçişlerinde kısa mavi metin butonu kullan.
- Panelin üst barında hamburger menüyü kullan. Operasyon ekranları, şube değişimi ve çıkış buradan erişilir.
- Menüde aktif şubeyi açık mavi başlıkta göster. Beyaz yüzey ve mavi ikonlar kullan. Şube ve oturum işlemlerini ince bir ayırıcıyla ekran geçişlerinden ayır.

## Yönetim paneli

- Haftalık plan kartında pazartesiden pazara gün adlarını, günlük planlı kişi sayısını ve “Planı aç” eylemini göster. Bugünü ince bir mavi kenarlıkla belirt. Sıfır atamayı nötr göster.
- Personel akordiyonlarında ad ve haftalık vardiya sayısı kapalıyken görünür. Açıldığında gün, vardiya saati, izin veya planlanmadı durumu gösterilir.
- Panelde gerçek şube verisini kullan. Yükleniyor, boş ve hata durumlarını ayrı göster.
