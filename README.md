# FLOW — تمرکز کن، جریان بساز.

اپلیکیشن اندروید برای پومودورو، مدیریت وظایف، برنامه‌ریزی و آمار، با ظاهر Liquid Glass و رابط کاملاً فارسی و راست‌چین.

## تکنولوژی
- Kotlin + Jetpack Compose (اپ بومی، نه WebView)
- معماری لایه‌ای: `core` (تاریخ شمسی/فرمت) ← `domain` (موتور پومودورو، آمار، برنامه‌ریز؛ خالص و تست‌پذیر) ← `data` (مدل‌ها و ذخیره‌سازی JSON اتمیک) ← `platform` (Alarm، اعلان، صدا) ← `ui`
- تایمر بر پایه‌ی زمان دیواری (`endAt`) است نه شمارنده‌ی UI؛ پایان جلسه با `AlarmManager` دقیق ثبت می‌شود و وضعیت پس از بستن برنامه یا ری‌استارت دستگاه حفظ می‌شود.
- تقویم شمسی: الگوریتم حسابی ۳۳‌ساله با تست رفت‌وبرگشت روی هر روز ۱۹۹۰ تا ۲۰۳۰.

## اجرا و ساخت
```bash
./gradlew testDebugUnitTest          # تست‌های واحد
./gradlew assembleRelease            # APK انتشار (در build/outputs/apk/release)
```
نیازمند JDK 17 و Android SDK (platform 35).

## فونت
CI فونت Vazirmatn (OFL) را در `app/src/main/assets/fonts/` دانلود می‌کند. برای ساخت محلی، فایل‌های `Vazirmatn-Regular/Medium/Bold.ttf` را همان‌جا بگذار؛ بدون آن‌ها فونت سیستم استفاده می‌شود.

## GitHub Actions
`.github/workflows/android-apk.yml` — با Push به `main`/`master`، دستی (`workflow_dispatch`) و روی Tag به شکل `v1.2.3` اجرا می‌شود: تست ← ساخت APK ← بررسی امضا ← آپلود Artifact؛ و برای Tag در GitHub Releases منتشر می‌کند.

امضا: اگر Secrets زیر تنظیم باشند APK با Keystore شما امضا می‌شود، وگرنه با کلید debug (نصب‌شدنی برای تست، نام فایل با `-testsigned`):
`ANDROID_KEYSTORE_BASE64`، `ANDROID_KEYSTORE_PASSWORD`، `ANDROID_KEY_ALIAS`، `ANDROID_KEY_PASSWORD`

## صدای محیط
باران، طبیعت و نویزها با پردازش سیگنال داخل برنامه تولید می‌شوند (بدون فایل صوتی و بدون مشکل مجوز). «کافه» و «کتابخانه» تقریب سنتزی‌اند، نه ضبط واقعی؛ برای ضبط واقعی باید فایل دارای مجوز اضافه شود.
