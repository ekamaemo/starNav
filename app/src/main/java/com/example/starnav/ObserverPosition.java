package com.example.starnav;

import android.os.Build;


import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;


import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

public class ObserverPosition {
    public static double getLatitude(double y){
        int Y = 992;
        float FOV = 60.0F;
        // y - координата Полярной звезды,Y - высота изображения, FOV- угловой обзор камеры по вертикали
        return (Y - y) / Y * FOV;
    }

    public static double calculateLongitude(double raCenterHours, String utcDateTime) {
        ZonedDateTime utcTime = parseUtcDateTime(utcDateTime);
        double gst = calculateGST(utcTime);
        double longitudeHours = (raCenterHours - gst) % 24;
        double longitudeDeg = longitudeHours * 15;
        if (longitudeDeg > 180) {
            longitudeDeg -= 360;
        }
        return longitudeDeg;
    }

    // Парсинг строки даты-времени в формате ISO-8601 (UTC)
    private static ZonedDateTime parseUtcDateTime(String utcDateTime) {
        return ZonedDateTime.parse(utcDateTime + "Z", DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private static double calculateGST(ZonedDateTime utcTime) {
        // Упрощённый расчёт GST (для точности нужна реализация по алгоритму Meesus)
        double jd = toJulianDate(utcTime);
        double t = (jd - 2451545.0) / 36525.0;
        double gst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * t * t - t * t * t / 38710000.0;
        return (gst % 360.0) / 15.0; // Конвертация в часы
    }

    private static double toJulianDate(ZonedDateTime date) {
        // Конвертация даты в Юлианскую (упрощённо)
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        double hour = date.getHour() + date.getMinute() / 60.0;

        if (month <= 2) {
            year--;
            month += 12;
        }
        int a = year / 100;
        int b = 2 - a + a / 4;
        return (int) (365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day + b - 1524.5 + hour / 24.0;
    }
}
