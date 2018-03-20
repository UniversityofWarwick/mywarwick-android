package uk.ac.warwick.my.app.utils;

import java.text.ParseException;
import java.util.Date;

public interface ISO8601RoughParser {

    Date parse(String input) throws ParseException;

}
