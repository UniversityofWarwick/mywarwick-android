package uk.ac.warwick.my.app.services;

import android.content.Context;
import android.content.SharedPreferences;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.util.TimeZone;

import uk.ac.warwick.my.app.bridge.MyWarwickPreferences;
import uk.ac.warwick.my.app.data.Event;

@RunWith(MockitoJUnitRunner.class)
public class EventNotificationServiceTest {
    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences features;

    @Mock
    private Context ctx;

    private EventNotificationService service;

    @Before
    public void before() {
         this.service = new EventNotificationService(ctx, new MyWarwickPreferences(ctx, prefs, features));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testEventWithNullLocation() throws JSONException, ParseException {
        String src = "{\n" +
                "   \"id\":\"15-2f8bbc46-1b71-4c0e-a9c6-6140db61a1b6\",\n" +
                "   \"start\":\"2019-01-07T14:00:00.000Z\",\n" +
                "   \"end\":\"2019-01-07T15:00:00.000Z\",\n" +
                "   \"isAllDay\":false,\n" +
                "   \"title\":\"CS.personal tutor meeting Term 2 yr 3\",\n" +
                "   \"type\":\"Meeting\",\n" +
                "   \"parent\":{\n" +
                "      \"shortName\":\"CS.GENERAL\",\n" +
                "      \"fullName\":\"Computer Science\"\n" +
                "   },\n" +
                "   \"staff\":[\n" +
                "\n" +
                "   ],\n" +
                "   \"academicWeek\":15\n" +
                "}";
        Event ev = (new EventFetcher(ctx, new MyWarwickPreferences(ctx, prefs, features))).buildEvent(new JSONObject(src));
        String text = this.service.getNotificationText(ev);
        Assert.assertFalse(text.contains("null"));
    }

    @Test
    public void testNormalEvent() throws JSONException, ParseException {
        String src = "{\n" +
                "   \"id\":\"1-7bcf9c3658f9ffaf03d39cfccb92338e\",\n" +
                "   \"start\":\"2018-10-04T09:00:00.000Z\",\n" +
                "   \"end\":\"2018-10-04T10:00:00.000Z\",\n" +
                "   \"isAllDay\":false,\n" +
                "   \"title\":\"CS915L\",\n" +
                "   \"location\":{\n" +
                "      \"name\":\"CS1.04\",\n" +
                "      \"href\":\"https://campus.warwick.ac.uk/?slid=26858\"\n" +
                "   },\n" +
                "   \"type\":\"Lecture\",\n" +
                "   \"parent\":{\n" +
                "      \"shortName\":\"CS915\",\n" +
                "      \"fullName\":\"Advanced Computer Security\"\n" +
                "   },\n" +
                "   \"staff\":[\n" +
                "      {\n" +
                "         \"firstName\":\"Arshad\",\n" +
                "         \"lastName\":\"Jhumka\",\n" +
                "         \"email\":\"H.A.Jhumka@warwick.ac.uk\",\n" +
                "         \"userType\":\"Staff\",\n" +
                "         \"universityId\":\"0381471\"\n" +
                "      }\n" +
                "   ],\n" +
                "   \"academicWeek\":1\n" +
                "}";
        Event ev = (new EventFetcher(ctx, new MyWarwickPreferences(ctx, prefs, features))).buildEvent(new JSONObject(src));
        String text = this.service.getNotificationText(ev);
        Assert.assertEquals("CS1.04, 09:00 – 10:00", text);
    }
}
