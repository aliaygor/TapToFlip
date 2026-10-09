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
