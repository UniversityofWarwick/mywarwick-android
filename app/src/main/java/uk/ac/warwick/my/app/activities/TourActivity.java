package uk.ac.warwick.my.app.activities;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.paolorotolo.appintro.AppIntro;

import uk.ac.warwick.my.app.R;


public class TourActivity extends AppIntro {

    private static final int LOCATION_PERMISSION_REQUEST = 0;

    private static final int[] LAYOUTS = new int[]{
            R.layout.fragment_tour_page_1,
            R.layout.fragment_tour_page_2,
            R.layout.fragment_tour_page_3,
            R.layout.fragment_tour_page_4,
            // R.layout.fragment_tour_page_5,
            R.layout.fragment_tour_page_6,
            R.layout.fragment_tour_page_7,
            R.layout.fragment_tour_page_8,
    };

    private boolean requestedPermissions = false;
    private boolean finishAfterPermissionRequest = false;

    public static class SlideFragment extends Fragment {

        private static String POSITION_ARG = "position";

        public static SlideFragment newInstance(int position) {
            SlideFragment f = new SlideFragment();

            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putInt(POSITION_ARG, position);
            f.setArguments(args);

            return f;
        }

        private int getPosition() {
            return getArguments().getInt(POSITION_ARG, 0);
        }

        @NonNull
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(LAYOUTS[getPosition()], container, false);
            view.setBackgroundColor(Color.WHITE);
            return view;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < LAYOUTS.length; i++) {
            addSlide(SlideFragment.newInstance(i));
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

        finishAfterPermissionRequest = true;
        requestPermissionsOnce();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);

        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);

        requestPermissionsOnce();
    }

    private void requestPermissionsOnce() {
        if (!requestedPermissions) {
            requestedPermissions = true;

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else if (finishAfterPermissionRequest) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST && finishAfterPermissionRequest)
            finish();
    }

}
