package uk.ac.warwick.my.app.services;

import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

public class DoNotDisturbService {
    private final MyWarwickPreferences preferences;

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
    private Date reschedule(Calendar cal, int startHr, int endHr) {
        int nowHr = cal.get(Calendar.HOUR_OF_DAY);

        if (endHr < startHr) { // time period spans two days
            if (nowHr >= startHr && nowHr <= 23) {
                return dateRoundedToHour(endHr, 1);
            } else if (nowHr < endHr) {
                return dateRoundedToHour(endHr, 0);
            }
        } else { // time period inside single day
            if (nowHr >= startHr && nowHr < endHr) {
                return dateRoundedToHour(endHr, 0);
            }
        }
        return null;
    }

    @Nullable
    public Date getDoNotDisturbEnd(Calendar today) {
        if (preferences.getDoNotDisturbEnabled()) {
            switch (today.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.SATURDAY:
                case Calendar.SUNDAY:
                    return reschedule(today, preferences.getDnDWeekendStart(), preferences.getDnDWeekendEnd());
                default:
                    return reschedule(today, preferences.getDnDWeekdayStart(), preferences.getDnDWeekdayEnd());
            }
        }
        return null;
    }
}
