# بناء Harmonix Player بواسطة Termux وGitHub

هذه الأوامر تحدّث المستودع الموجود:

`https://github.com/omissi/harmonix-player`

ثم تراقب GitHub Actions وتنزّل ملف APK إلى مجلد Download في الهاتف.

## 1. تجهيز Termux

نفّذ الأوامر التالية بالترتيب:

```bash
termux-setup-storage
pkg update -y
pkg install -y git gh unzip
gh auth status
```

إذا ظهر أن GitHub غير متصل، نفّذ:

```bash
gh auth login
```

اختر: `GitHub.com` ثم `HTTPS` ثم تسجيل الدخول عبر المتصفح.

## 2. فك الملف وتحديث المستودع

تأكد أن الملف `Harmonix_Player_Source_v1.zip` موجود في مجلد Download، ثم نفّذ المجموعة التالية كاملة:

```bash
ZIP="$HOME/storage/downloads/Harmonix_Player_Source_v1.zip"
WORK="$HOME/harmonix-player-fix-$(date +%s)"

mkdir -p "$WORK/source"
unzip -q "$ZIP" -d "$WORK/source"

gh repo clone omissi/harmonix-player "$WORK/repo"
cp -a "$WORK/source/HarmonixPlayer/." "$WORK/repo/"

cd "$WORK/repo"
git config user.name "ALOMESSI TECH"
git config user.email "omissi@users.noreply.github.com"
git add -A
git commit -m "Fix Kotlin and Compose build errors - v1.1.1"
git push origin main
```

يبدأ البناء تلقائيًا بعد `git push`.

## 3. مراقبة البناء

نفّذ الأوامر التالية؛ ستبحث عن البناء المرتبط بآخر commit بدل اختيار بناء قديم:

```bash
HEAD_SHA=$(git rev-parse HEAD)
RUN_ID=""

for i in $(seq 1 12); do
  RUN_ID=$(gh run list --repo omissi/harmonix-player --workflow "Build Harmonix APK" --commit "$HEAD_SHA" --limit 1 --json databaseId --jq '.[0].databaseId')
  [ -n "$RUN_ID" ] && break
  sleep 5
done

[ -n "$RUN_ID" ] || { echo "لم يظهر البناء في GitHub Actions"; exit 1; }
echo "Run ID: $RUN_ID"
gh run watch "$RUN_ID" --repo omissi/harmonix-player --exit-status
```

عند ظهور `✓ Build Harmonix APK` يكون البناء ناجحًا.

## 4. تنزيل APK إلى الهاتف

```bash
APK_DIR="$HOME/storage/downloads/Harmonix_APK_v1.1.1"
mkdir -p "$APK_DIR"
gh run download "$RUN_ID" --repo omissi/harmonix-player --name Harmonix-Player-debug-apk --dir "$APK_DIR"
ls -lh "$APK_DIR"
```

ستجد التطبيق هنا:

```text
Download/Harmonix_APK_v1.1.1/app-debug.apk
```

## إذا فشل البناء

استخدم هذا الأمر لعرض الأخطاء فقط:

```bash
gh run view "$RUN_ID" --repo omissi/harmonix-player --log-failed
```

ثم انسخ الأسطر التي تبدأ بـ `e:` أو `FAILURE` لإصلاحها مباشرة.
