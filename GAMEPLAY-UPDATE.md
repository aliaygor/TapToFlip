# TapToFlip gameplay ve oyuncu ilerlemesi güncellemesi

Uygulanan proje: `C:\Users\alica\AndroidStudioProjects\TapToFlip`. Mevcut `main` geçmişi üzerine aktarıldı.

## İnceleme

Önceki motor 400×700 referans alanında 170 birim/sn yatay hız, 980 birim/sn² yerçekimi ve -510 birim/sn zıplama kullanıyordu. Hız çarpanı skora bağlı logaritmik büyüyordu: yaklaşık 30 saniyede 2,08 katına çıkıyordu. Ekran kenarları ve engel çarpışmaları turu bitiriyordu. Engel karakterleri skorla açılıyor ancak fiziksel hareketleri aynı kalıyordu. Her karede 0,033 saniye sınırı, düşük kare hızında oyunu yavaşlatabiliyordu.

Projede SharedPreferences ile rekorlar, yerel günlük hedef, ses, AdMob banner/geçiş reklamları, bir kez ödüllü devam ve WorkManager hatırlatıcıları bulunuyor. Çevrim içi liderlik tablosu veya Analytics SDK’sı bulunmadı.

## Değişen dosyalar

- `app/src/main/java/com/aliaygor/taptoflip/GameplayRules.kt`: Merkezi fizik değerleri, süreye bağlı yumuşak ve sınırlı zorluk eğrisi, modlar, kombo ve saf görev mantığı; takılabilir Analytics olay arayüzü.
- `app/src/main/java/com/aliaygor/taptoflip/GameEngine.kt`: Başlangıç hızı 95, yerçekimi 420, zıplama -300. İlk 25 saniye ekran kenarı desteği; engel çarpışmaları geçerli. 120 Hz sabit fizik adımı; aşırı uzun karelerde en fazla 0,25 saniye işleme. İlk dakika hız artışı yaklaşık %27. Üç erken kayıpta en fazla %18 başlangıç desteği; başarılı turdan sonra normal dengeye dönüş. Bonus skorlar hız eğrisini etkilemiyor. Engeller arasında en az 1,25 saniyelik yatay açıklık. 60. saniyeden itibaren uçan engellere 10 saniyede açılan sınırlı salınım. Başarılı engel geçişlerinde kombo, 3 geçişte 2x, 6 geçişte 3x; her 10 geçişte ek 20 puan. Time Attack 60 saniyede biter; Survival daha yüksek uzun dönem zorluk sınırına sahiptir.
- `app/src/main/java/com/aliaygor/taptoflip/PlayerProgress.kt`: Mevcut kayıt altyapısı ve klasik rekor anahtarı korundu. Mod rekorları, ilk öğretici durumu, erken kayıp desteği, toplam oyun ve yıldızlar saklanıyor. Yerel takvim tarihine göre üç görev: 100 toplam puan, 3 tamamlanan oyun, 5 kombo eşiği. Her tamamlanan görev bir kalıcı yıldız verir. Yarım bırakılan tur, tamamlanan oyun sayılmaz. Ödüllü devam turu sonuçlandırılana kadar görevlerin iki kez sayılmasını önleyen UI akışı korunur.
- `app/src/main/java/com/aliaygor/taptoflip/MainActivity.kt`: İlk oyun öğreticisi, duraklatılabilen 3-2-1 sayacı, mod HUD’u, kenar desteğinin biteceği uyarısı, kombo animasyonu ve sesi, günlük görev özeti ve Türkçe/İngilizce kaybetme metinleri. 20 saniyeden kısa turlar geçiş reklamı üretmez; mevcut üç uygun tur ve 45 saniye reklam aralığı korunur. Analytics olay noktaları: game_start, game_over, session_duration, score_reached, retry_clicked, game_mode_selected, daily_task_completed. Game over olayları aktif oyun süresi ile 10/30/60 saniye eşiklerini ve modu içerir.
- `app/src/main/java/com/aliaygor/taptoflip/HomeScreen.kt`: Aynı ana menüde mod seçimi, mod rekoru, üç görevin ilerlemesi, yıldızlar ve toplam oyun.
- `app/src/main/java/com/aliaygor/taptoflip/GameText.kt`: Eklenen metinler için cihaz diline göre Türkçe/İngilizce seçimi.
- `app/src/test/java/com/aliaygor/taptoflip/GameplayRulesTest.kt`: Zorluk, kombo/puan, görev ilerlemesi, 30/60/90/120 Hz süre-fizik-skor tutarlılığı, bir kez engel ödülü, Time Attack bitişi ve öğrenme desteğinin sona ermesi testleri.
- `app/src/test/java/com/aliaygor/taptoflip/GameEngineTest.kt`: Eski agresif hız beklentisi yeni öğrenme dönemi kriterine güncellendi; diğer testler korundu.
- `app/src/androidTest/java/com/aliaygor/taptoflip/PlayerProgressTest.kt`: Görevlerin ve mod rekorlarının yeniden açılışta korunması, yıldız ödülleri ve gün yenilenmesi testi eklendi.
- `gradle/libs.versions.toml`, `app/build.gradle.kts`: Mevcut AdMob 25.5.0 bağımlılığının Kotlin 2.3 metadata gereksinimi için Kotlin/Compose derleyicisi 2.3.0, mevcut AGP 9.0.1 / Gradle 9.1.0 korunarak compilerOptions DSL. Uygulama kimliği, sürüm, SDK hedefleri ve imzalama ayarları değiştirilmedi. Araç uyumluluğu: https://developer.android.com/build/releases/agp-8-13-0-release-notes

## Doğrulama ve sınırlar

- 36 birim testi geçti; hata veya atlanan test yok.
- `testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug` komutu başarıyla tamamlandı. Debug APK, R8 ile küçültülen release APK ve Android test APK’sı derlendi. Release APK mevcut yapılandırma gereği imzasızdır; yayınlama yapılmadı.
- Derleme günlüğü: `app/build/reports/gameplay-build.log`; test raporu: `app/build/reports/tests/testDebugUnitTest/index.html`; lint raporu: `app/build/reports/lint-results-debug.html`.
- Lint debug kontrolü geçti; mevcut bağımlılık/API uyarıları ayrıca raporda görülebilir.
- Android testleri çalıştırılmadı: `adb devices` bağlı cihaz göstermedi. Fiziksel cihazda/emülatörde görsel, ses ve performans doğrulaması henüz yapılmadı.
- Analytics varsayılan olarak boş bir sink kullanıyor: olay noktaları hazır, veri gönderimi yok. Çevrim içi liderlik tablosu veya yeni backend eklenmedi.
- Yıldızlar yerel başarı göstergesidir; mağaza veya harcama sistemi eklenmedi.
- Gerçek oyuncu tutundurma etkisi henüz ölçülmedi. Cihazda ilk 25 saniye kenar desteği, 25. saniye geçişi, 60. saniye hareketli engeller, tekrar oyna, üç mod, rewarded continue ve tarihin değişmesi özellikle denenmeli.
