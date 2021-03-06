package com.angcyo.uiview.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.utils.ScreenUtil;
import com.angcyo.uiview.view.ILifecycle;
import com.angcyo.uiview.view.UIIViewImpl;
import com.orhanobut.hawk.Hawk;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：支持透明状态栏, 支持Dialog, 支持动画
 * 创建人员：Robi
 * 创建时间：2016/12/21 9:01
 * 修改人员：Robi
 * 修改时间：2016/12/21 9:01
 * 修改备注：
 * Version: 1.0.0
 */
public class RSoftInputLayout extends FrameLayout implements ILifecycle {

    public static final String KEY_KEYBOARD_HEIGHT = "key_keyboard_height";

    private static final String TAG = "Robi";
    View contentLayout;
    View emojiLayout;
    /**
     * 缺省的键盘高度
     */
    int keyboardHeight = 0;
    /**
     * 需要显示的键盘高度,可以指定显示多高
     */
    int showEmojiHeight = 0;
    /**
     * 动画执行时, 表情显示的高度
     */
    int animShowEmojiHeight = -1;
    boolean isEmojiShow = false;
    HashSet<OnEmojiLayoutChangeListener> mEmojiLayoutChangeListeners = new HashSet<>();
    /**
     * 需要隐藏键盘, 当为true时, 不用代码检查键盘是否显示
     */
    boolean needHideSoftInput = false;
    /**
     * 是否是需要显示表情布局
     */
    boolean needShowEmojiLayout = false;
    /**
     * 键盘是否显示
     */
    private boolean isKeyboardShow = false;
    private boolean mClipToPadding;
    private ValueAnimator mValueAnimator;
    /**
     * 使用动画的形式展开表情布局
     */
    private boolean isAnimToShow = true;
    /**
     * 所在的界面,是否隐藏了. 隐藏了,不处理事件
     */
    private boolean isViewHide = false;
    /**
     * 是否需要处理键盘
     */
    private boolean enableSoftInput = true;
    private Runnable mCheckSizeChanged = new Runnable() {
        @Override
        public void run() {
            onSizeChanged(-1, -1, 0, 0);
        }
    };

    public RSoftInputLayout(Context context) {
        super(context);
    }

    public RSoftInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RSoftInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RSoftInputLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public static void hideSoftInput(View view) {
        InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showSoftInput(View view) {
        view.requestFocus();
        InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.showSoftInput(view, 0);
    }

    public static int getSoftKeyboardHeight(View view) {
        int screenHeight = view.getResources().getDisplayMetrics().heightPixels;
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        int visibleBottom = rect.bottom;
        return screenHeight - visibleBottom;
    }

    public void setEnableSoftInput(boolean enableSoftInput) {
        this.enableSoftInput = enableSoftInput;
        if (enableSoftInput) {
            setFitsSystemWindows();
        } else {
            setFitsSystemWindows(false);
        }
    }

    public void setAnimToShow(boolean animToShow) {
        isAnimToShow = animToShow;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        setKeyboardHeight(Hawk.get(KEY_KEYBOARD_HEIGHT, 0));

        if (!isInEditMode() && isEnabled()) {
            setFitsSystemWindows();
            setClipToPadding(false);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            setFitsSystemWindows();
        } else {
            setFitsSystemWindows(false);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        int childCount = getChildCount();
        /*请按顺序布局*/
        if (childCount > 0) {
            contentLayout = getChildAt(0);
        }
        if (childCount > 1) {
            emojiLayout = getChildAt(1);
        }

//        if (haveChildSoftInput(child)) {
//            onLifeViewHide();
//        }
    }

    /**
     * 解决RSoftInputLayout嵌套RSoftInputLayout的问题
     */
    private boolean haveChildSoftInput(View child) {
        if (child instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) child).getChildCount(); i++) {
                return haveChildSoftInput(((ViewGroup) child).getChildAt(i));
            }
        }
        return child instanceof RSoftInputLayout;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!enableSoftInput ||
                isViewHide ||
                contentLayout == null ||
                !isEnabled()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
//        int maxHeight = heightSize - getPaddingBottom() - getPaddingTop();
        int maxHeight = heightSize - getPaddingTop();
        int paddingBottom = getPaddingBottom();

//        L.w("height:" + heightSize + " paddBottom:" + paddingBottom);

        if (isInEditMode()) {
            contentLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
            if (emojiLayout != null) {
                emojiLayout.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(widthSize, heightSize);
            return;
        }

        //星期一 2017-8-14
//        isKeyboardShow = isSoftKeyboardShow();
//        if (isKeyboardShow) {
//            keyboardHeight = getSoftKeyboardHeight();
//            //isEmojiShow = false;
//        }
//
//        int contentHeight;
//        int emojiHeight;
//
//        if (isEmojiShow) {
//            int showEmojiHeight = getShowEmojiHeight();
//            if (showEmojiHeight == 0) {
//                emojiHeight = keyboardHeight;
//            } else {
//                emojiHeight = showEmojiHeight;
//            }
//        } else {
//            emojiHeight = 0;
//        }
//
//        if (isKeyboardShow) {
//            if (maxHeight + keyboardHeight > getScreenHeightPixels()) {
//                contentHeight = maxHeight - keyboardHeight;
//            } else {
//                contentHeight = maxHeight;
//            }
//        } else {
//            contentHeight = maxHeight - emojiHeight;
//        }
//
//        contentLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));
//        if (isEmojiShow && emojiLayout != null) {
//            emojiLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
//                    MeasureSpec.makeMeasureSpec(keyboardHeight /*emojiHeight*/, MeasureSpec.EXACTLY));
//        }

        //键盘是否显示
        boolean softKeyboardShow = isSoftKeyboardShow();
        //是否需要显示表情
        int contentLayoutHeight, emojiLayoutHeight, keyboardHeight;
        keyboardHeight = getSoftKeyboardHeight();

        //L.e("call: -> softKeyboardShow:" + softKeyboardShow + " needShowEmojiLayout:" + needShowEmojiLayout + " needHideSoftInputForStart:" + needHideSoftInputForStart);

        //如果键盘显示了, 那么内容的高度=总高度-键盘的高度
        if (softKeyboardShow) {
            isEmojiShow = false;
            if (UIIViewImpl.isLollipop()) {
                contentLayoutHeight = maxHeight - keyboardHeight;
            } else {
                contentLayoutHeight = maxHeight;
            }
            emojiLayoutHeight = keyboardHeight;
            this.keyboardHeight = keyboardHeight;//缓存键盘的高度
        } else if (needShowEmojiLayout) {
            //如果需要显示表情
            isEmojiShow = true;
            if (animShowEmojiHeight > 0) {
                contentLayoutHeight = maxHeight - animShowEmojiHeight;
            } else {
                contentLayoutHeight = maxHeight - showEmojiHeight;
                //emojiLayoutHeight = getKeyboardHeight();//键盘的高度, 如果之前还没有缓存键盘的高度, 那么就用默认的高度
            }
            emojiLayoutHeight = showEmojiHeight;
        } else {
            //标准布局情况, 内容占满全屏
            isEmojiShow = false;
            contentLayoutHeight = maxHeight;
            emojiLayoutHeight = getKeyboardHeight();
        }

        contentLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(contentLayoutHeight, MeasureSpec.EXACTLY));
        if (emojiLayout != null) {
            emojiLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(emojiLayoutHeight /*emojiHeight*/, MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean oldNeedHideSoftInput = this.needHideSoftInput;
        this.needHideSoftInput = false;

        if (!enableSoftInput ||
                isViewHide ||
                contentLayout == null ||
                !isEnabled()) {
            super.onLayout(changed, l, t, r, b);
            return;
        }

        if (isInEditMode()) {
            contentLayout.layout(l, t, r, b);
            if (emojiLayout != null) {
                emojiLayout.layout(l, b, r, b);
            }
            return;
        }

        int paddingTop = getPaddingTop();
        t += paddingTop;

        int contentLayoutHeight = contentLayout.getMeasuredHeight();
        int contentBottom = contentLayoutHeight + paddingTop;
        contentLayout.layout(l, t, r, contentBottom);

        if (isSoftKeyboardShow()) {
            if (oldNeedHideSoftInput) {
            } else {
                int viewHeight = getMeasuredHeight();
                if (contentLayoutHeight == viewHeight || contentLayoutHeight == viewHeight - getSoftKeyboardHeight()) {
                    needShowEmojiLayout = false;
                    showEmojiHeight = 0;
                    contentBottom = viewHeight;
                }
            }
        }
        if (emojiLayout != null) {
            emojiLayout.layout(l, contentBottom, r, contentBottom + emojiLayout.getMeasuredHeight());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //L.e("call: onSizeChanged([w, h, oldw, oldh])-> " + h + " oldh:" + oldh);
        removeCallbacks(mCheckSizeChanged);

        if (!isEnabled() || !enableSoftInput || isViewHide) {
            return;
        }

        if (w == oldw && h == oldh) {
            return;
        }

        isKeyboardShow = isSoftKeyboardShow();
        if (isKeyboardShow) {
            isEmojiShow = false;
        }
        checkEmojiLayout();
        if (emojiLayout != null && !isInEditMode()) {
            L.w("onSizeChanged: emojiLayout:" + this.hashCode() + " -> top:" + emojiLayout.getTop() +
                    " height:" + emojiLayout.getMeasuredHeight() +
                    " viewHeight:" + getMeasuredHeight() +
                    " contentHeight:" + contentLayout.getMeasuredHeight());
        }
        if (!isInEditMode()) {
            notifyEmojiLayoutChangeListener(isEmojiShow, isKeyboardShow,
                    isKeyboardShow ? getSoftKeyboardHeight() : showEmojiHeight);
        }
    }

    private void checkEmojiLayout() {
        if (emojiLayout != null) {
            if (emojiLayout.getTop() != 0 &&
                    emojiLayout.getTop() < getMeasuredHeight() &&
                    emojiLayout.getMeasuredHeight() != 0) {
                isEmojiShow = true;
            } else {
                isEmojiShow = false;
            }
        }
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        super.setClipToPadding(clipToPadding);
        mClipToPadding = clipToPadding;
    }

    @Override
    public int getPaddingTop() {
        if (!mClipToPadding) {
            return 0;
        }
        return super.getPaddingTop();
    }

    /**
     * 当在5.0+ 手机上, 并且设置了 状态栏透明, 则onSizeChange方法不会执行,
     * 因为此时系统设置了PaddingBottom来腾出键盘的位置, 而不是通过改变高度.
     * 这个时候需要通过下面的方法处理
     */
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        boolean result = false;
        if (getFitsSystemWindows()) {
            result = super.fitSystemWindows(insets);
            checkOnSizeChanged();
        } else {
            insets.set(0, 0, 0, 0);
        }
        return result;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows()) {
            return super.onApplyWindowInsets(insets);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return insets.replaceSystemWindowInsets(0, 0, 0, 0);
            } else {
                return super.onApplyWindowInsets(insets);
            }
        }
    }

    /**
     * 判断键盘是否显示
     */
    public boolean isSoftKeyboardShow() {
        int screenHeight = getScreenHeightPixels();
        int keyboardHeight = getSoftKeyboardHeight();
        return screenHeight != keyboardHeight && keyboardHeight > 50 * ScreenUtil.density;
    }

    /**
     * 获取键盘的高度
     */
    public int getSoftKeyboardHeight() {
        return getSoftKeyboardHeight(this);
    }

    /**
     * 屏幕高度(不包含虚拟导航键盘的高度)
     */
    private int getScreenHeightPixels() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private void showEmojiLayoutInner(int height, boolean isInAnim) {
        if (/*height > 0 && */isEmojiShow() && height == showEmojiHeight) {
            return;
        }

        boolean keyboardShow = isKeyboardShow();
        //动画高度的开始值
        int oldHeight = showEmojiHeight;

        needShowEmojiLayout = height > 0;

        if (isInAnim) {
            animShowEmojiHeight = height;
        } else {
            this.showEmojiHeight = height;
        }

        if (keyboardShow) {
            needHideSoftInput = true;
            hideSoftInput(this);
        } else {
            requestLayout();
            if (isAnimToShow) {
                animToShow(height, oldHeight);
            } else {
                checkOnSizeChanged();
            }
        }
    }

    private void checkOnSizeChanged() {
        removeCallbacks(mCheckSizeChanged);
        post(mCheckSizeChanged);
    }

    private void animToShow(int height, int oldHeight) {
        if (mValueAnimator != null) {
            return;
        }

        mValueAnimator = ObjectAnimator.ofInt(oldHeight, height);
        mValueAnimator.setDuration(300);
        mValueAnimator.setInterpolator(new DecelerateInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                showEmojiLayoutInner((Integer) animation.getAnimatedValue(), true);
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mValueAnimator = null;
                animShowEmojiHeight = -1;
                checkOnSizeChanged();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mValueAnimator.start();
    }

    /**
     * 显示表情布局
     *
     * @param height 指定表情需要显示的高度
     */
    public void showEmojiLayout(final int height) {
        showEmojiLayoutInner(height, false);
    }

    /**
     * 采用默认的键盘高度显示表情, 如果键盘从未弹出过, 则使用一个缺省的高度
     */
    public void showEmojiLayout() {
        if (keyboardHeight <= 0) {
            keyboardHeight = getDefaultKeyboardHeight();
        }
        showEmojiLayoutInner(keyboardHeight, false);
    }

    private int getDefaultKeyboardHeight() {
        // 设置默认高度 275
        return (int) (getResources().getDisplayMetrics().density * 275);
    }

    /**
     * 采用默认的键盘高度显示表情, 如果键盘从未弹出过, 则使用一个缺省的高度
     */
    public void hideEmojiLayout() {
        if (isSoftKeyboardShow() || isEmojiShow()) {
            showEmojiLayoutInner(0, false);
        }
    }

    public void addOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.add(listener);
    }

    public void removeOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.remove(listener);
    }

    private void notifyEmojiLayoutChangeListener(boolean isEmojiShow, boolean isKeyboardShow, int height) {
        L.w(hashCode() + " 表情:" + isEmojiShow + " 键盘:" + isKeyboardShow + " 高度:" + height);

        if (isKeyboardShow) {
            Hawk.put(KEY_KEYBOARD_HEIGHT, height);
        }

        Iterator<OnEmojiLayoutChangeListener> iterator = mEmojiLayoutChangeListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onEmojiLayoutChange(isEmojiShow, isKeyboardShow, height);
        }
    }

    /**
     * 当键盘显示时, 可以通过此方法返回键盘的高度
     */
    public int getKeyboardHeight() {
        if (keyboardHeight <= 0) {
            keyboardHeight = getDefaultKeyboardHeight();
        }
        return keyboardHeight;
    }

    public void setKeyboardHeight(int keyboardHeight) {
        this.keyboardHeight = keyboardHeight;
    }

    public boolean isEmojiShow() {
        boolean softKeyboardShow = isSoftKeyboardShow();
        if (softKeyboardShow) {
            isEmojiShow = false;
            return false;
        }
        checkEmojiLayout();
        return isEmojiShow;
    }

    public boolean isKeyboardShow() {
        if (needHideSoftInput) {
            isKeyboardShow = false;
        } else {
            isKeyboardShow = isSoftKeyboardShow();
        }
        return isKeyboardShow;
    }

    /**
     * 当表情显示时, 可以通过此方法返回表情的高度
     */
    public int getShowEmojiHeight() {
        return showEmojiHeight;
    }

    /**
     * 返回true时, 可以退出. 否则会隐藏键盘或者表情.
     */
    public boolean requestBackPressed() {
        if (isSoftKeyboardShow() || isEmojiShow()) {
            hideEmojiLayout();
            return false;
        }
        return true;
    }

    @Override
    public void onLifeViewShow() {
        isViewHide = false;
        setFitsSystemWindows();
    }

    public void setFitsSystemWindows() {
        setFitsSystemWindows(isEnabled() && !isViewHide && enableSoftInput);
    }

    @Override
    public void onLifeViewHide() {
        isViewHide = true;
        setFitsSystemWindows(false);
    }

    public interface OnEmojiLayoutChangeListener {
        /**
         * @param height         EmojiLayout弹出的高度 或者 键盘弹出的高度
         * @param isEmojiShow    表情布局是否显示了
         * @param isKeyboardShow 键盘是否显示了
         */
        void onEmojiLayoutChange(boolean isEmojiShow, boolean isKeyboardShow, int height);
    }
}
