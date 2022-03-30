package spinninghead.overlaybutton;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by donmatthews on 9/11/17.
 */

public class OverlayButtonManager {

    PendingIntent launchIntent = null;
    int iconResourceId = 0;
    int sliderButtonColor = 0;

    int orientation=0;

    float overlaySizeRatio = 0;
    int overlayX = -100001;
    int overlayY = 0;
    int screenWidth, screenHeight = 0;
    boolean onLeftSide = false;
    boolean onRightSide = false;
    int largeIconSize = 0;
    int smallIconSize = 0;
    int gutterSize = 0;
    int leftSideValue = 0;
    int rightSideValue = 0;
    int gutterHeight = 0;
    int slideCompletionValue = 0;
    int slideButtonWidth = 0;

    static long ButtonPressTime = 0;


    ImageView iconView = null;
    ImageView sliderView = null;
    ImageView dragView = null;

    //private GestureDetectorCompat gestureDetector;

    WindowManager wm;

    /**
     * Constructor for OverlayButtonManager
     * @param launchIntent the intent to be performed when overlay button is tapped
     * @param iconResourceId resource ID for the image to be used as button
     * @param sliderButtonColor resource ID for the color of the side button slider
     */
    public OverlayButtonManager(PendingIntent launchIntent, int iconResourceId, int sliderButtonColor) {
        this.launchIntent = launchIntent;

        if (iconResourceId==0) {
            this.iconResourceId = R.drawable.ic_toys_white_24dp;
        } else {
            this.iconResourceId = iconResourceId;
        }
    }

    public void updateOrientation(Context context) {

        if (iconView!=null||sliderView!=null||dragView!=null) {
            int newOrientation = getWM(context).getDefaultDisplay().getRotation();
            if (newOrientation != orientation) {
                orientation = newOrientation;
                removeViews(context);
                showNewButtonOverlay(context, 0l);
            }
        }
    }

    /**
     * Convenience method to convert DB to pixels
     * @param dp dp to be converted
     * @param context context used to retrieve Resources
     * @return pixels in the form of a float
     */
    protected static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * Retrieves the button coordinates for the appropriate screen orientation
     * @param context context used for the operation
     */
    @SuppressLint("WrongConstant")
    private void getXYLocations(Context context) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        overlaySizeRatio = sharedPref.getFloat(OverlaySettingsActivity.Overlay_Size_Ratio, 1f);

        orientation = getWM(context).getDefaultDisplay().getRotation();

        Display display = getWM(context).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        if (screenWidth<screenHeight) {
            smallIconSize = (screenWidth/15);
        } else {
            smallIconSize =  (screenHeight/15);
        }

        largeIconSize = (int) (convertDpToPixel(78, context) * overlaySizeRatio);
        gutterSize = (int) convertDpToPixel(10, context);
        slideButtonWidth = (int) convertDpToPixel(140, context);
        slideCompletionValue = (int) convertDpToPixel(140, context);
        gutterHeight = (int) convertDpToPixel(200, context);

        leftSideValue = screenWidth/-2;
        rightSideValue = screenWidth/2;

        overlayX = sharedPref.getInt("overlayX" + orientation, 0 - (screenWidth / 2) + (largeIconSize/2));
        overlayY = sharedPref.getInt("overlayY" + orientation, 0 + (screenHeight / 2) - largeIconSize);
    }

    /**
     * Recreate the overlay button to show a size change
     * @param context
     */
    protected void updateButtonSize(Context context) {
        getXYLocations(context);
        removeOverlays(context);
        showNewButtonOverlay(context, 0);
    }

    /**
     * Recreates the overlay button to update it's location on the screen.
     * @param context
     */
    protected void resetButtonPosition(Context context) {
        getXYLocations(context);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putInt("overlayX" + orientation, 0 - (screenWidth / 2) + (largeIconSize/2));
        edit.putInt("overlayY" + orientation, 0 + (screenHeight / 2) - largeIconSize);
        edit.commit();

        removeOverlays(context);
        showNewButtonOverlay(context, 0);
    }

    /**
     * Saves the X and Y coordinates of the overlay button
     * @param context
     */
    private void saveXYLocations(Context context) {

        int orientation = getWM(context).getDefaultDisplay().getRotation();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putInt("overlayX" + orientation, overlayX);
        edit.putInt("overlayY" + orientation, overlayY);
        edit.commit();
    }

    /**
     * Convenience method to retrieve WindowManager singleton
     * @param context
     * @return
     */
    private WindowManager getWM(Context context) {
        if (wm == null) {
            wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        }
        return wm;
    }

    /**
     * Performs animation to close overlay button slider
     */
    protected void animateSliderClose() {

        if (sliderView!=null) {
            final WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) sliderView.getLayoutParams());
            final WindowManager wm = getWM(sliderView.getContext());
            final int startX = prm.x;
            int endX = 0;

            if (onLeftSide) {
                endX=  leftSideValue - (slideButtonWidth / 2) + gutterSize;
            } else {
                endX = rightSideValue + (slideButtonWidth / 2) - gutterSize;
            }

            ValueAnimator animation = ValueAnimator.ofInt(startX, endX);
            animation.setDuration(400);
            animation.setInterpolator(new BounceInterpolator());
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    // You can use the animated value in a property that uses the
                    // same type as the animation. In this case, you can use the
                    // float value in the translationX property.
                    int animatedValue = (int)updatedAnimation.getAnimatedValue();

                    prm.x = animatedValue;
                    wm.updateViewLayout(sliderView, prm);
                }
            });
            animation.start();
        }
    }

    /**
     * Performs slider bounds animation
     */
    protected void animateSliderBounce() {

        if (sliderView!=null) {

            final WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) sliderView.getLayoutParams());
            final WindowManager wm = getWM(sliderView.getContext());
            final int startX = prm.x;

            ValueAnimator animation = ValueAnimator.ofInt(0, 75);
            animation.setDuration(600);
            animation.setInterpolator(new CycleInterpolator(1));
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    // You can use the animated value in a property that uses the
                    // same type as the animation. In this case, you can use the
                    // float value in the translationX property.

                    if (sliderView!=null) {
                        int animatedValue = (int) updatedAnimation.getAnimatedValue();

                        if (onLeftSide) {
                            prm.x = startX - animatedValue;
                        } else {
                            prm.x = startX + animatedValue;
                        }
                        if (sliderView != null) {
                            wm.updateViewLayout(sliderView, prm);
                        }
                    } else {
                        updatedAnimation.cancel();
                    }

                }
            });

            animation.start();
        }
    }

    /**
     * Performs overlay button spin animation
     * @param animationDelay delay in ms before animation starts
     */
    protected void animateIconSpin(long animationDelay) {

        if (iconView!=null) {

            final WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) iconView.getLayoutParams());
            final WindowManager wm = getWM(iconView.getContext());
            final int startX = prm.x;

            final float startRotation = iconView.getRotation();

            ValueAnimator animation = ValueAnimator.ofInt(0, 360);
            animation.setStartDelay(animationDelay);
            animation.setDuration(1000);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    // You can use the animated value in a property that uses the
                    // same type as the animation. In this case, you can use the
                    // float value in the translationX property.
                    int animatedValue = (int)updatedAnimation.getAnimatedValue();

                    if (iconView!=null) {

                        iconView.setRotation(startRotation + animatedValue);

                        iconView.setAlpha(animatedValue / 360f);
                    } else {
                        updatedAnimation.cancel();
                    }
                }
            });

            animation.start();
        }
    }

    /**
     * Overlay button fade in animation
     */
    protected void animateIconFadeIn() {

        System.out.println("Overlay: animateIconFadeIn");

        if (iconView!=null) {

            final WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) iconView.getLayoutParams());
            final WindowManager wm = getWM(iconView.getContext());
            final int startWidth = prm.width;
            final int startHeight = prm.height;

            if (prm.layoutAnimationParameters!=null) {
                System.out.println("Overlay: prm.flags = " + prm.layoutAnimationParameters.count);
            }

            final float startRotation = iconView.getRotation();

            ValueAnimator animation = ValueAnimator.ofFloat(0, .85f);
            animation.setDuration(2000);
            animation.setInterpolator(new AccelerateDecelerateInterpolator());
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                    // You can use the animated value in a property that uses the
                    // same type as the animation. In this case, you can use the
                    // float value in the translationX property.
                    float animatedValue = (float)updatedAnimation.getAnimatedValue();
                    iconView.setAlpha(animatedValue);
                    wm.updateViewLayout(iconView, prm);
                }
            });
            animation.start();
        }
    }

    /**
     * Performs the intent tied to the overlay button
     * @param context
     */
    private void openApp(Context context) {

        if (SystemClock.uptimeMillis() > (ButtonPressTime + 1000)) {

            ButtonPressTime = SystemClock.uptimeMillis();

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            vibrator.vibrate(50l);

            if (launchIntent != null) {
                removeViews(context);
                try {
                    launchIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
                //context.getApplicationContext().(launchIntent);
            } else {
                Toast toast = Toast.makeText(context, "Tap, Hold and Drag to reposition", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

            }
        }

    }

    /**
     * Loads and sets up the overlay button
     * @param context
     */
    private void loadViews(Context context) {

        removeViews(context);

        if (overlayX - (smallIconSize/2) < leftSideValue) {
            onLeftSide = true;
            onRightSide = false;

        } else if (overlayX + (smallIconSize/2) > rightSideValue) {
            onRightSide = true;
            onLeftSide = false;
        } else {
            onLeftSide = false;
            onRightSide = false;
        }

        if (onLeftSide && (orientation == Surface.ROTATION_270))  {
            onLeftSide = false;
        }

        if (onRightSide && (orientation == Surface.ROTATION_90)) {
            onRightSide = false;
        }

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openApp(v.getContext());
            }
        };

        LongClickListener longClickListener = new LongClickListener();

        if (onLeftSide) {
            sliderView = new ImageView(context);
            sliderView.setClickable(true);
            sliderView.setImageResource(iconResourceId);
            sliderView.setBackgroundResource(R.drawable.overlay_side_bar_left);
            sliderView.setOnTouchListener(new GutterOnTouchListener());
            sliderView.setOnLongClickListener(longClickListener);
            sliderView.setPadding(50,50,60,50);


        } else if (onRightSide) {
            sliderView = new ImageView(context);
            sliderView.setImageResource(iconResourceId);
            sliderView.setBackgroundResource(R.drawable.overlay_side_bar_right);
            sliderView.setClickable(true);
            sliderView.setOnTouchListener(new GutterOnTouchListener());
            sliderView.setOnLongClickListener(longClickListener);
            sliderView.setPadding(60,50,50,50);
        } else {
            iconView = new ImageView(context);
            iconView.setImageResource(iconResourceId);
            iconView.setClickable(true);
            iconView.setAlpha(0f);
            iconView.setOnClickListener(clickListener);
            iconView.setOnLongClickListener(longClickListener);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }


    }

    /**
     * Removes views associated with the overlay
     * @param context
     */
    synchronized private void removeViews(Context context) {

        if (iconView!=null) {
            iconView.setOnClickListener(null);
            iconView.setOnTouchListener(null);
            getWM(context).removeViewImmediate(iconView);
            iconView=null;
        }

        if (sliderView!=null) {
            sliderView.setOnClickListener(null);
            sliderView.setOnTouchListener(null);
            getWM(context).removeViewImmediate(sliderView);
            sliderView=null;
        }

        if (dragView!=null) {
            dragView.setOnClickListener(null);
            dragView.setOnTouchListener(null);
            getWM(context).removeViewImmediate(dragView);
            dragView=null;
        }

    }

    /**
     * Use this method to remove overlay buttons
     * @param context
     */
    public void removeOverlays(Context context) {
        removeViews(context);
    }

    /**
     * Shows the overlay button with applicable animation
     * @param context
     * @param animationDelay
     */
    protected void showNewButtonOverlay(Context context, long animationDelay) {

        if (SystemClock.uptimeMillis() > (ButtonPressTime + 500)) {

                getXYLocations(context);

                loadViews(context);

                WindowManager.LayoutParams params;

                int overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                }

                if (onLeftSide || onRightSide) {
                    params = new WindowManager.LayoutParams(smallIconSize, smallIconSize, overlayX, overlayY, overlayType, 0, PixelFormat.TRANSLUCENT);
                } else {
                    params = new WindowManager.LayoutParams(largeIconSize, largeIconSize, overlayX, overlayY, overlayType, 0, PixelFormat.TRANSLUCENT);
                }

                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

                if (!onLeftSide && !onRightSide) {
                    wm.addView(iconView, params);

                    animateIconSpin(animationDelay);

                } else {
                    WindowManager.LayoutParams gutterParams;
                    if (onLeftSide) {
                        gutterParams = new WindowManager.LayoutParams(slideButtonWidth, gutterHeight, leftSideValue - (slideButtonWidth/2) + gutterSize, overlayY, overlayType, 0, PixelFormat.TRANSLUCENT);
                    } else {
                        gutterParams = new WindowManager.LayoutParams(slideButtonWidth, gutterHeight, rightSideValue + (slideButtonWidth/2) - gutterSize, overlayY, overlayType, 0, PixelFormat.TRANSLUCENT);
                    }

                    gutterParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

                    wm.addView(sliderView, gutterParams);

                    animateSliderBounce();

                }

            }
    }

    /**
     * Creates view of button to be shown as user is repositioning the button
     * @param context
     */
    private void createDragView(Context context) {

        int overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        dragView = new ImageView(context);
        dragView.setImageResource(iconResourceId);
        dragView.setPadding(10,10,10,10);
        dragView.setClickable(true);
        dragView.setAlpha(.85f);
        dragView.setBackgroundColor(Color.parseColor("#AAFF0000"));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(largeIconSize+40, largeIconSize+40, overlayX, overlayY, overlayType, 0, PixelFormat.TRANSLUCENT);

        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        wm.addView(dragView,params);

    }

    private void removeDragView() {
        wm.removeView(dragView);
        dragView=null;
    }

    /**
     * LongClick listener for overlay button used for repositioning button
     */
    private class LongClickListener implements View.OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {

            ClipData.Item item = new ClipData.Item("overlay");

            ClipData dragData = ClipData.newPlainText("overlay", "overlay");

            if (dragView==null) {
                createDragView(v.getContext());
            }

            v.setOnTouchListener(new View.OnTouchListener() {
                boolean touchconsumedbyMove = false;
                int recButtonLastX;
                int recButtonLastY;
                int recButtonFirstX;
                int recButtonFirstY;

                @TargetApi(Build.VERSION_CODES.FROYO)
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) dragView.getLayoutParams());
                    int totalDeltaX = recButtonLastX - recButtonFirstX;
                    int totalDeltaY = recButtonLastY - recButtonFirstY;

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            recButtonLastX = (int) event.getRawX();
                            recButtonLastY = (int) event.getRawY();
                            recButtonFirstX = recButtonLastX;
                            recButtonFirstY = recButtonLastY;

                            break;
                        case MotionEvent.ACTION_UP:

                            saveXYLocations(v.getContext());
                            //removeDragView();
                            removeViews(v.getContext());
                            showNewButtonOverlay(v.getContext().getApplicationContext(), 0l);

                            break;
                        case MotionEvent.ACTION_MOVE:
                            int deltaX = (int) event.getRawX() - recButtonLastX;
                            int deltaY = (int) event.getRawY() - recButtonLastY;
                            recButtonLastX = (int) event.getRawX();
                            recButtonLastY = (int) event.getRawY();
                            if (Math.abs(totalDeltaX) >= 5 || Math.abs(totalDeltaY) >= 5) {
                                if (event.getPointerCount() == 1) {
                                    prm.x += deltaX;
                                    prm.y += deltaY;
                                    touchconsumedbyMove = true;
                                    //viewGroup.setColorFilter(Color.RED);
                                    //v.invalidate();

                                    wm.updateViewLayout(dragView, prm);

                                    overlayX = prm.x;
                                    overlayY = prm.y;
                                } else {
                                    touchconsumedbyMove = false;
                                }
                            } else {
                                touchconsumedbyMove = false;
                            }
                            break;
                        default:
                            break;
                    }
                    return touchconsumedbyMove;
                }
            });
            return false;
        }
    };

    private static Point getTouchPositionFromDragEvent(View item, DragEvent event) {
        Rect rItem = new Rect();
        item.getGlobalVisibleRect(rItem);
        return new Point(rItem.left + Math.round(event.getX()), rItem.top + Math.round(event.getY()));
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        Context context;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

    }

    /**
     * Touch Listener used to handle the overlay button when it is used on the sides (gutter) of the screen.
     */
    class GutterOnTouchListener implements View.OnTouchListener {

        boolean touchconsumedbyMove = false;
        int recButtonLastX;
        int recButtonLastY;
        int recButtonFirstX;
        int recButtonFirstY;
        long downTime = 0;
        Handler mHandler = null;
        View theView = null;
        boolean triggered = false;


        private void cleanUp ()  {
            theView = null;
            if (mHandler!=null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
        }

        private void performLongClick() {
            if (theView!=null) {
                theView.performLongClick();
            }
        }

        private void vibrate(Context context) {

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(20l);

        }

        @TargetApi(Build.VERSION_CODES.FROYO)
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            WindowManager.LayoutParams prm = ((WindowManager.LayoutParams) v.getLayoutParams());
            //WindowManager.LayoutParams iconPrm = ((WindowManager.LayoutParams) views.get(0).getLayoutParams());
            int totalDeltaX = recButtonLastX - recButtonFirstX;
            int totalDeltaY = recButtonLastY - recButtonFirstY;


            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:

                    theView = v;

                    triggered = false;

                    mHandler = new Handler(Looper.getMainLooper()) {

                        @Override
                        public void handleMessage(Message msg) {

                            System.out.println("Message: ");

                            if (SystemClock.uptimeMillis() > downTime + 600) {
                                mHandler.removeCallbacksAndMessages(null);
                                mHandler = null;
                                performLongClick();
                            } else {
                                mHandler.sendEmptyMessageDelayed(0, 300);
                            }
                        }
                    };

                    mHandler.sendEmptyMessageDelayed(0, 300);

                    recButtonLastX = (int) event.getRawX();
                    recButtonLastY = (int) event.getRawY();
                    recButtonFirstX = recButtonLastX;
                    recButtonFirstY = recButtonLastY;

                    downTime = SystemClock.uptimeMillis();
                    System.out.println("Updating Downtime");

                    //vibrate(v.getContext());

                    break;
                case MotionEvent.ACTION_UP:

                    cleanUp();
                    //removeViews(v.getContext());
                    //showNewButtonOverlay(v.getContext().getApplicationContext());
                    if (triggered) {
                        triggered = false;
                        openApp(v.getContext());
                    }

                    animateSliderClose();

                    break;
                case MotionEvent.ACTION_MOVE:
                    int deltaX = (int) event.getRawX() - recButtonLastX;
                    int deltaY = (int) event.getRawY() - recButtonLastY;
                    recButtonLastX = (int) event.getRawX();
                    recButtonLastY = (int) event.getRawY();
                    if (Math.abs(totalDeltaX) >= 30 || Math.abs(totalDeltaY) >= 200) {
                        if (event.getPointerCount() == 1) {

                            if (Math.abs(totalDeltaX) >= 150) {
                                downTime = SystemClock.uptimeMillis();
                                System.out.println("Updating Downtime");
                            }

                            int newX =  prm.x + deltaX;

                            touchconsumedbyMove = true;

                            if (onLeftSide && newX > leftSideValue + (slideCompletionValue/2)) {
                                cleanUp();
                                if(!triggered) {
                                    vibrate(v.getContext());
                                }
                                triggered = true;

                            } else if (onRightSide && newX < rightSideValue - (slideCompletionValue/2) ) {
                                cleanUp();
                                if(!triggered) {
                                    vibrate(v.getContext());
                                }
                                triggered = true;

                            } else {

                                prm.x = newX;

                                wm.updateViewLayout(v, prm);
                                //wm.updateViewLayout(views.get(0), iconPrm);
                            }

                        } else {

                        }
                    } else {

                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    };

}
