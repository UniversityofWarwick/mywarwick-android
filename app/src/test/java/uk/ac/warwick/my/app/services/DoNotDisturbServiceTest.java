package uk.ac.warwick.my.app.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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

    private void mockDoNotDisturb(int start, int end, boolean dndEnabled) {
        Mockito.when(prefs.getDoNotDisturbEnabled()).thenReturn(dndEnabled);
        Mockito.when(prefs.getDnDWeekendStart()).thenReturn(start);
        Mockito.when(prefs.getDnDWeekendEnd()).thenReturn(end);
        Mockito.when(prefs.getDnDWeekdayStart()).thenReturn(start);
        Mockito.when(prefs.getDnDWeekdayEnd()).thenReturn(end);
    }

    private static Calendar setHr(Calendar cal, int hour) {
        // Thursday 7 June 2018
        cal.set(2018, 5, 7, hour, 0);
        return cal;
    }

    private static int getHourFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    private static int getDayOfWeekFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    private void runTest(int nowHr, int startHr, int endHr, boolean assertNextDay, boolean assertNullDate) {
        mockDoNotDisturb(startHr, endHr, true);

        Calendar cal = setHr(Calendar.getInstance(), nowHr);
        Date date = service.getDoNotDisturbEnd(cal);

        if (assertNullDate) {
            assertNull("Expected date to be null, signaling reschedule not required", date);
        } else {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int expectedOutputDay = assertNextDay ? (dayOfWeek % 7) + 1 : dayOfWeek;

            assertEquals("Expected correct hour in rescheduled date", endHr, getHourFromDate(date));
            assertEquals("Expected correct day of week in rescheduled date", expectedOutputDay, getDayOfWeekFromDate(date));
        }
    }

    @Test
    public void dndIsDisabled() {
        int nowHr = 2;
        int startHr = 1;
        int endHr = 3;
        mockDoNotDisturb(startHr, endHr, false);

        Calendar cal = setHr(Calendar.getInstance(), nowHr);
        assertNull("Expected date to be null when DnD is disabled", service.getDoNotDisturbEnd(cal));
    }

    @Test
    public void doNotReschedule() {
        runTest(19, 12, 18, false, true);
    }

    @Test
    public void sameDayReschedule() {
        runTest(15, 12, 18, false, false);
    }

    @Test
    public void testZeroEndHourWithResched() {
        runTest(23, 21, 0, true, false);
    }

    @Test
    public void testZeroEndHourNoResched() {
        runTest(1, 21, 0, false, true);
    }

    @Test
    public void testZeroStartHourWithResched() {
        runTest(0, 0, 7, false, false);
    }

    @Test
    public void testZeroStartHourNoResched() {
        runTest(23, 0, 7, false, true);
    }

    @Test
    public void spanDaysRescheduleSameDay() {
        runTest(4, 21, 7, false, false);
    }

    @Test
    public void spanDaysRescheduleNextDay() {
        runTest(23, 21, 7, true, false);
    }
}
