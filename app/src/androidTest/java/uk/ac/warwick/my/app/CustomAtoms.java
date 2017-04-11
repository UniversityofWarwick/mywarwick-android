package uk.ac.warwick.my.app;

import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.Atoms;
import android.support.test.espresso.web.model.Evaluation;

public class CustomAtoms {
    public static Atom<Integer> getScrollY() {
        return Atoms.script("return window.scrollY", Atoms.castOrDie(Integer.class));
    }

    public static Atom<Evaluation> setScrollY(int scrollY) {
        return Atoms.script(String.format("window.scrollTo(0, %d)", scrollY));
    }
}
