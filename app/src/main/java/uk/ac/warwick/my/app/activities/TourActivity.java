package uk.ac.warwick.my.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.paolorotolo.appintro.AppIntro;

import uk.ac.warwick.my.app.R;

public class TourActivity extends AppIntro {

    private static final int[] LAYOUTS = new int[]{
            R.layout.fragment_tour_page_1,
            R.layout.fragment_tour_page_2,
            R.layout.fragment_tour_page_3,
            R.layout.fragment_tour_page_4,
            R.layout.fragment_tour_page_5,
            R.layout.fragment_tour_page_6,
            R.layout.fragment_tour_page_7,
            R.layout.fragment_tour_page_8,
    };

    static class SlideFragment extends Fragment {
        private final int position;

        public SlideFragment(int position) {
            this.position = position;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(LAYOUTS[position], container, false);
            view.setBackgroundColor(Color.WHITE);
            return view;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < LAYOUTS.length; i++) {
            addSlide(new SlideFragment(i));
        }

        int colorPrimary = getResources().getColor(R.color.colorPrimary);

        setColorDoneText(colorPrimary);
        setColorSkipButton(colorPrimary);
        setIndicatorColor(colorPrimary, colorPrimary);
        setNextArrowColor(colorPrimary);
        setBarColor(Color.WHITE);

        setDoneText(getResources().getString(R.string.finish));
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        finish();
    }
}
