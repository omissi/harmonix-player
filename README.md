# Harmonix Player

مشغّل موسيقى محلي احترافي لنظام Android، مبني باستخدام Kotlin وJetpack Compose وMedia3/ExoPlayer. المشروع مستوحى وظيفيًا من تطبيقات تشغيل الموسيقى الحديثة، لكنه يستخدم اسمًا وهوية وكودًا وأصولًا أصلية خاصة به.

## المزايا المنفذة

- فحص ملفات الصوت المحلية تلقائيًا عبر MediaStore.
- دعم Android 7.0 حتى Android 16 (`minSdk 24` / `targetSdk 36`).
- تشغيل MP3 وAAC وFLAC وWAV وOGG والصيغ التي يدعمها الجهاز وMedia3.
- تشغيل في الخلفية مع إشعار تحكم وشاشة القفل وسماعات Bluetooth.
- مكتبة حسب الأغاني، الألبومات، الفنانين والمجلدات.
- تصنيفات موسيقية Genres على Android الحديث مع توافق رجعي للأجهزة القديمة.
- بحث سريع وفرز حسب العنوان والفنان والألبوم والتاريخ والمدة.
- شاشة تشغيل كاملة مع التقديم، العشوائي والتكرار.
- Mini Player وقائمة انتظار قابلة للتنقل.
- مفضلة، سجل استماع، الأكثر تشغيلًا وقوائم تشغيل مخصصة.
- Equalizer فعلي متعدد النطاقات مع Bass Boost وإعدادات جاهزة.
- Virtualizer وReverb قابلان للتخصيص مع حفظ إعدادات الصوت.
- مؤقت نوم 15–90 دقيقة.
- سرعة تشغيل من 0.5× إلى 2× وحفظ آخر قائمة وموضع تشغيل.
- مشاركة ملفات الصوت وتعيين الأغنية كنغمة رنين.
- كلمات محلية يضيفها المستخدم، وتحرير العنوان والفنان والألبوم والنوع والغلاف داخل التطبيق.
- إخفاء الأغاني وإدارتها من شاشة مستقلة.
- Widget موسيقي للشاشة الرئيسية بأزرار السابق والتشغيل/الإيقاف والتالي.
- نسخ احتياطي واستعادة للقوائم والمفضلة والسجل والكلمات والتعديلات والمخفي بصيغة JSON.
- واجهة عربية RTL وإنجليزية، وضع فاتح/داكن وأربعة ألوان للهوية.
- بدون إعلانات وبدون رفع ملفات المستخدم إلى الإنترنت.

## بنية المشروع

- `data/`: فحص MediaStore والنماذج وحفظ القوائم والإعدادات.
- `player/`: ExoPlayer وMediaSessionService والـ Equalizer.
- `ui/`: واجهات Compose والشاشات والمكونات.
- `.github/workflows/build-apk.yml`: بناء واختبار APK تلقائيًا.

## البناء عبر GitHub Actions من Termux

بعد فك ضغط المشروع داخل مجلد Download:

```bash
termux-setup-storage
pkg update -y
pkg install -y git gh unzip

cd ~/storage/downloads/HarmonixPlayer
git init
git branch -M main
git add .
git commit -m "Initial Harmonix Player app"
gh repo create omissi/harmonix-player --public --source=. --remote=origin --push
```

بعد الرفع افتح المستودع ثم: `Actions` → `Build Harmonix APK` → `Run workflow`. بعد نجاح البناء ستجد الملف داخل `Artifacts` باسم `Harmonix-Player-debug-apk`.

لتشغيل بناء جديد بعد أي تعديل:

```bash
git add .
git commit -m "Update Harmonix Player"
git push origin main
```

## البناء في Android Studio

1. افتح مجلد `HarmonixPlayer`.
2. استخدم JDK 17 واترك Android Studio ينفذ Gradle Sync.
3. ثبّت Android SDK Platform 36 وBuild Tools 36.0.0 عند الطلب.
4. اختر `Build` → `Build APK(s)`.

ويمكن البناء من الطرفية بعد تثبيت JDK 17 وAndroid SDK 36:

```bash
./gradlew testDebugUnitTest assembleDebug
```

الـGradle Wrapper مضمّن داخل المشروع، لذلك لا تحتاج إلى تثبيت Gradle يدويًا.

## ملاحظات النشر

- غيّر `applicationId` قبل النشر إذا رغبت باسم حزمة مختلف.
- أنشئ مفتاح توقيع Release خاصًا بك ولا ترفعه إلى GitHub.
- أضف سياسة خصوصية واضحة عند النشر على Google Play.
- لا يتضمن المشروع أي اسم أو شعار أو لقطة شاشة من التطبيق المرجعي.
