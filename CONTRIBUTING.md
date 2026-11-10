# Contributing · مشارکت

[English](#english) · [فارسی](#فارسی)

## English

Thanks for taking the time to contribute! The number-one priority of this repository is **accuracy**, so please back up any calendar claim with a reliable source and any code change with tests.

### How you can help

* **Improve the documentation:** Please keep the English (`docs/en`) and Persian (`docs/fa`) pages in sync. If you're only comfortable writing in one of the two languages, just mention it in your pull request and someone else can help translate.
* **Add a new programming language** to [`implementations/`](implementations):
  1. Port the algorithm from [`docs/en/03-algorithm.md`](docs/en/03-algorithm.md). Follow your language's idiomatic naming and style,
     but keep the same core API: leap year check, days in month, date validation, to/from JDN, to/from Gregorian, day of week (0 = Saturday), and day of year.
  2. Write a test suite that reads `data/years.csv` and `data/test-vectors.csv`, round-trips every single day from 1178 to 3000 AP, and tests input validation
     (you can model it after any of the existing language tests).
  3. Add a CI job to [`.github/workflows/test.yml`](.github/workflows/test.yml) and add your language to [`implementations/README.md`](implementations/README.md).
* **Evaluate another library:** Add a test runner to [`tools/library-evaluation`](tools/library-evaluation) that outputs `results/<library>_<version>.txt`,
  run `cargo run --release -- score-libraries` inside `tools/calendar-data`, and update [page 6 of the guide](docs/en/06-languages-and-libraries.md).
* **Report bugs upstream** to library maintainers and link the issue in the docs.

### A few ground rules

* Any statement about how the calendar works needs a verifiable source (an official University of Tehran Calendar Center publication, a peer-reviewed paper, CLDR, or reproducible code).
* Never edit the files in `data/` by hand. Instead, update [`tools/calendar-data`](tools/calendar-data) and regenerate them:
  `cd tools/calendar-data && cargo run --release`. CI runs `cargo run --release -- --check` to make sure they stay in sync.
* Make sure all tests pass locally before opening a pull request.
* For holidays: keep official public holidays, non-holiday observances, and different countries/regions clearly separated, and always cite your source.
* Keep the tone practical, objective, and developer-focused.

## فارسی

<div dir="rtl">

ممنون که وقت می‌ذارید و کمک می‌کنید! اولویت اول این مخزن **درستی و دقت** اطلاعاته، پس برای هر تغییری لطفا یک منبع معتبر یا تست همراهش بذارید.

### چطور می‌تونید مشارکت کنید؟

* **اصلاح یا بهتر کردن مستندات**: صفحات انگلیسی (`docs/en`) و فارسی (`docs/fa`) رو هماهنگ نگه دارید. اگر فقط به یکی از دو زبان می‌نویسید، توی توضیحات Pull Request بگید تا بقیه در ترجمه‌ش کمک کنن.
* **اضافه کردن یک زبان برنامه‌نویسی جدید** به [`implementations/`](implementations):
  1. الگوریتم صفحهٔ [`docs/fa/03-algorithm.md`](docs/fa/03-algorithm.md) رو به زبان مورد نظرتون پیاده‌سازی کنید. قواعد نام‌گذاری و استایل همون زبان رو رعایت کنید، اما همین توابع استاندارد رو نگه دارید: تشخیص سال کبیسه، تعداد روزهای ماه، اعتبارسنجی تاریخ، تبدیل از/به JDN، تبدیل از/به میلادی، روز هفته (۰ = شنبه) و شمارهٔ روز در سال.
  2. یک فایل تست بنویسید که `data/years.csv` و `data/test-vectors.csv` رو بخونه، تک‌تک روزهای سال ۱۱۷۸ تا ۳۰۰۰ رو دوطرفه تبدیل و چک کنه و اعتبارسنجی ورودی‌ها رو هم تست کنه (می‌تونید از تست‌های زبان‌های دیگه الگو بگیرید).
  3. یک job به فایل [`.github/workflows/test.yml`](.github/workflows/test.yml) و یک سطر به جدول [`implementations/README.md`](implementations/README.md) اضافه کنید.
* **بررسی و تست یک کتابخانهٔ جدید**: یک اسکریپت در پوشهٔ [`tools/library-evaluation`](tools/library-evaluation) اضافه کنید که فایل `results/<library>_<version>.txt` رو تولید کنه، بعد توی پوشهٔ `tools/calendar-data` دستور `cargo run --release -- score-libraries` رو اجرا کنید و در نهایت صفحهٔ ۶ مستندات رو آپدیت کنید.
* **گزارش کردن باگ به سازندگان کتابخانه‌ها** و قرار دادن لینک Issue در مستندات.

### چند قانون ساده

* هر ادعا یا نکته‌ای دربارهٔ تقویم باید منبع معتبر داشته باشه (انتشارات رسمی مرکز تقویم دانشگاه تهران، مقالهٔ علمی، استاندارد CLDR یا کد قابل بازتولید).
* فایل‌های داخل پوشهٔ `data/` رو هیچ‌وقت دستی ویرایش نکنید؛ اگر تغییری لازمه، کد [`tools/calendar-data`](tools/calendar-data) رو تغییر بدید و داده‌ها رو از نو بسازید (`cd tools/calendar-data && cargo run --release`). سرویس CI هم با دستور `cargo run --release -- --check` این موضوع رو چک می‌کنه.
* قبل از فرستادن Pull Request، مطمئن بشید که تمام تست‌های کدتون روی سیستم خودتون پاس می‌شن.
* در بخش تعطیلات: تعطیلات رسمی، مناسبت‌های غیرتعطیل و کشورهای مختلف رو از هم جدا نگه دارید و همیشه منبع رو ذکر کنید.
* لحن فنی، مستند و بی‌طرفانهٔ راهنما رو حفظ کنید.

</div>
