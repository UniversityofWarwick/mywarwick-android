package uk.ac.warwick.my.app.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class DoNotDisturbServiceTest {

    @Mock
    private MyWarwickPreferences prefs;

    private DoNotDisturbService service;

    @Before
    public void before() {
        service = new DoNotDisturbService(prefs);
    }

    private void mockDoNotDisturb(String start, String end, boolean dndEnabled) {
        Mockito.when(prefs.getDoNotDisturbEnabled()).thenReturn(dndEnabled);
        Mockito.when(prefs.getDnDWeekendStart()).thenReturn(start);
        Mockito.when(prefs.getDnDWeekendEnd()).thenReturn(end);
        Mockito.when(prefs.getDnDWeekdayStart()).thenReturn(start);
        Mockito.when(prefs.getDnDWeekdayEnd()).thenReturn(end);
    }

    private static Calendar setHr(Calendar cal, int hour) {
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static String getTimeFromDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(date);
    }

    private static int getDayOfWeekFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private void runTest(int nowHr, String startHr, String endHr, boolean assertNextDay, boolean assertNullDate) {
        mockDoNotDisturb(startHr, endHr, true);

        Calendar cal = setHr(Calendar.getInstance(), nowHr);
        Date date = service.getDoNotDisturbEnd(cal);

        if (assertNullDate) {
            assertNull("Expected date to be null, signaling reschedule not required", date);
        } else {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int expectedOutputDay = assertNextDay ? (dayOfWeek % 7) + 1 : dayOfWeek;

            assertEquals("Expected correct hour in rescheduled date", endHr, getTimeFromDate(date));
            assertEquals("Expected correct day of week in rescheduled date", expectedOutputDay, getDayOfWeekFromDate(date));
        }
    }

    @Test
    public void dndIsDisabled() {
        int nowHr = 2;
        String startTime = "01:00";
        String endTime = "03:00";
        mockDoNotDisturb(startTime, endTime, false);

        Calendar cal = setHr(Calendar.getInstance(), nowHr);
        assertNull("Expected date to be null when DnD is disabled", service.getDoNotDisturbEnd(cal));
    }

    @Test
    public void doNotReschedule() {
        runTest(19, "12:00", "18:00", false, true);
    }

    @Test
    public void sameDayReschedule() {
        runTest(15, "12:00", "18:00", false, false);
    }

    @Test
    public void testZeroEndHourWithResched() {
        runTest(23, "21:00", "00:00", true, false);
    }

    @Test
    public void testZeroEndHourNoResched() {
        runTest(1, "21:00", "00:00", false, true);
    }

    @Test
    public void testZeroStartHourWithResched() {
        runTest(0, "00:00", "07:00", false, false);
    }

    @Test
    public void testZeroStartHourNoResched() {
        runTest(23, "00:00", "07:00", false, true);
    }

    @Test
    public void spanDaysRescheduleSameDay() {
        runTest(4, "21:00", "07:00", false, false);
    }

    @Test
    public void spanDaysRescheduleNextDay() {
        runTest(23, "21:00", "07:00", true, false);
    }
}
