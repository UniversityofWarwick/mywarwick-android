package uk.ac.warwick.my.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Parses the datetimes that we need to parse, but
 * not everything in ISO8601, which is why it's "rough".
 *
 * Like SimpleDateFormat, this is _not_ thread-safe.
 */
public class ISO8601RoughParserImpl implements ISO8601RoughParser {
    private final SimpleDateFormat[] formats = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.UK),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.UK),
    };

    public Date parse(String input) throws ParseException {
        ParseException lastException = null;
        for (SimpleDateFormat format : formats) {
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
