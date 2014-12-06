package com.espian.showcaseview;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class ShowcaseViews {

    private final List<ShowcaseView> views = new ArrayList<ShowcaseView>();
    private final List<float[]> animations = new ArrayList<float[]>();
    private final Activity activity;
    private OnShowcaseAcknowledged showcaseAcknowledgedListener = new OnShowcaseAcknowledged() {
        @Override
        public void onShowCaseAcknowledged(ShowcaseView showcaseView) {
            //DEFAULT LISTENER - DOESN'T DO ANYTHING!
        }
    };

    private static final int ABSOLUTE_COORDINATES = 0;
    private static final int RELATIVE_COORDINATES = 1;

    public interface OnShowcaseAcknowledged {
        void onShowCaseAcknowledged(ShowcaseView showcaseView);
    }

    public ShowcaseViews(Activity activity) {
        this.activity = activity;
    }

    public ShowcaseViews(Activity activity, OnShowcaseAcknowledged acknowledgedListener) {
        this(activity);
        this.showcaseAcknowledgedListener = acknowledgedListener;
    }

    public ShowcaseViews addView(ShowcaseView showcaseView) {

        showcaseView.overrideButtonClick(createShowcaseViewDismissListener(showcaseView));
        views.add(showcaseView);

        animations.add(null);

        return this;
    }

    /**
     * Add an animated gesture to the view at position viewIndex.
     * @param viewIndex     The position of the view the gesture should be added to (beginning with 0 for the view which had been added as the first one)
     * @param offsetStartX  x-offset of the start position
     * @param offsetStartY  y-offset of the start position
     * @param offsetEndX    x-offset of the end position
     * @param offsetEndY    y-offset of the end position
     * @see com.espian.showcaseview.ShowcaseView#animateGesture(float, float, float, float)
     * @see com.espian.showcaseview.ShowcaseViews#addAnimatedGestureToView(int, float, float, float, float, boolean)
     */
    public void addAnimatedGestureToView(int viewIndex, float offsetStartX, float offsetStartY, float offsetEndX, float offsetEndY) throws IndexOutOfBoundsException {
        addAnimatedGestureToView(viewIndex, offsetStartX, offsetStartY, offsetEndX, offsetEndY, false);
    }

    /**
     * Add an animated gesture to the view at position viewIndex.
     * @param viewIndex             The position of the view the gesture should be added to (beginning with 0 for the view which had been added as the first one)
     * @param startX                x-coordinate or x-offset of the start position
     * @param startY                y-coordinate or x-offset of the start position
     * @param endX                  x-coordinate or x-offset of the end position
     * @param endY                  y-coordinate or x-offset of the end position
     * @param absoluteCoordinates   If true, this will use absolute coordinates instead of coordinates relative to the center of the showcased view
     */
    public void addAnimatedGestureToView(int viewIndex, float startX, float startY, float endX, float endY, boolean absoluteCoordinates) throws IndexOutOfBoundsException {
        animations.remove(viewIndex);
        animations.add(viewIndex, new float[]{absoluteCoordinates?ABSOLUTE_COORDINATES:RELATIVE_COORDINATES, startX, startY, endX, endY});
    }

    private View.OnClickListener createShowcaseViewDismissListener(final ShowcaseView showcaseView) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showcaseView.onClick(showcaseView); //Needed for TYPE_ONE_SHOT
                int fadeOutTime = showcaseView.getConfigOptions().fadeOutDuration;
                if (fadeOutTime > 0) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNextView(showcaseView);
                        }
                    }, fadeOutTime);
                } else {
                    showNextView(showcaseView);
                }
            }
        };
    }

    private void showNextView(ShowcaseView showcaseView) {
        if (views.isEmpty()) {
            showcaseAcknowledgedListener.onShowCaseAcknowledged(showcaseView);
        } else {
            show();
        }
    }

    public void show() {
        if (views.isEmpty()) {
            return;
        }
        final ShowcaseView view = views.get(0);

        boolean hasShot = activity.getSharedPreferences(ShowcaseView.PREFS_SHOWCASE_INTERNAL, Context.MODE_PRIVATE)
                .getBoolean("hasShot" + view.getConfigOptions().showcaseId, false);
        if (hasShot && view.getConfigOptions().shotType == ShowcaseView.TYPE_ONE_SHOT) {
            // The showcase has already been shot once, so we don't need to do show it again.
            view.setVisibility(View.GONE);
            views.remove(0);
            animations.remove(0);
            view.getConfigOptions().fadeOutDuration = 0;
            view.performButtonClick();
            return;
        }

        view.setVisibility(View.INVISIBLE);
        ((ViewGroup) activity.getWindow().getDecorView()).addView(view);
        view.show();

        float[] animation = animations.get(0);
        if (animation != null) {
            view.animateGesture(animation[1], animation[2], animation[3], animation[4], animation[0] == ABSOLUTE_COORDINATES);
        }

        views.remove(0);
        animations.remove(0);
    }

    public boolean hasViews(){
        return !views.isEmpty();
    }
}
