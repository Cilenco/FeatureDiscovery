package com.cilenco.discoveryview;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.StringRes;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

//@SuppressWarnings("unused")
public class DiscoveryView extends View {
    private static final int TEXT_PADDING_TOP = 40; // 20
    private static final int TEXT_PADDING_LR = 40;
    private static final int TEXT_DISTANCE = 16;
    private static final int TARGET_RADIUS = 44;

    private static final int PRIMARY_TEXT_SIZE = 18;
    private static final int SECONDARY_TEXT_SIZE = 16;

    private OnDiscoveryViewClickListener listener;

    private Dialog dialog;                          // Holds the overlay dialog where the view is displayed
    private View target;                            // Holds the View target to discover
    private PointF center;                          // Represents the center of the target View

    private Paint colorPaint;                       // Used to paint the background
    private TextPaint primaryTextPaint;             // Used to paint the primaryText
    private TextPaint secondaryTextPaint;           // Used to paint the secondaryText

    private int backgroundColor;                    // Holds the backgroundColor of the View
    private int primaryTextColor;                   // Holds the primaryTextColor
    private int secondaryTextColor;                 // Holds the secondaryTextColor
    private int textAlpha;                          // Holds the textAlpha

    private String primaryText;                     // Holds the primaryText
    private String secondaryText;                   // Holds the secondaryText

    private StaticLayout primaryTextLayout;         // Used to paint the primaryText
    private StaticLayout secondaryTextLayout;       // Used to paint the secondaryText

    private float textPaddingTopDp;                 // Holds the Top text padding
    private float textPaddingLrDp;                  // Holds the text padding left and right
    private float textDistanceDp;                   // Holds the distance between primary and secondaryText

    private float primaryTextSize;                  // Holds the textSize of the primaryText
    private float secondaryTextSize;                // Holds the textSize of the secondaryText

    private float primaryTextY;
    private float secondaryTextY;

    private float bgRadius;                         // Holds the radius of the background
    private float targetRadiusDp;                   // Holds the target radius in dp
    private float animTargetRadiusDp;               // Holds the animated target radius in dp

    private float rippleWidth;                      // Holds the rippleWidth (in dp from xml)
    private int rippleAlpha;                        // Holds the rippleAlpha (0 - 255)

    private ColorFilter colorFilter;                // Used to paint the target in another color
    private Bitmap targetBitmap;                    // Used to paint the target on the view surface
    private int targetAlpha;                        // Used to animate the alpha of targetBitmap

    public interface OnDiscoveryViewClickListener {
        void onDiscoveryViewClicked(DiscoveryView discoveryView);

        void onDiscoveryViewDismissed(DiscoveryView discoveryView);
    }

    public DiscoveryView(Context context) {
        super(context);
        initialise();
    }

    public DiscoveryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    public DiscoveryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialise();
    }

    protected void initialise() {
        Resources resources = getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        textPaddingTopDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_PADDING_TOP, metrics);
        textPaddingLrDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_PADDING_LR, metrics);
        textDistanceDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_DISTANCE, metrics);
        targetRadiusDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TARGET_RADIUS, metrics);

        primaryTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, PRIMARY_TEXT_SIZE, metrics);
        secondaryTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, SECONDARY_TEXT_SIZE, metrics);

        backgroundColor = getThemeColor(R.attr.colorPrimary);
        primaryTextColor = Color.WHITE;
        secondaryTextColor = Color.WHITE;

        primaryTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        secondaryTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);

        colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorPaint.setStyle(Paint.Style.FILL);
        colorPaint.setColor(backgroundColor);

        setBackgroundColor(backgroundColor);
        setPrimaryTextSize(primaryTextSize);
        setSecondaryTextSize(secondaryTextSize);

        dialog = new Dialog(getContext(), android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(this);
        center = new PointF();
    }

    /**
     * Sets the target and so the center of highlighted area
     *
     * @param target View target of the DiscoveryView
     */
    public void setTarget(View target) {
        this.target = target;

        int pos[] = new int[2];
        target.getLocationOnScreen(pos);

        Rect rectangle = new Rect();
        Window window = ((Activity) getContext()).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;

        center.x = pos[0] + target.getWidth() / 2;
        center.y = pos[1] + target.getHeight() / 2 - statusBarHeight;

        targetBitmap = Bitmap.createBitmap(target.getWidth(), target.getHeight(), Bitmap.Config.ARGB_8888);
        target.layout(target.getLeft(), target.getTop(), target.getRight(), target.getBottom());

        Canvas c = new Canvas(targetBitmap);
        target.draw(c);
    }

    public View getTarget() {
        return target;
    }

    public void setOnDiscoveryViewClickListener(OnDiscoveryViewClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int width = (int) (w - 2 * textPaddingLrDp);

        this.primaryTextLayout = new StaticLayout(primaryText, primaryTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        this.secondaryTextLayout = new StaticLayout(secondaryText, secondaryTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        setupMetrics();
        /*float textPositionX = textPaddingLrDp;

        primaryTextY = center.y + targetRadiusDp + textPaddingTopDp;
        secondaryTextY = primaryTextY + primaryTextLayout.getHeight() + textDistanceDp;

        bgRadius = (float) sqrt(pow(center.x - textPositionX, 2) + pow(center.y - secondaryTextY - secondaryTextLayout.getHeight(), 2));
        bgRadius += textPaddingLrDp;*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        colorPaint.setColor(backgroundColor);
        colorPaint.setAlpha(245);
        canvas.drawCircle(center.x, center.y, bgRadius, colorPaint);

        colorPaint.setColor(Color.WHITE);
        colorPaint.setAlpha(255);
        if (targetRadiusDp > animTargetRadiusDp)
            canvas.drawCircle(center.x, center.y, targetRadiusDp, colorPaint);
        else canvas.drawCircle(center.x, center.y, animTargetRadiusDp, colorPaint);


        colorPaint.setAlpha(rippleAlpha);
        canvas.drawCircle(center.x, center.y, targetRadiusDp * 1.1f + rippleWidth, colorPaint);

        if (primaryTextLayout != null) {
            canvas.save();
            canvas.translate(textPaddingLrDp, primaryTextY);
            primaryTextPaint.setColor(primaryTextColor);
            primaryTextPaint.setAlpha(textAlpha);
            primaryTextLayout.draw(canvas);
            canvas.restore();
        }

        if (secondaryTextLayout != null) {
            canvas.save();
            canvas.translate(textPaddingLrDp, secondaryTextY);
            secondaryTextPaint.setColor(secondaryTextColor);
            secondaryTextPaint.setAlpha(textAlpha);
            secondaryTextLayout.draw(canvas);
            canvas.restore();
        }

        if (target != null) {
            if (colorFilter != null) colorPaint.setColorFilter(colorFilter);
            colorPaint.setAlpha(targetAlpha);
            canvas.drawBitmap(targetBitmap, center.x - target.getWidth() / 2, center.y - target.getHeight() / 2, colorPaint);
            if (colorFilter != null) colorPaint.setColorFilter(null);
        }

        // For debug options
        //colorPaint.setColor(Color.GREEN);
        //canvas.drawLine(center.x, center.y, textPaddingLrDp, primaryTextY, colorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        Rect touchArea = new Rect();
        target.getGlobalVisibleRect(touchArea);

        if (touchArea.contains(x, y)) {
            target.onTouchEvent(event);
        }

        if (listener != null) listener.onDiscoveryViewClicked(this);
        return false;
    }

    /**
     * Sets the primary text of the DiscoveryView
     *
     * @param primaryText New text which is displaced as Headline
     */
    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }

    /**
     * Returns the current primary text
     *
     * @return Current primary text
     */
    public String getPrimaryText() {
        return primaryText;
    }

    /**
     * Sets the text size of the primary text (Default 16sp)
     *
     * @param primaryTextSize The new size of the primary text
     */
    public void setPrimaryTextSize(float primaryTextSize) {
        primaryTextPaint.setTextSize(primaryTextSize);
    }

    /**
     * Sets the new color of the primary text
     *
     * @param primaryTextColor The new color of the primary text
     */
    public void setPrimaryTextColor(int primaryTextColor) {
        this.primaryTextColor = primaryTextColor;
    }

    /**
     * Sets the secondary text of the DiscoveryView
     *
     * @param secondaryText New text which is displaced as description
     */
    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }

    /**
     * Returns the current secondary text
     *
     * @return Current secondary text
     */
    public String getSecondaryText() {
        return secondaryText;
    }

    /**
     * Sets the text size of the secondary text (Default 14sp)
     *
     * @param secondaryTextSize The new size of the secondary text
     */
    public void setSecondaryTextSize(float secondaryTextSize) {
        secondaryTextPaint.setTextSize(secondaryTextSize);
    }

    /**
     * Sets the new color of the secondary text
     *
     * @param secondaryTextColor The new color of the secondary text
     */
    public void setSecondaryTextColor(int secondaryTextColor) {
        this.secondaryTextColor = secondaryTextColor;
    }

    /**
     * Sets the new color of the background and the targetView
     *
     * @param color The new color of the background
     */
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        //colorFilter = new LightingColorFilter(0, color); // TODO check if used
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.colorFilter = colorFilter;
    }

    public ColorFilter getColorFilter() {
        return colorFilter;
    }

    public boolean hasColorFilter() {
        return (colorFilter != null);
    }

    public void show(final boolean animated) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(getThemeColor(R.attr.colorPrimaryDark));
        }

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (animated) {
                    flyIn();
                    return;
                }

                targetAlpha = 255;
                animTargetRadiusDp = targetRadiusDp;
                DiscoveryView.this.invalidate();
                startPulse();
            }
        });

        dialog.show();
    }

    public void show() {
        show(true);
    }

    public void dismiss() {
        this.dismiss(true);
    }

    public void dismiss(boolean animated) {
        if (!animated) dialog.dismiss();
        else flyOut();
    }

    // At this point the target, the primary text
    // and the secondary text are set and readable
    private void setupMetrics() {
        int pos[] = new int[2];
        target.getLocationOnScreen(pos);

        Resources resources = getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();

        if (metrics.heightPixels / 2 < pos[1]) {
            if (metrics.widthPixels / 2 < pos[0]) {
                secondaryTextY = center.y - targetRadiusDp - textPaddingTopDp - secondaryTextLayout.getHeight(); //primaryTextY + primaryTextLayout.getHeight() + textDistanceDp;
                primaryTextY = secondaryTextY - textDistanceDp - primaryTextLayout.getHeight();

                bgRadius = (float) sqrt(pow(center.x - textPaddingLrDp, 2) + pow(center.y - primaryTextY - primaryTextLayout.getHeight(), 2));
            } else { // Target is on the left side of the screen
                secondaryTextY = center.y - targetRadiusDp - textPaddingTopDp - secondaryTextLayout.getHeight(); //primaryTextY + primaryTextLayout.getHeight() + textDistanceDp;
                primaryTextY = secondaryTextY - textDistanceDp - primaryTextLayout.getHeight();

                bgRadius = (float) sqrt(pow(center.x - (textPaddingLrDp + primaryTextLayout.getWidth()), 2) + pow(center.y - primaryTextY - primaryTextLayout.getHeight(), 2));
            }
        } else { // Target is on the upper half of the screen
            if (metrics.widthPixels / 2 < pos[0]) {
                primaryTextY = center.y + targetRadiusDp + textPaddingTopDp;
                secondaryTextY = primaryTextY + primaryTextLayout.getHeight() + textDistanceDp;

                bgRadius = (float) sqrt(pow(center.x - textPaddingLrDp, 2) + pow(center.y - secondaryTextY - secondaryTextLayout.getHeight(), 2));
            } else { // Target is on the left side of the screen
                primaryTextY = center.y + targetRadiusDp + textPaddingTopDp;
                secondaryTextY = primaryTextY + primaryTextLayout.getHeight() + textDistanceDp;

                bgRadius = (float) sqrt(pow(center.x - (textPaddingLrDp + primaryTextLayout.getWidth()), 2) + pow(center.y - secondaryTextY - secondaryTextLayout.getHeight(), 2));
            }
        }

        bgRadius += textPaddingLrDp;
    }

    private void flyIn() {
        AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.fly_in);
        ArrayList<Animator> animators = anim.getChildAnimations(); // Get all Animator to set them up

        ObjectAnimator bgAnimator = (ObjectAnimator) animators.get(0);
        ObjectAnimator targetAnimator = (ObjectAnimator) animators.get(1);

        bgAnimator.setFloatValues(0, bgRadius);
        targetAnimator.setFloatValues(0, targetRadiusDp);

        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                startPulse();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        anim.setTarget(this);
        anim.start();
    }

    private void flyOut() {
        AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.fly_out);
        ArrayList<Animator> animators = anim.getChildAnimations(); // Get all Animator to set them up

        ObjectAnimator bgAnimator = (ObjectAnimator) animators.get(0);
        ObjectAnimator targetAnimator = (ObjectAnimator) animators.get(1);

        bgAnimator.setFloatValues(bgRadius, 0);
        targetAnimator.setFloatValues(targetRadiusDp, 0);

        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                dialog.dismiss();
                if (listener != null) listener.onDiscoveryViewDismissed(DiscoveryView.this);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        anim.setTarget(this);
        anim.start();
    }

    private void startPulse() {
        AnimatorSet anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.pulse);

        Animator pulseAnimator = anim.getChildAnimations().get(0);
        pulseAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                startPulse();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        anim.setTarget(this);
        anim.start();
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = getContext().obtainStyledAttributes(typedValue.data, new int[]{attr});
        int color = a.getColor(0, 0);

        a.recycle();
        return color;
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    ///                                                                                  ///
    ///    Following method are only used by the Android framework for the animations    ///
    ///                                                                                  ///
    ////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused")
    private void setPulseRadius(float value) {
        animTargetRadiusDp = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setBackgroundRadius(float value) {
        bgRadius = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setTargetRadius(float value) {
        targetRadiusDp = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setTargetAlpha(int value) {
        targetAlpha = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setTextAlpha(int value) {
        textAlpha = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setRippleWidth(float value) {
        rippleWidth = value;
        this.invalidate();
    }

    @SuppressWarnings("unused")
    private void setRippleAlpha(int value) {
        rippleAlpha = value;
        this.invalidate();
    }

    public static class Builder {
        private Context context;
        private View target;

        private OnDiscoveryViewClickListener listener;

        private String primaryText;
        private String secondaryText;

        private int primaryTextSize = -1;
        private int secondaryTextSize = -1;

        private int primaryTextColor = -1;
        private int secondaryTextColor = -1;

        private int backgroundColor = -1;
        private ColorFilter colorFilter;

        public Builder(Context context, View target) {
            this.context = context;
            this.target = target;
        }

        public Builder setOnClickListener(OnDiscoveryViewClickListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setPrimaryText(@StringRes int primaryText) {
            this.primaryText = context.getString(primaryText);
            return this;
        }

        public Builder setPrimaryText(String primaryText) {
            this.primaryText = primaryText;
            return this;
        }

        public Builder setSecondaryText(@StringRes int secondaryText) {
            this.secondaryText = context.getString(secondaryText);
            return this;
        }

        public Builder setSecondaryText(String secondaryText) {
            this.secondaryText = secondaryText;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setPrimaryTextColor(int primaryTextColor) {
            this.primaryTextColor = primaryTextColor;
            return this;
        }

        public Builder setPrimaryTextSize(int primaryTextSize) {
            this.primaryTextSize = primaryTextSize;
            return this;
        }

        public Builder setSecondaryTextColor(int secondaryTextColor) {
            this.secondaryTextColor = secondaryTextColor;
            return this;
        }

        public Builder setSecondaryTextSize(int secondaryTextSize) {
            this.secondaryTextSize = secondaryTextSize;
            return this;
        }

        public Builder setColorFilter(ColorFilter colorFilter) {
            this.colorFilter = colorFilter;
            return this;
        }

        public Builder usePrimaryColorAsFilter(boolean choise) {
            if (choise) colorFilter = new LightingColorFilter(0, 0xFF3F51B5);
            return this;
        }

        public DiscoveryView build() {
            DiscoveryView v = new DiscoveryView(context);
            v.setTarget(target);

            if (primaryText != null) v.setPrimaryText(primaryText);
            if (secondaryText != null) v.setSecondaryText(secondaryText);

            if (primaryTextSize != -1) v.setPrimaryTextSize(primaryTextSize);
            if (secondaryTextSize != -1) v.setSecondaryTextSize(secondaryTextSize);

            if (primaryTextColor != -1) v.setPrimaryTextColor(primaryTextColor);
            if (secondaryTextColor != -1) v.setSecondaryTextColor(secondaryTextColor);
            if (backgroundColor != -1) v.setBackgroundColor(backgroundColor);
            if (colorFilter != null) v.setColorFilter(colorFilter);

            if (listener != null) v.setOnDiscoveryViewClickListener(listener);

            return v;
        }
    }
}