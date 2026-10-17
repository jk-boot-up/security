package com.jk.explore.security.backups;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Which daily snapshots to keep: the newest few days, the newest few Sundays, and the first snapshot of each of the newest few months. */
final class Retention {

    private Retention() {
    }

    static TreeSet<LocalDate> keep(List<LocalDate> days, LocalDate today, int daily, int weekly, int monthly) {
        TreeSet<LocalDate> kept = new TreeSet<>();
        days.stream().filter(day -> !day.isAfter(today) && day.isAfter(today.minusDays(daily))).forEach(kept::add);
        days.stream().filter(day -> day.getDayOfWeek() == DayOfWeek.SUNDAY).sorted(Comparator.reverseOrder()).limit(weekly).forEach(kept::add);
        Map<YearMonth, LocalDate> firstOfMonth = new TreeMap<>();
        days.stream().sorted().forEach(day -> firstOfMonth.putIfAbsent(YearMonth.from(day), day));
        firstOfMonth.entrySet().stream().sorted(Map.Entry.<YearMonth, LocalDate>comparingByKey().reversed())
                .limit(monthly).forEach(entry -> kept.add(entry.getValue()));
        return kept;
    }
}
