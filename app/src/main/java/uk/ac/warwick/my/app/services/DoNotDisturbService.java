package uk.ac.warwick.my.app.services;

import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

public class DoNotDisturbService {
    private final MyWarwickPreferences preferences;
    private static final Pattern timeRegex = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    DoNotDisturbService(final MyWarwickPreferences preferences) {
        this.preferences = preferences;
    }

    private Date dateRoundedToHour(int hour, int plusDays) {
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_WEEK, plusDays);
        endCal.set(Calendar.HOUR_OF_DAY, hour);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);
        return endCal.getTime();
    }

    @Nullable
    private Date reschedule(Calendar cal, String start, String end) {
        if (!start.matches(timeRegex.pattern()) || !end.matches(timeRegex.pattern())) {
            Crashlytics.logException(new IllegalArgumentException("Expected times in 24 hour format (\"HH:mm\") but was " + start + " and " + end));
        }
        Calendar calClone = (Calendar) cal.clone();
        String[] startTime = start.split(":");
        String[] endTime = end.split(":");

        // TODO: minute value from HH:mm string is currently ignored
        int startHr = Integer.parseInt(startTime[0]);
        int endHr = Integer.parseInt(endTime[0]);
        int nowHr = calClone.get(Calendar.HOUR_OF_DAY);

        if (endHr < startHr) { // time period spans two days
            if (nowHr >= startHr) {
                return dateRoundedToHour(endHr, 1);
            } else if (nowHr < endHr) {
                return dateRoundedToHour(endHr, 0);
            }
        } else if (nowHr >= startHr && nowHr < endHr) { // time period inside single day
                return dateRoundedToHour(endHr, 0);
        }
        return null;
    }

    @Nullable
    public Date getDoNotDisturbEnd(Calendar today) {
        if (preferences.getDoNotDisturbEnabled()) {
            return reschedule(today, preferences.getDnDWeekendStart(), preferences.getDnDWeekendEnd());
        }
        return null;
    }
}
