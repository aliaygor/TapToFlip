# TapToFlip — ücretsiz büyüme planı

## Hazırlananlar

- Play Console varsayılan İngilizce kısa/tam açıklaması güncel iki mod, bonus, ortam, renk reklamı ve rekabet bilgileriyle taslak kaydedildi.
- Türkçe (tr-TR) mağaza girişi eklendi; kısa/tam açıklamalar taslak kaydedildi. Ücretli çeviri kullanılmadı.
- Gerçek uygulamadan yeni ana ekran ve renk seçimi görüntüleri mağaza taslağına eklendi; toplam 7 telefon görüntüsü mevcut. Eski 5 oyun görüntüsü korundu. Yeni görseller `play-store-assets/` altında.
- Ana menüde doğrudan Google Play değerlendirme bağlantısı ve arkadaşla paylaşma bağlantısı var. Paylaşım seçili modun rekorunu kullanır.
- Resmi in-app review isteği debug sürümünde kapalıdır; en az 5 tamamlanmış tur ve 3 gün kullanım sonrasında, son denemeden en az 90 gün geçince menüde denenir. Google pencereyi göstermeyebilir; sonucu yıldız sayısı/yorum yazıldı olarak yorumlamayız. Menüdeki açık düğme mağaza sayfasına gider.
- Açıklamalar yeni AAB ve PGS yayınıyla birlikte yayınlanmalıdır; mağza taslakları üretime gönderilmedi.

## ASO ve trafik

1. Arama niyetini açıkça anlat: Türkçede kurbağa oyunu, tek dokunuş, refleks; İngilizcede frog arcade game, one-tap, high-score. Metinde doğal biçimde kullan, tekrar ve anahtar kelime listeleri ekleme.
2. İlk görselde gerçek oynanışı, ikinci görselde iki modu, üçüncüde gece ortamını, dördüncüde renkleri göster. Şimdiki iki yeni görüntü taslağa eklendi; sonraki görsel testinde eski oyun görüntülerini de yeni HUD ile yenile.
3. Ücretsiz kısa video fikirleri: 15 saniyede gündüz/gece geçişi; 5 baloncuk +25 bonus; 60 saniye yarışta kişisel rekor. Her videoda tek özellik ve Play Store bağlantısı. Sosyal hesaplara otomatik gönderim yapılmadı.
4. Paylaşım ekranındaki rekor + mağaza bağlantısı gerçek oyuncuların arkadaşlarını davet etmesini kolaylaştırır. Yorum veya davet karşılığında oyun avantajı sunma.
5. Play Console edinme raporunda arama ziyaretleri, mağaza dönüşüm oranı ve ülke/dil kırılımını haftalık karşılaştır. Uygulama tarafında geri dönüş ve tur sayısını ayrıca ölç; indirme artışı ile oyuncu tutmayı karıştırma.
6. Yeterli ziyaret oluşunca kısa açıklama veya ilk ekran görüntüsü için tek değişkenli mağaza deneyi yap. Ücretli reklam/kampanya veya faturalandırma açma.

## Yayın öncesi

- Gerçek mağaza kurulumu ile Google Play puanlama ve ödüllü renk reklamını kontrol et.
- Yeni AAB sürümünü, PGS yapılandırmasını ve mağaza taslaklarını birlikte yayına hazırla.
- Trafik/sıralama artışı garanti değildir; değişiklikleri raporlardaki sonuçlarla değerlendir.

Resmi kaynaklar:
https://developer.android.com/guide/playcore/in-app-review
https://developer.android.com/guide/playcore/in-app-review/kotlin-java
https://support.google.com/googleplay/android-developer/answer/9898842


## Store refresh and account clarity — 9 October 2026
- Submitted five store changes for Google review: Turkish listing, English title/short/full description and phone screenshots. Managed publishing remains off. No paid campaigns or billing changes.
- English title: TapToFlip: Frog Jump. Turkish title: TapToFlip: Kurbağa Oyunu. Both descriptions explain the alternate spaced name Tap to Flip and relevant frog jumping / arcade / reflex gameplay naturally; no keyword ranking guarantee.
- Removed five obsolete June screenshots. Four phone assets now show current home, frog colors and two actual gameplay-area captures (no test ads or fabricated scores). Turkish currently inherits default graphics. New account UI screenshots will replace home after 1.5 is released.
- Account UI prepared in version 1.5 (code 9): visible profile icon/header button, signed-in status card, controller pictogram, full-width Google sign-in and guest actions, scrollable account dialog. Sign-in SDK behavior unchanged.
- Debug build/lint and two device UI/capture checks passed. User must generate a signed 1.5 AAB to ship account changes; only store metadata was submitted in this update.
