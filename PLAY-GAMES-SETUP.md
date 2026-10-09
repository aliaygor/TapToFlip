# TapToFlip – Google Play Games kurulumu

Uygulama kodu `C:\Users\alica\AndroidStudioProjects\TapToFlip` projesine eklendi. Gerçek çevrim içi rekabet için aşağıdaki Console yapılandırması gerekir. Kimlikler boşken SDK başlatılmaz ve oyun misafir olarak çalışır; sahte oyuncu veya sıralama gösterilmez.

## Play Console

1. Geliştirici hesabıyla Play Console'a gir; mevcut TapToFlip uygulamasını aç.
2. Kullanıcıları artır / Grow users → Play Games Services → Setup and management → Configuration üzerinden mevcut uygulamaya bir oyun hizmetleri projesi bağla. Oyun hizmetleri proje ID'sini kaydet; bu, AdMob App ID veya uygulama paket adı değildir.
3. Google Cloud OAuth consent ekranını ve Android kimlik bilgilerini Console'un yönlendirmesiyle oluştur. Paket adı: `com.aliaygor.taptoflip`. Bir sunucu OAuth istemcisi, Firebase veya Gmail okuma izni gerekmez.
4. Yayınlanan uygulama için Play Console'un App integrity / App signing bölümündeki **app signing certificate SHA-1** değerini kullan; upload key SHA-1 değeriyle karıştırma. Android Studio'dan çalıştırmak için ayrıca debug sertifikasının SHA-1 değeriyle bir Android istemcisi bağla. SHA-1 değerleri `gradlew.bat signingReport` ile incelenebilir; debug keystore veya özel anahtarı paylaşma.
5. Bu puanlama sürümü için üç **yeni** liderlik tablosu oluştur: `Classic`, `Time Attack`, `Survival`. Tür: tam sayı skor; sıralama: büyük puan daha iyi / Larger is better. Google her tablo için günlük, haftalık ve tüm zamanlar aralıklarını sağlar. Mevcut eski puanlama sürümleri aynı tabloları kullanmamalı.
6. Oluşturulan üç tablo ID'sini aşağıdaki XML'e yerleştir. Proje ID'siyle karıştırma; kaynakları Console'dan dışa aktarmak mümkünse oradaki değerleri kullan.
7. Testers bölümüne test edecek Google hesabını ekle. İki ayrı oyuncu hesabıyla giriş, skor gönderimi, oyuncunun sırası ve sıralama ekranı gerçek cihazda doğrulanmalı. Oyuncuların herkese açık tabloda görünmesi Play Games profil paylaşım ayarlarına da bağlıdır.
8. Testlerden sonra PGS yapılandırmasını Console üzerinden yayımla; bu işlem uygulama APK/AAB yayınından ayrıdır. Uygulama mağaza sürümünü ayrıca normal yayın süreciyle hazırlarsın.

Kaynak dosya: `app/src/main/res/values/play_games.xml`

```xml
<resources>
    <string name="game_services_project_id" translatable="false">OYUN_HIZMETLERI_PROJE_ID</string>
    <string name="leaderboard_classic" translatable="false">CLASSIC_TABLO_ID</string>
    <string name="leaderboard_time_attack" translatable="false">TIME_ATTACK_TABLO_ID</string>
    <string name="leaderboard_survival" translatable="false">SURVIVAL_TABLO_ID</string>
</resources>
```

Örnek isimleri gerçek değerler değildir; bunları aynen kullanma. Uygulamadaki boş değerler eksik kurulumun güvenli şekilde fark edilmesini sağlar.

## Uygulamadaki davranış

- Ana menüde misafir ve Play Games'e bağlan seçenekleri vardır. Play Games profili oyuncu adını sağlar; Gmail adresi gösterilmez.
- Misafir seçimi oyun içindeki rekabeti kapatır. Google platform hesabından çıkış yapmaz; hesap yönetimi Play Games/Android ayarlarına aittir. Yeni kullanıcıya otomatik profil oluşturma ekranı zorlanmaz.
- Seçili mod için günlük, haftalık ve tüm zamanlar düğmeleri gerçek Google liderlik ekranını açar; manuel bir sıralama sunucusu eklenmedi.
- Sadece bağlantı doğrulanmışken başlanan **yeni rekabet turları** sıralamaya gönderilebilir. Tüm rekabet turları aynı başlangıç zorluğunu kullanır; erken kayıp desteği misafir/kişisel turlarda korunur.
- Reklamla devam edilen ve yarım bırakılan turlar yalnızca kişisel rekorlara sayılır. Reklam izlemek rakiplere avantaj sağlamaz. İlk ölümde sonuç otomatik gönderilmez; tur tekrar oyna/menü/ekranın kapanmasıyla sonuçlandırılır. Oyuncu devam ederse o turun tamamı sıralama dışı kalır.
- Eski cihaz rekorları, misafir turları ve farklı hesabın skorları çevrim içi tabloya taşınmaz.
- Gönderim başarısız olursa skor, başladığı oyuncu hesabına ve moda bağlı yerel kuyrukta tutulur. Aynı oyuncu yeniden doğrulandığında/menüye döndüğünde uygun skorlar tekrar denenir. Gün değiştikten sonra eski bekleyen skorun yeni günlük yarışmaya yazılmasını önlemek için kuyruk kaydı silinir; kişisel rekor kalır. Google tablolarının gün sınırı 07:00 UTC'dir; cihaz tarihine göre çalışan günlük görevlerden farklıdır.
- SDK'ya yeni günün eski yanıtı geldiğinde yeni kuyruk kaydı silinmez; bekleyen daha yüksek skor da korunur.

## Doğrulama

- `CompetitionPolicyTest.kt`: giriş/konfigürasyon eksikliği, tur uygunluğu, rewarded devam, gün sınırı ve rekabette eşit zorluk.
- `PlayerProgressTest.kt`: oyuncu/mod ayrımı, bekleyen daha yüksek skorun korunması ve eski yanıtların yeni kaydı silmemesi için Android testleri eklendi.
- Google giriş ve gerçek skor gönderimi, Console kimlikleri ve yetkili test hesabı olmadan uçtan uca doğrulanamaz. Yerel testler sunucu tarafında hile önleme garantisi değildir; Console tamper protection açık tutulmalı.

Resmî kaynaklar:
- https://developer.android.com/games/pgs/console/setup
- https://developer.android.com/games/pgs/android/android-signin
- https://developer.android.com/games/pgs/leaderboards

## Console kurulum durumu (9 Ekim 2026)

- Gerçek proje: `C:\Users\alica\AndroidStudioProjects\TapToFlip`. Geliştirmeler release 1.4 kaynak koduyla birlikte sürüm kontrolüne alındı.
- Cloud projesi `taptoflip-play-games`; oyun hizmetleri proje kimliği `101726480194`.
- OAuth uygulaması `TapToFlip`, External / In production. Geliştirici hesabı OAuth ve PGS test listelerinde mevcut. `games_lite` kapsamı kaydedildi; Games API etkin.
- Üç yayınlanmış tablo uygulamadaki XML'e bağlandı: Classic `CgkIws77-voCEAIQAA`, Time Attack `CgkIws77-voCEAIQAQ`, Survival `CgkIws77-voCEAIQAg`. Tam sayı, en yüksek önce; hile koruması açık.
- Debug Android OAuth istemcisi `101726480194-178o820a6el7krfv95b8drgoru1lqls6.apps.googleusercontent.com`; PGS kimlik bilgisi `CgkIws77-voCEAIQAw`. Debug SHA-1: `BD:55:18:0D:B1:59:A1:C3:33:46:89:A0:1F:28:0D:10:B6:1E:A3:41`.
- Play imzası Android OAuth istemcisi `101726480194-5j6ff1h6n3d3q56ijci4qi1lmirrfari.apps.googleusercontent.com`; PGS kimlik bilgisi `CgkIws77-voCEAIQBA`, yeni yüklemeler için seçildi. Play app signing SHA-1: `A6:D8:8A:9B:A4:33:1B:90:EE:D0:3B:39:37:C0:7D:48:F9:DB:2A:15`. Upload sertifikası kullanılmadı.
- Oyun adı, açıklama, Arcade kategorisi ve 512x512 simge taslak kaydedildi. Kullanıcı özellik grafiğini standart tarayıcıda kaydetti. Console yayın ekranı yenilenerek doğrulandı: zorunlu eksik kalmadı, kimlik bilgileri ve skor tabloları yayınlanmaya hazır, Yayınla düğmesi etkin.
- Mağazadaki özgün 1024x500 özellik grafiği indirildi: `play-store-assets/taptoflip-feature-1024x500.png` (875007 bayt). Kullanıcının isteğiyle Python/Pillow kullanılarak aynı boyutta JPEG kopyası oluşturuldu (180336 bayt). PNG ve JPEG yüklemelerinde Codex tarayıcısı çöktü; kullanıcı standart tarayıcıda grafiği yükleyip kaydetti; sonrasında Console yayın hazırlığı doğrulandı.
- Billing ekranı doğrudan doğrulandı: `This project has no billing account` / `This project is not linked to a billing account`. Faturalandırma hesabı, ücretli hizmet veya ücretsiz deneme açılmadı. Kanıt Codex çalışma klasöründe `tmp/taptoflip-no-billing.png`.
- Debug/release derleme, lint, 45 birim testi ve 12 farklı Android testi geçti; boş yapılandırma testi bu yapılandırmada atlandı. Release derleme imzasız doğrulama çıktısıdır.
- Kullanıcı gerçek Google hesabıyla giriş yaptı ve Google skor tablosunun açıldığını doğruladı. Yeni skorun çevrim içi görünmesi ve gerçek reklam ödülü henüz mağaza sürümünde doğrulanmadı.
- Play Games hizmetleri yayınlandı; OAuth hedef kitlesi In production durumunda doğrulandı. Kullanıcının imzalı 8 (1.4) AAB paketi, Türkçe ve İngilizce sürüm notlarıyla üretimde %100 sunum için Google incelemesine gönderildi. Yönetilen yayınlama kapalı; gönderim sırasında henüz canlıya çıkmamıştı. Kullanıcının isteğiyle dahili test yayını yapılmadı.

## Kalan işler

1. Yetkili hesapla güncel uygulamada Play Games'e bağlan, yeni turu tamamla, menüye dön ve ilgili skor tablosunda puanı doğrula. İkinci hesapla rekabet görünümünü ayrıca kontrol et.
2. Google incelemesi onaylandıktan sonra mağazadaki 1.4 sürümünü ve gerçek reklam ödülünü doğrula. OAuth ve PGS üretim yayını tamamlandı.
