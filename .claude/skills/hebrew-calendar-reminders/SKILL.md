---
name: hebrew-calendar-reminders
description: Rules for Hebrew-date logic (month day, 29-day months, weekday split) and Shabbat/Yom-Tov-aware reminder shifting (≥30 min before candle lighting / ≥30 min after havdalah). Use when touching calendar/, reminders/, or schedules.
---
# Hebrew calendar & reminders
- Hebrew date from `android.icu.util.HebrewCalendar` (offline, leap years correct). Hebrew day changes at **nightfall (tzeit)** optionally — setting "יום עברי מתחלף בצאת הכוכבים" (default: midnight-based civil date, as in kodesh-app).
- Monthly schedule (Tehilim by day 1–30): 1:1-9, 2:10-17, 3:18-22, 4:23-28, 5:29-34, 6:35-38, 7:39-43, 8:44-48, 9:49-54, 10:55-59, 11:60-65, 12:66-68, 13:69-71, 14:72-76, 15:77-78, 16:79-82, 17:83-87, 18:88-89, 19:90-96, 20:97-103, 21:104-105, 22:106-107, 23:108-112, 24:113-118, 25:119:1-88, 26:119:89-176, 27:120-134, 28:135-139, 29:140-144, 30:145-150.
- In a month with only 29 days, day 29 shows chapters of day 29 **and** day 30 (140-150) and is labelled "כ״ט + ל׳".
- Weekly split (Sefaria/standard): Sun 1-29, Mon 30-50, Tue 51-72, Wed 73-89, Thu 90-106, Fri 107-119, Shabbat 120-150. Verify against Sefaria on CI.
- Zmanim offline via KosherJava `ComplexZmanimCalendar` with user location/city (default Jerusalem); candle lighting = sunset − 18 (Jerusalem 40, configurable), havdalah = tzeit (configurable: 8.5° / 42 min / 72 min).
- Reminder shift rule, for any reminder whose fire time T falls on Shabbat or Yom Tov (incl. the interval from candle lighting to havdalah):
  * `before` ⇒ move to ≤ candleLighting − 30 min of that day (never later than the original T's day start if earlier; if it would pass the original time of day it stays at min(T, candleLighting − 30)).
  * `after` ⇒ move to ≥ havdalah + 30 min.
  * user chooses per reminder: before / after / both (two notifications) / skip.
  * Two consecutive holy days (Yom Tov + Shabbat): "before" = before the first candle lighting, "after" = after the last havdalah.
  * Diaspora second day (Yom Tov Sheni) is a setting (default Israel).
- Pure functions with injected `Clock`; test leap years, 29/30-day months, Rosh Hashana 2 days, Shabbat+YT chains, DST changes.
