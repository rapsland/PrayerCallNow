# Değişiklik Günlüğü / Changelog

## Sürüm Kuralları (Semantic Versioning)
- **Major (`x.0.0`)**: Büyük mimari değişiklikler veya geriye dönük uyumsuz güncellemeler
- **Feature (`0.x.0`)**: Yeni özellikler (MINOR artar, PATCH sıfırlanır)
- **Bugfix (`0.0.x`)**: Hata düzeltmeleri ve küçük iyileştirmeler

---

## v0.8.0 (2026-11-20)
### Özellikler
- **Geliştirici Özel Test Vakti (`Özel Test Vakti`)**: Zaman simülasyonu olmadan gerçek `AlarmClock` bildirimi ve sesini test etmek için istenen `SS:dd` saatini seçme (ve tek dokunuşla `+1 dk`, `+2 dk`, `+5 dk` hızlı kurulum) özelliği eklendi.
- **Vakitler Ekranında İsteğe Bağlı 7. Kart**: Ayarlar → *Test Modu ve Geliştirici Testi* üzerinden açıldığında *Vakitler* ekranında sıradan vakitlerin altında canlı *Özel Test Vakti* kartı da görüntülenir ve açılıp kapatılabilir.

---

## v0.7.1 (2026-11-20)
### Düzeltmeler & İyileştirmeler
- **Sessiz Modda Bildirim Sesi Düzeltmesi**: Cihaz zil sesi sessiz veya titreşim modundayken (`STREAM_NOTIFICATION` = 0) vakit bildirimlerinin sessiz kalması giderildi; arka plan vakit alarmları artık `USAGE_ALARM` (yedek olarak `USAGE_MEDIA`) akışı üzerinden duyulabilir biçimde çalar.
- **Eski Bildirim Kanalları Temizliği**: Önceki sürümlerden kalan kullanılmayan `ezannow_alert_v3_*` bildirim kanalları Android bildirim ayarlarından otomatik temizlenir.

---

## v0.7.0 (2026-11-20)
### Özellikler
- **Durum Çubuğunda Sıradaki Vakit Göstergesi (Pil Dostu, Servis Gerektirmez)**: Üst durum çubuğunda küçük cami simgesi ve bildirim panelinde Android'in yerleşik geri sayımı (`Chronometer`) ile sıradaki namaz vaktini ve günün tüm vakitlerini sessizce gösterir (~%0 CPU yükü).
- **Ayarlar Kontrolü & Otomatik Güncelleme**: *Ayarlar → Bildirimler ve Uyarılar → „Durum Çubuğunda Vakit Göster“* anahtarıyla açılıp kapatılabilir; her vakit geçişinde ve saat 24:00'te günlük bildirim temizliğiyle uyumlu biçimde otomatik yenilenir.

---

## v0.6.0 (2026-11-20)
### Özellikler
- **Uygulama Kapatılsa da Sesli Bildirim (`PrayerAlertService` + `setAlarmClock`)**: Uygulama Son Uygulamalar ekranından kaydırılarak tamamen kapatıldığında veya cihaz Doze/kilit modundayken dahi namaz vakti alarmları `AlarmClock` önceliğiyle tetiklenir; kısa süreli ön plan servisi seçilen tonu 4–10 saniye çalıp otomatik olarak kapanır (arka planda 0 yük kalır).
- **10 Saniye Arka Plan Testi**: Ayarlar → *Bildirimler ve Uyarılar* altına, uygulamayı kaydırarak kapattıktan sonra alarm ve sesi canlı test etmek için *"10 sn Arka Plan Testi"* butonu eklendi.

---

## v0.5.1 (2026-11-20)
### Düzeltmeler & İyileştirmeler
- **Ses Kaynakları ve Etiket Temizliği**: Tüm yerleşik bildirim ve ezan seslerinden harici meta veri (ID3/Xing) başlıkları temizlendi, ses dosyaları standart `ezannow_*.mp3` biçiminde adlandırıldı ve arayüz ses isimleri sadeleştirildi.

---

## v0.5.0 (2026-11-20)
### Özellikler
- **Ayarlanabilir Bildirim Sesi Süresi (Varsayılan 4 sn, 1–10 sn arası)**: Ayarlar → *Bildirimler ve Uyarılar* ve ses seçici penceresi üzerinden çalma süresi 1–10 saniye arasında (varsayılan 4 saniye) ayarlanabilir hale getirildi.
- **24:00 Günlük Otomatik Bildirim Temizliği**: Her gün saat 24:00'te (`00:00`) o güne ait açık namaz vakti bildirimleri hem Android sistem zaman aşımı (`setTimeoutAfter`) hem de günlük `00:00` alarmı ve 0–24s Test Modu gün sonu geçişiyle otomatik silinir.

---

## v0.4.0 (2026-11-20)
### Özellikler
- **Yerleşik Bildirim ve Ezan Sesleri (11 MP3)**: Uygulama içi kullanım için 11 adet MP3 bildirim ve kısa ezan sesi (`melody_soft`, `melody_calm`, `melody_short`, `ney_classic`, `ney_peaceful`, `kanun_classic`, `tasavvuf_melody`, `adhan_short_1`, `adhan_short_2`, `fajr_call`, `alarm_classic`) entegre edildi.
- **Önizleme & Zaman Aşımı**: Ayarlardaki ses listesi güncellendi; seçimde anında önizleme yapılır ve tüm gerçek/simülasyon bildirimlerinde belirlenen süre sonunda otomatik durdurma sağlanır.

---

## v0.3.0 (2026-11-20)
### Özellikler
- **Bildirim Sesi Seçenekleri (6 Özgün 3 sn Ton + Sistem + Özel Ses)**: Ayarlar üzerinden *Yumuşak Melodi*, *Ney Sesi (Hicaz)*, *Kanun Tınısı*, *Tasavvuf & Huzur*, *Sabah Makamı*, *Klasik Kısa Uyarı*, *Cihaz Varsayılan Sesi*, *Telefondan Özel Ses (MP3/WAV)* ve *Sessiz* seçenekleri eklendi.
- **Canlı Ses Önizleme & 3 sn Otomatik Kesme**: Seçim listesinde dokunulan her ses anında 3 saniye önizlenir; özel seçilen uzun ses dosyaları da bildirimlerde en fazla 3.0 saniye çalar.

---

## v0.2.1 (2026-11-20)
### Düzeltmeler & İyileştirmeler
- **Yeni Mitolojik Zeus İllüstrasyonu**: Açılış animasyonundaki vektör Zeus figürü, galaksi ve kozmik nebula içinden çıkan detaylı illüstratif Zeus görseli (`zeus_nebula.png`), parlayan elektrik göz arkları ve çok dallı şimşek efektiyle yenilendi.

---

## v0.2.0 (2026-11-20)
### Özellikler
- **3 Saniyelik Yumuşak Bildirim Sesi (`ezan_gentle_tone.wav`)**: Gerçek vakit alarmları, Test Bildirimi ve 0–24 saat Test Modu bildirimleri için özgün 3.0 saniyelik armonik bildirim tonu eklendi.
- **Akıllı Bildirim Kanalları (`v2`)**: Gerçek alarmlarda özel ses kanalı, Test Modu ve önizlemede çakışma/tekrar önleyici doğrudan `MediaPlayer` oynatımı (`GentleTonePlayer`).
- **Ayarlar Kontrolü**: **Bildirimler ve Uyarılar** bölümüne *"Bildirim Sesi (3 sn Yumuşak Ton)"* anahtarı ve *"Sesi Dinle"* önizleme butonu eklendi.

---

## v0.1.0 (2026-11-20)
### Özellikler
- **Galaksi + Nebula + Zeus Yıldırım Açılış Animasyonu**: Açılışta dönen sarmal galaksi, kozmik nebula, buluttan beliren Zeus silüeti, çok dallı şimşek atışı, flaş/şok dalgası ve `EzanNow!` marka geçişi (Ayarlardan tekrar izlenebilir).
- **0–24 Saat Test Modu**: Zamanı 00:00–24:00 arasında seçilebilir hızlarda (`1 dk/sn`, `5 dk/sn`, `15 dk/sn`, `30 dk/sn`, `60 dk/sn`) oynatma, zaman çubuğu ile ileri/geri alma, sıradaki vakte `-1 dk` atlama ve canlı bildirim tetikleme testi.
- **Sürüm & APK Yönetimi**: `version.properties` üzerinden merkezi sürüm yönetimi; APK dosyalarının `EzanNow_v<sürüm>_<YYYYMMDD>_<buildType>.apk` formatında adlandırılması ve paketleme öncesi eski APK'ların otomatik temizlenmesi.
- **Çekirdek Özellikler**: Çevrimdışı Diyanet / astronomik vakit motoru, Vakitler geri sayımı ve Kerahat banner'ı, Aylık İmsakiye ve Dini Günler, Kıble Pusulası, Zikirmatik & Kaza Takibi, 81 il + diaspora şehir kataloğu, GPS, AlarmManager bildirimleri ve ana ekran widget'ı.
