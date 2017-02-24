package uk.ac.warwick.my.app.helper;

public class Objects {

    /**
     * NEWSTART-535
     * java 7 has the same functionality, but require android api level 17
     * it doesn't seem worth excluding users just to save a couple of lines of code.
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
