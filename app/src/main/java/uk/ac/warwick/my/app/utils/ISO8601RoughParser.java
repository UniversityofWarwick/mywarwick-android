package uk.ac.warwick.my.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Parses the datetimes that we need to parse, but
 * not everything in ISO8601, which is why it's "rough".
 *
 * Like SimpleDateFormat, this is _not_ thread-safe.
 *
 * Android API < 24 doesn't support X in format, so manually support Zulu, GMT, and BST
 */
public class ISO8601RoughParser {
    private final SimpleDateFormat[] zuluFormats = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK),
    };

    private final SimpleDateFormat[] gmtFormats = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+0000'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'", Locale.UK),
    };

    private final SimpleDateFormat[] bstFormats = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+01:00'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+01:00'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+0100'", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+0100'", Locale.UK),
    };

    public ISO8601RoughParser() {
        TimeZone utcTZ = TimeZone.getTimeZone("UTC");
        for (SimpleDateFormat format : zuluFormats) {
            format.setTimeZone(utcTZ);
        }
    }

    public Date parse(String input) throws ParseException {
        ParseException lastException = null;
        for (SimpleDateFormat format : zuluFormats) {
            try {
                return format.parse(input);
            } catch (ParseException e) {
                lastException = e;
            }
        }
        for (SimpleDateFormat format : gmtFormats) {
            try {
                return format.parse(input);
            } catch (ParseException e) {
                lastException = e;
            }
        }
        for (SimpleDateFormat format : bstFormats) {
            try {
                return format.parse(input);
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
