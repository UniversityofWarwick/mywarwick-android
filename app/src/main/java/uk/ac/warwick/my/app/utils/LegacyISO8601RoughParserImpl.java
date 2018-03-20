package uk.ac.warwick.my.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the datetimes that we need to parse, but
 * not everything in ISO8601, which is why it's "rough".
 *
 * Like SimpleDateFormat, this is _not_ thread-safe.
 *
 * Android API < 24 doesn't support X in format, so manually support time zone offset
 */
public class LegacyISO8601RoughParserImpl implements ISO8601RoughParser {
    private final SimpleDateFormat[] formats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.UK),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK),
    };

    private final Pattern timeZonePattern = Pattern.compile(".+([+-])(\\d\\d):?(\\d\\d)?");

    public LegacyISO8601RoughParserImpl() {
        TimeZone utcTZ = TimeZone.getTimeZone("UTC");
        for (SimpleDateFormat format : formats) {
            format.setTimeZone(utcTZ);
        }
    }

    private Date handleTimeZone(Date date, String input) throws ParseException {
        if (input.endsWith("Z")) {
            return date;
        } else {
            Matcher m = timeZonePattern.matcher(input);
            if (m.matches() && m.groupCount() >= 3) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int signMultiplier = 1;
                if ("+".equals(m.group(1))) {
                    signMultiplier = -1;
                }
                int hours = Integer.parseInt(m.group(2));
                cal.add(Calendar.HOUR, hours * signMultiplier);
                if (m.group(3) != null) {
                    int minutes = Integer.parseInt(m.group(3));
                    cal.add(Calendar.MINUTE, minutes * signMultiplier);
                }
                return cal.getTime();
            }
        }
        return date;
    }

    public Date parse(String input) throws ParseException {
        ParseException lastException = null;
        for (SimpleDateFormat format : formats) {
            try {
                return handleTimeZone(format.parse(input), input);
            } catch (ParseException e) {
                lastException = e;
            }
        }
        if (lastException == null) {
            // Shouldn't get here
            throw new IllegalStateException("Unexpected exception");
        }
        throw lastException;
    }
}