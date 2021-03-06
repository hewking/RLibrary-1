package com.angcyo.uiview.view;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.R;
import com.angcyo.uiview.RApplication;
import com.angcyo.uiview.base.UIBaseView;
import com.angcyo.uiview.base.UIIDialogImpl;
import com.angcyo.uiview.base.UILayoutActivity;
import com.angcyo.uiview.container.ILayout;
import com.angcyo.uiview.container.UILayoutImpl;
import com.angcyo.uiview.container.UIParam;
import com.angcyo.uiview.dynamicload.ProxyActivity;
import com.angcyo.uiview.dynamicload.ProxyCompatActivity;
import com.angcyo.uiview.dynamicload.internal.DLPluginPackage;
import com.angcyo.uiview.kotlin.ExKt;
import com.angcyo.uiview.model.AnimParam;
import com.angcyo.uiview.model.TitleBarPattern;
import com.angcyo.uiview.model.ViewPattern;
import com.angcyo.uiview.recycler.RBaseViewHolder;
import com.angcyo.uiview.recycler.RRecyclerView;
import com.angcyo.uiview.resources.AnimUtil;
import com.angcyo.uiview.skin.ISkin;
import com.angcyo.uiview.skin.SkinHelper;
import com.angcyo.uiview.utils.RUtils;
import com.angcyo.uiview.utils.ThreadExecutor;
import com.angcyo.uiview.viewgroup.TouchBackLayout;
import com.angcyo.uiview.viewgroup.TouchLayout;
import com.angcyo.uiview.widget.EmptyView;
import com.angcyo.uiview.widget.RSoftInputLayout;
import com.angcyo.uiview.widget.viewpager.UIViewPager;
import com.tbruyelle.rxpermissions.Permission;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.angcyo.uiview.RCrashHandler.getMemoryInfo;
import static com.angcyo.uiview.view.IViewAnimationType.SCALE_TO_MAX_AND_END_OVERSHOOT;
import static com.angcyo.uiview.view.IViewAnimationType.SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT;
import static com.angcyo.uiview.view.IViewAnimationType.SCALE_TO_MAX_OVERSHOOT;

/**
 * 接口的实现, 仅处理了一些动画, 其他实现都为空
 * 对对话框做了区分处理
 * <p>
 * Created by angcyo on 2016-11-12.
 */

public abstract class UIIViewImpl implements IView {

    public static final int DEFAULT_ANIM_TIME = 200;
    public static final int DEFAULT_DELAY_ANIM_TIME = 260;
    public static final int DEFAULT_FINISH_ANIM_TIME = 200;//完成动画,尽量比启动动画快一点(相差最好是一帧的时间)
    public static final int DEFAULT_DIALOG_FINISH_ANIM_TIME = 150;
    public static int DEFAULT_CLICK_DELAY_TIME = RClickListener.Companion.getDEFAULT_DELAY_CLICK_TIME();
    public Activity mActivity;
    public Runnable onIViewLoad, onIViewUnload;
    protected ILayout mILayout;
    protected ILayout mParentILayout;//上层ILayout, 用来管理上层IView的生命周期, 如果有值, 会等于mILayout
    protected ILayout mChildILayout;//
    /**
     * 根布局
     */
    protected View mRootView;
    /**
     * 用来管理rootView, onViewCreate 中创建了
     */
    protected RBaseViewHolder mViewHolder;
    protected RBaseViewHolder $;
    protected IViewShowState mIViewStatus = IViewShowState.STATE_NORMAL;
    /**
     * 最后一次显示的时间
     */
    protected long mLastShowTime = 0;
    protected List<ILifecycle> mILifecycleList = new ArrayList<>();
    /**
     * 当{@link #onViewShow(Bundle)}被调用一次, 计数器就会累加
     */
    protected long viewShowCount = 0;
    protected long showInPagerCount = 0;
    protected boolean isShowInViewPager = false;
    protected int mBaseSoftInputMode;
    protected OnUIViewListener mOnUIViewListener;
    /**
     * 启动IView的动画类型
     */
    protected IViewAnimationType mAnimationType = IViewAnimationType.TRANSLATE_HORIZONTAL;
    protected DLPluginPackage mPluginPackage;
    protected RxPermissions mRxPermissions;
    OnCountDown mOnCountDown;
    int maxCount;
    int currentCount;
    //提供倒计时的功能
    protected Runnable countDownRunnable = new Runnable() {
        @Override
        public void run() {
            currentCount--;
            if (mOnCountDown != null) {
                mOnCountDown.onCountDown(currentCount);
                if (currentCount == 0) {
                } else {
                    postDelayed(this, 1000);
                }
            }
        }
    };
    private boolean mIsRightJumpLeft = false;
    private boolean interruptTask = false;

    //当界面首次显示时, 多少毫秒后, 自动关闭
    private long delayFinish = -1;
    private Runnable delayFinishRunnable;

    public static void setDefaultConfig(Animation animation, boolean isFinish) {
        if (isFinish) {
            animation.setDuration(DEFAULT_FINISH_ANIM_TIME);
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setFillAfter(true);
        } else {
            animation.setDuration(DEFAULT_ANIM_TIME);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setFillAfter(false);
        }
        animation.setFillBefore(true);
    }

    /**
     * 判断设备是否是低端
     */
    public static boolean isLowDevice() {
        boolean isLowMen = true;
        try {
            final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo(RApplication.getApp());
            isLowMen = memoryInfo.totalMem < 1000 * 1000 * 1000 * 3L;//小于2G的内存
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!isLollipop()) {
            return true;
        }
        return isLowMen;
    }

    public static boolean isLowLowDevice() {
        boolean isLowMen = true;
        try {
            final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo(RApplication.getApp());
            isLowMen = memoryInfo.totalMem < 1000 * 1000 * 1000 * 1.5f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!isLollipop()) {
            return true;
        }
        return isLowMen;
    }

    public static boolean isHighDevice() {
        boolean isHighMen = true;
        try {
            final ActivityManager.MemoryInfo memoryInfo = getMemoryInfo(RApplication.getApp());
            isHighMen = memoryInfo.totalMem >= 1000 * 1000 * 1000 * 3.8f;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isHighMen;
    }

    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public TitleBarPattern loadTitleBar(Context context) {
        L.d(this.getClass().getSimpleName(), "loadTitleBar: ");
        return null;
    }

    @Override
    public void onAttachedToILayout(ILayout iLayout) {
        mILayout = iLayout;
        setChildILayout(iLayout);
        if (mParentILayout == null) {
            mParentILayout = iLayout;
        }
    }

    @Override
    public View inflateContentView(Activity activity, ILayout iLayout, FrameLayout container, LayoutInflater inflater) {
        L.d(this.getClass().getSimpleName(), "inflateContentView: ");
        mActivity = activity;
        View baseView;
        try {
            baseView = inflateBaseView(container, inflater);
        } catch (Exception e) {
            e.printStackTrace();
            UILayoutImpl.saveToSDCard(e.toString());

            baseView = new EmptyView(activity);
            ((EmptyView) baseView).setDefaultColor(SkinHelper.getSkin().getThemeSubColor());
        }
        if (enableTouchBack() && baseView instanceof TouchBackLayout) {
            initTouchBackLayout((TouchBackLayout) baseView);
        }
        return baseView;
    }

    protected abstract View inflateBaseView(@NonNull FrameLayout container /*此View 就是UILayoutImpl*/, @NonNull LayoutInflater inflater);

    @CallSuper
    @Override
    public void loadContentView(View rootView) {
        L.d(this.getClass().getSimpleName(), "loadContentView: ");
        //不使用 butterknife
//        try {
//            ButterKnife.bind(this, rootView);
//        } catch (Exception e) {
//            String message = e.getMessage();
//        }
    }

    /**
     * 开始布局动画
     */
    public void startLayoutAnim(View parent) {
        final Animation layoutAnimation = loadLayoutAnimation(new AnimParam());
        if (layoutAnimation != null && parent instanceof ViewGroup) {
            AnimUtil.applyLayoutAnimation(findChildViewGroup((ViewGroup) parent), layoutAnimation);
        }
    }

    /**
     * 查找具有多个子View的ViewGroup, 用来播放布局动画
     */
    private ViewGroup findChildViewGroup(ViewGroup parent) {
        final int childCount = parent.getChildCount();
        if (childCount == 1) {
            View childAt = parent.getChildAt(0);
            if (childAt instanceof ViewGroup) {
                return findChildViewGroup((ViewGroup) childAt);
            } else {
                return parent;
            }
        } else {
            return parent;
        }
    }

    @CallSuper
    @Override
    @Deprecated
    public void onViewCreate(View rootView) {
        L.d(this.getClass().getSimpleName(), "onViewCreate: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_CREATE;
        mRootView = rootView;
        mViewHolder = new RBaseViewHolder(mRootView);
        $ = mViewHolder;
    }

    @Override
    public void onViewCreate(View rootView, UIParam param) {
        L.d(this.getClass().getSimpleName(), "onViewCreate 2: " + mIViewStatus);
        mBaseSoftInputMode = mActivity.getWindow().getAttributes().softInputMode;

        requestedDefaultOrientation();
    }

    @CallSuper
    @Override
    public void onViewLoad() {
        L.d(this.getClass().getSimpleName(), "onViewLoad: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_LOAD;

        if (onIViewLoad != null) {
            onIViewLoad.run();
        }

        if (mOnUIViewListener != null) {
            mOnUIViewListener.onViewLoad(this);
        }
    }

    @Deprecated
    @Override
    @CallSuper
    public void onViewShow() {
        onViewShow(null);
    }

    @Override
    public void onViewShow(@Nullable Bundle bundle, @Nullable Class<?> fromClz) {
        String className = "null";
        if (fromClz != null) {
            className = fromClz.getSimpleName();
        }
        L.d(this.getClass().getSimpleName(), "onViewShow: " + mIViewStatus + " from:" + className);
        onViewShow(bundle);
    }

    @CallSuper
    @Override
    public void onViewShow(@Nullable Bundle bundle) {
        //L.d(this.getClass().getSimpleName(), "onViewShow: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_SHOW;
        long lastShowTime = mLastShowTime;
        viewShowCount++;
        mLastShowTime = System.currentTimeMillis();

        requestedDefaultOrientation();

        notifyLifeViewShow();

        if (lastShowTime == 0) {
            if (delayFinish > 0) {
                if (delayFinishRunnable == null) {
                    delayFinishRunnable = new Runnable() {
                        @Override
                        public void run() {
                            finishIView();
                        }
                    };
                }
                postDelayed(delayFinish, delayFinishRunnable);
            }
            onViewShowFirst(bundle);
        } else {
            onViewShowNotFirst(bundle);
        }

        onViewShow(viewShowCount);
        if (!isChildILayoutEmpty()) {
            mChildILayout.onLastViewShow(bundle);
        }

        if (mOnUIViewListener != null) {
            mOnUIViewListener.onViewShow(this, viewShowCount);
        }
    }

    public void notifyLifeViewShow() {
        for (ILifecycle life : mILifecycleList) {
            life.onLifeViewShow();
        }
    }

    public void onViewShowFirst(@Nullable Bundle bundle) {
        L.v(this.getClass().getSimpleName(), "onViewShowFirst: ");
    }

    public void onViewShowNotFirst(@Nullable Bundle bundle) {
        L.v(this.getClass().getSimpleName(), "onViewShowNotFirst: ");
    }

    //星期五 2017-2-17
    public void onViewShow(long viewShowCount /*从1开始的调用次数*/) {
        L.v(this.getClass().getSimpleName(), "onViewShowCount " + viewShowCount);
    }

    @CallSuper
    @Override
    public void onViewReShow(@Nullable Bundle bundle) {
        L.d(this.getClass().getSimpleName(), "onViewReShow: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_SHOW;

        if (!isChildILayoutEmpty()) {
            mChildILayout.onLastViewReShow(bundle);
        }
    }

    @Override
    public void onViewHideFromDialog() {
        L.d(this.getClass().getSimpleName(), "onViewHideFromDialog: " + mIViewStatus);
    }

    @CallSuper
    @Override
    public void onViewHide() {
        L.d(this.getClass().getSimpleName(), "onViewHide: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_HIDE;

        notifyLifeViewHide();

        if (!isChildILayoutEmpty()) {
            mChildILayout.onLastViewHide();
        }
    }

    public void notifyLifeViewHide() {
        for (ILifecycle life : mILifecycleList) {
            life.onLifeViewHide();
        }
    }

    @Deprecated
    @CallSuper
    @Override
    public void onViewUnload() {
        L.d(this.getClass().getSimpleName(), "onViewUnload: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_UNLOAD;
        if (mOnUIViewListener != null) {
            mOnUIViewListener.onViewUnload(this);
        }
        setChildILayout(null);
    }

    @Deprecated
    @CallSuper
    @Override
    public void onViewUnloadDelay() {
        L.d(this.getClass().getSimpleName(), "onViewUnload: " + mIViewStatus);
        mIViewStatus = IViewShowState.STATE_VIEW_UNLOAD;
        if (mOnUIViewListener != null) {
            mOnUIViewListener.onViewUnloadDelay(this);
        }
        setChildILayout(null);
    }

    @Override
    public void onViewUnload(UIParam uiParam) {
        removeCallbacks(delayFinishRunnable);
        if (onIViewUnload != null) {
            onIViewUnload.run();
        }
    }

    @Override
    public void onViewUnloadDelay(UIParam uiParam) {

    }

    @CallSuper
    @Override
    public void release() {
        mOnUIViewListener = null;
        //mActivity = null;
        mChildILayout = null;
        //mParentILayout = null;
        //mILayout = null;
        mILifecycleList.clear();
    }

    @Override
    public Animation loadStartAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadStartAnimation: " + mIViewStatus);
        switch (mAnimationType) {
            case NONE:
                return null;
            case ALPHA:
                return AnimUtil.createAlphaEnterAnim(0.8f);
            case TRANSLATE_VERTICAL:
                return AnimUtil.translateStartAnimation();
            case SCALE_TO_MAX:
            case SCALE_TO_MAX_AND_END:
            case SCALE_TO_MAX_OVERSHOOT:
            case SCALE_TO_MAX_AND_END_OVERSHOOT:
            case SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT:
                Animation animation = AnimUtil.scaleMaxAlphaStartAnimation(0.7f);
                if (mAnimationType == SCALE_TO_MAX_OVERSHOOT ||
                        mAnimationType == SCALE_TO_MAX_AND_END_OVERSHOOT ||
                        mAnimationType == SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT) {
                    animation.setInterpolator(new OvershootInterpolator());
                }
                return animation;
            case TRANSLATE_HORIZONTAL:
            default:
                return defaultLoadStartAnimation(animParam);
        }
    }

    @Override
    public Animation loadFinishAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadFinishAnimation: ");
        switch (mAnimationType) {
            case NONE:
                return null;
            case ALPHA:
                return AnimUtil.createAlphaExitAnim(0.2f);
            case TRANSLATE_VERTICAL:
                return AnimUtil.translateFinishAnimation();
            case SCALE_TO_MAX_AND_END:
            case SCALE_TO_MAX_AND_END_OVERSHOOT:
                Animation animation = AnimUtil.scaleMaxAlphaFinishAnimation(0.7f);
                if (mAnimationType == SCALE_TO_MAX_AND_END_OVERSHOOT) {
                    animation.setInterpolator(new OvershootInterpolator());
                }
                return animation;
            case SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT:
                Animation animation2 = AnimUtil.scaleMaxAlphaFinishAnimation(1.2f);
                animation2.setInterpolator(new AnticipateInterpolator());
                return animation2;
            case SCALE_TO_MAX:
            case SCALE_TO_MAX_OVERSHOOT:
            case TRANSLATE_HORIZONTAL:
            default:
                return defaultLoadFinishAnimation(animParam);
        }
    }

    @Override
    public Animation loadOtherEnterAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadOtherEnterAnimation: ");
        switch (mAnimationType) {
            case NONE:
                return null;
            case ALPHA:
                return AnimUtil.createAlphaEnterAnim(0.8f);
            case TRANSLATE_VERTICAL:
                return AnimUtil.createAlphaEnterAnim(0.8f);
            case SCALE_TO_MAX_AND_END:
            case SCALE_TO_MAX_AND_END_OVERSHOOT:
            case SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT:
                if (isDialog()) {
                    return AnimUtil.scaleMaxAlphaStartAnimation(0.7f);
                } else {
                    return AnimUtil.createOtherEnterNoAnim();
                }
            case SCALE_TO_MAX:
            case SCALE_TO_MAX_OVERSHOOT:
            case TRANSLATE_HORIZONTAL:
            default:
                return defaultLoadOtherEnterAnimation(animParam);
        }
    }

    @Override
    public Animation loadOtherExitAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadOtherExitAnimation: ");
        switch (mAnimationType) {
            case NONE:
                return null;
            case ALPHA:
                return AnimUtil.createAlphaExitAnim(0.8f);
            case TRANSLATE_VERTICAL:
                return AnimUtil.createAlphaExitAnim(0.8f);
            case SCALE_TO_MAX_AND_END:
            case SCALE_TO_MAX_AND_END_OVERSHOOT:
            case SCALE_TO_MAX_AND_TO_MAX_END_OVERSHOOT:
            case SCALE_TO_MAX:
            case SCALE_TO_MAX_OVERSHOOT:
                if (isDialog()) {
                    return AnimUtil.scaleMaxAlphaFinishAnimation(0.5f);
                } else {
                    return AnimUtil.createOtherExitNoAnim();
//                    return AnimUtil.scaleMaxAlphaFinishAnimation(0.9f);
                }
            case TRANSLATE_HORIZONTAL:
            default:
                return defaultLoadOtherExitAnimation(animParam);
        }
    }


    protected Animation defaultLoadStartAnimation(@NonNull AnimParam animParam) {
        TranslateAnimation translateAnimation;
        if (mIsRightJumpLeft) {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -0.99f, Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        } else {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.99f, Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        }
        setDefaultConfig(translateAnimation, false);
        return translateAnimation;
    }


    protected Animation defaultLoadFinishAnimation(@NonNull AnimParam animParam) {
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        setDefaultConfig(translateAnimation, true);
        return translateAnimation;
    }

    @Override
    public Animation loadShowAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadShowAnimation: ");
        return loadStartAnimation(animParam);
    }

    @Override
    public Animation loadHideAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadHideAnimation: ");
        return loadFinishAnimation(animParam);
    }

    public Animation defaultLoadOtherExitAnimation(@NonNull AnimParam animParam) {
        if (animParam.targetIView.isDialog()) {
            return UIIDialogImpl.dialogDefaultLoadFinishAnimation();
        }

        TranslateAnimation translateAnimation;
        if (mIsRightJumpLeft) {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1f,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        } else {
            translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1f,
                    Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        }
        setDefaultConfig(translateAnimation, true);
        return translateAnimation;
    }

    public Animation defaultLoadOtherEnterAnimation(@NonNull AnimParam animParam) {
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, -1f, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        setDefaultConfig(translateAnimation, false);
        return translateAnimation;
    }

    @Override
    public Animation loadOtherHideAnimation(@NonNull AnimParam animParam) {
        return loadOtherExitAnimation(animParam);
    }

    @Override
    public Animation loadOtherShowAnimation(@NonNull AnimParam animParam) {
        return loadOtherEnterAnimation(animParam);
    }

    @Override
    public Animation loadLayoutAnimation(@NonNull AnimParam animParam) {
        L.v(this.getClass().getSimpleName(), "loadLayoutAnimation: ");
//        if (mIsRightJumpLeft) {
//
//        } else {
//
//        }
//
//        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, -1f,
//                Animation.RELATIVE_TO_PARENT, 0f,
//                Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_PARENT, 0f);
//        setDefaultConfig(translateAnimation);
//        return translateAnimation;
        return null;
    }

    @Override
    public boolean isDialog() {
        return false;
    }

    @Override
    public boolean isDimBehind() {
        return true;
    }

    @Override
    public boolean canCanceledOnOutside() {
        return true;
    }

    @Override
    public boolean canTouchOnOutside() {
        return true;
    }

    @Override
    public boolean canCancel() {
        return true;
    }

    @Override
    public View getView() {
        return mRootView;
    }

    @Override
    public View getDialogDimView() {
        //请在对话框中实现
        return null;
    }

    /**
     * 在inflateContentView之前调用, 返回的都是null
     */
    @Override
    public ILayout getILayout() {
        return mILayout;
    }

    /**
     * 此方法只在UIVIewPager中会调用, 当前IView显示时
     */
    @Override
    public void onShowInPager(@NonNull UIViewPager viewPager) {
        showInPagerCount++;
        L.i(this.getClass().getSimpleName(), "onShowInPager: " + showInPagerCount);
        notifyLifeViewShow();
    }

    /**
     * 此方法只在UIVIewPager中会调, 当前IView隐藏时
     */
    @Override
    public void onHideInPager(@NonNull UIViewPager viewPager) {
        L.i(this.getClass().getSimpleName(), "onHideInPager: ");
        notifyLifeViewHide();
    }

    public void startIView(final IView iView) {
        startIView(iView, true);
    }

    public void startIView(final IView iView, boolean anim) {
        startIView(iView, createUIParam().setAnim(anim));
    }

    public void startIView(final IView iView, final UIParam param) {
        if (iView == null) {
            return;
        }
        iView.setInterruptTask(false);
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        if (isInPlugin()) {
            iView.setPluginPackage(mPluginPackage);
        }
        mParentILayout.startIView(iView, param);
    }

    public void finishIView() {
        finishIView(this);
    }

    public void finishIView(final IView iView) {
        finishIView(iView, true);
    }

    public void finishIView(final IView iView, final UIParam param) {
        if (iView == null) {
            return;
        }
        iView.setInterruptTask(true);
        if (mParentILayout == null) {
            //throw new IllegalArgumentException("mParentILayout 还未初始化");
        } else {
            mParentILayout.finishIView(iView, param);
        }
    }

    public void finishIView(final UIParam param) {
        finishIView(this, param);
    }

    public void finishIView(final Runnable unloadRunnable) {
        finishIView(createUIParam().setUnloadRunnable(unloadRunnable));
    }

    public void finishIView(boolean closeLastDialog, final Runnable unloadRunnable) {
        finishIView(createUIParam().setUnloadRunnable(unloadRunnable).setCloseLastDialog(closeLastDialog));
    }

    public void finishIView(final IView iView, boolean anim) {
        finishIView(iView, anim, false);
    }

    public void finishIView(final IView iView, boolean anim, boolean quiet) {
        finishIView(iView, createUIParam().setAnim(anim).setQuiet(quiet));
    }

    protected UIParam createUIParam() {
        return UIParam.get();
    }

    public void showIView(final View view) {
        showIView(view, true);
    }

    public void showIView(final View view, final boolean needAnim) {
        showIView(view, needAnim, null);
    }

    public void showIView(final View view, final boolean needAnim, final Bundle bundle) {
        if (view == null) {
            return;
        }
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        mParentILayout.showIView(view, createUIParam().setAnim(needAnim).setBundle(bundle));
    }

    public void showIView(IView iview, boolean needAnim) {
        showIView(iview, needAnim, null);
    }

    public void showIView(IView iview) {
        showIView(iview, true);
    }

    public void showIView(IView iview, boolean needAnim, Bundle bundle) {
        if (iview == null) {
            return;
        }
        iview.setInterruptTask(false);
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        if (isInPlugin()) {
            iview.setPluginPackage(mPluginPackage);
        }
        mParentILayout.showIView(iview, createUIParam().setAnim(needAnim).setBundle(bundle));
    }

    public void hideIView(final View view) {
        hideIView(view, true);
    }

    public void hideIView(final View view, final boolean needAnim) {
        hideIView(view, needAnim, null);
    }

    public void hideIView(final View view, final boolean needAnim, final Bundle bundle) {
        if (view == null) {
            return;
        }
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        mParentILayout.hideIView(view, createUIParam().setAnim(needAnim).setBundle(bundle));
    }

    public void hideIView(IView iview, boolean needAnim) {
        hideIView(iview, needAnim, null);
    }

    public void hideIView(IView iview) {
        hideIView(iview, true);
    }

    public void hideIView(IView iview, boolean needAnim, Bundle bundle) {
        if (iview == null) {
            return;
        }
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        mParentILayout.hideIView(iview, createUIParam().setAnim(needAnim).setBundle(bundle));
    }

    public void replaceIView(IView iView, boolean needAnim) {
        replaceIView(iView, createUIParam().setAnim(needAnim));
    }

    public void replaceIView(IView iView) {
        replaceIView(iView, true);
    }

    public void replaceIView(IView iView, UIParam param) {
        if (iView == null) {
            return;
        }
        iView.setInterruptTask(false);
        if (mParentILayout == null) {
            throw new IllegalArgumentException("mParentILayout 还未初始化");
        }
        if (isInPlugin()) {
            iView.setPluginPackage(mPluginPackage);
        }
        param.setReplaceIView(this);
        mParentILayout.replaceIView(iView, param);
    }

    public void post(Runnable action) {
        if (mILayout instanceof View) {
            ((View) mILayout).post(action);

            //RUtils.saveToSDCard("HnUIMain", "mILayout post." + ((View) mILayout).getParent());
            return;
        }

        if (mRootView != null) {
            mRootView.post(action);

            //RUtils.saveToSDCard("HnUIMain", "mRootView post." + mRootView.getParent());
        } else {
            //RUtils.saveToSDCard("HnUIMain", "mRootView post.jump");
        }
    }

    public void postDelayed(Runnable action, long delayMillis) {
        if (mILayout instanceof View) {
            ((View) mILayout).postDelayed(action, delayMillis);

            //RUtils.saveToSDCard("HnUIMain", "mILayout postDelayed." + ((View) mILayout).getParent());
            return;
        }

        if (mRootView != null) {
            mRootView.postDelayed(action, delayMillis);

            //RUtils.saveToSDCard("HnUIMain", "mRootView postDelayed." + mRootView.getParent());
        } else {
            //RUtils.saveToSDCard("HnUIMain", "mRootView postDelayed.jump");
        }
    }

    public void postDelayed(long delayMillis, Runnable action) {
        if (mILayout instanceof View) {
            ((View) mILayout).postDelayed(action, delayMillis);
            return;
        }

        if (mRootView != null) {
            mRootView.postDelayed(action, delayMillis);
        }
    }

    public void removeCallbacks(Runnable action) {
        if (mILayout instanceof View) {
            ((View) mILayout).removeCallbacks(action);
            return;
        }

        if (mRootView != null) {
            mRootView.removeCallbacks(action);
        }
    }

    /**
     * @return true 允许退出
     */
    @Override
    public boolean onBackPressed() {
        return true;
    }

    @Override
    public boolean canSwipeBackPressed() {
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public IView setIsRightJumpLeft(boolean isRightJumpLeft) {
        mIsRightJumpLeft = isRightJumpLeft;
        return this;
    }

    @Override
    public int getDimColor() {
        return Color.parseColor("#60000000");
    }

    @Override
    public View getAnimView() {
        return mRootView;
    }

    @Override
    public IView bindParentILayout(ILayout otherILayout) {
        mParentILayout = otherILayout;
        return this;
    }

    @Override
    public boolean haveParentILayout() {
        return mParentILayout != mILayout;
    }

    public ILayout getParentILayout() {
        return mParentILayout;
    }

    @Override
    public boolean haveChildILayout() {
        if (isChildILayoutEmpty()) {
            return false;
        }
        return mChildILayout != mILayout;
    }

    @Override
    public boolean canTryCaptureView() {
        return true;
    }

    public Resources getResources() {
        if (mActivity == null) {
            return RApplication.getApp().getResources();
        }
        return mActivity.getResources();
    }

    //星期二 2017-2-28
    public String getString(@StringRes int id) {
        return getResources().getString(id);
    }

    //星期二 2017-2-28
    public String getString(@StringRes int id, Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }

    /**
     * 是否全屏
     *
     * @param enable 是
     */
    //星期三 2017-3-1
    public void fullscreen(boolean enable) {
        fullscreen(enable, true);
    }

    /**
     * 是否是白色的标题栏, 如果是, 那么系统的状态栏字体会是灰色
     */
    public void lightStatusBar(boolean light) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int systemUiVisibility = mActivity.getWindow().getDecorView().getSystemUiVisibility();
            if (light) {
                if (ExKt.have(systemUiVisibility, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)) {
                    return;
                }
                mActivity.getWindow()
                        .getDecorView()
                        .setSystemUiVisibility(
                                systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                if (!ExKt.have(systemUiVisibility, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)) {
                    return;
                }
                mActivity.getWindow()
                        .getDecorView()
                        .setSystemUiVisibility(
                                systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        } else {
            if (mParentILayout instanceof UILayoutImpl) {
                if (isFullScreen()) {
                    ((UILayoutImpl) mParentILayout).setDimStatusBar(false);
                } else {
                    ((UILayoutImpl) mParentILayout).setDimStatusBar(light);
                }
            }
        }
    }

    /**
     * @param checkSdk true 表示只在高版本的SDK上使用.
     */
    public void fullscreen(final boolean enable, boolean checkSdk) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final View decorView = mActivity.getWindow().getDecorView();
                if (enable) {
                    decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_FULLSCREEN);
                } else {
                    decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        };

        if (checkSdk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                runnable.run();
            }
        } else {
            runnable.run();
        }
    }

    //星期一 2017-3-13
    @ColorInt
    public int getColor(@ColorRes int id) {
        return ContextCompat.getColor(mActivity, id);
    }

    public int getInteger(@IntegerRes int id) {
        return getResources().getInteger(id);
    }

    //2017-06-10
    public ColorStateList getColorList(@ColorRes int id) {
        return ContextCompat.getColorStateList(mActivity, id);
    }

    public Drawable getDrawable(@DrawableRes int id) {
        return ContextCompat.getDrawable(mActivity, id);//.mutate();
    }

    public int getDimensionPixelOffset(@DimenRes int id) {
        return getResources().getDimensionPixelOffset(id);
    }

    public float density() {
        return getResources().getDisplayMetrics().density;
    }

    public float scaledDensity() {
        return getResources().getDisplayMetrics().scaledDensity;
    }

    public int widthPixels() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    public int heightPixels() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 冻结界面, 拦截所有Touch事件
     */
    public void setLayoutFrozen(boolean frozen) {
        if (mRootView != null) {
            mRootView.setEnabled(frozen);
        }
    }

    @Override
    public boolean showOnDialog() {
        return false;
    }

    @Override
    public boolean canDoubleCancel() {
        return false;
    }

    public void updateSkin() {
        onSkinChanged(SkinHelper.getSkin());
    }

    @Override
    public void onSkinChanged(ISkin skin) {
        L.v(this.getClass().getSimpleName(), "onSkinChanged: " + skin.skinName());
        notifySkinChanged(mRootView, skin);

//        if (mChildILayout != null && mChildILayout != mParentILayout) {
//            mChildILayout.onSkinChanged(skin);
//        }
    }

    /**
     * 界面状态 (加载, 显示, 隐藏, 卸载)
     */
    @Override
    public IViewShowState getIViewShowState() {
        return mIViewStatus;
    }

    /**
     * 界面是否处于显示状态
     */
    public boolean isIViewShow() {
        return getIViewShowState() == IViewShowState.STATE_VIEW_SHOW;
    }

    /**
     * 界面显示过
     */
    public boolean isIViewShowOver() {
        return getIViewShowState() == IViewShowState.STATE_VIEW_SHOW ||
                getIViewShowState() == IViewShowState.STATE_VIEW_HIDE ||
                getIViewShowState() == IViewShowState.STATE_VIEW_UNLOAD;
    }

    public boolean isIViewUnload() {
        return getIViewShowState() == IViewShowState.STATE_NORMAL ||
                getIViewShowState() == IViewShowState.STATE_VIEW_UNLOAD;
    }

    public <T extends View> T v(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.v(id);
    }

    public TextView tv(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.tv(id);
    }

    public RRecyclerView rv(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.rv(id);
    }

    public View view(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.v(id);
    }

    public ViewGroup vg(@IdRes int id) {
        if (mViewHolder == null) {
            return null;
        }
        return mViewHolder.vg(id);
    }

    public RBaseViewHolder getViewHolder() {
        return mViewHolder;
    }

    public void click(@IdRes int id, final View.OnClickListener listener) {
        click(v(id), listener);
    }

    public void click(View view, final View.OnClickListener listener) {
        click(view, DEFAULT_CLICK_DELAY_TIME, listener);
    }

    public void click(View view, int delayTime, final View.OnClickListener listener) {
        if (listener instanceof RClickListener) {
            view.setOnClickListener(listener);
        } else {
            view.setOnClickListener(new RClickListener(delayTime) {
                @Override
                public void onRClick(View view) {
                    if (listener != null) {
                        listener.onClick(view);
                    }
                }
            });
        }
    }

    public void setChildILayout(ILayout childILayout) {
        mChildILayout = childILayout;
        if (mILayout != null) {
            mILayout.setChildILayout(mChildILayout);
        }
        if (mParentILayout != null) {
            mParentILayout.setChildILayout(mChildILayout);
        }
    }

    private boolean isChildILayoutEmpty() {
        return mChildILayout == null || mChildILayout == mILayout;
    }

    protected void notifySkinChanged(View view, ISkin skin) {
        if (view != null) {
            if (view instanceof UILayoutImpl) {
                ((UILayoutImpl) view).onSkinChanged(skin);
            }
            if (view instanceof RecyclerView) {
                RRecyclerView.ensureGlow(((RecyclerView) view), skin.getThemeSubColor());
            }
            if (view instanceof ViewPager) {
                UIViewPager.ensureGlow((ViewPager) view, skin.getThemeSubColor());
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < (viewGroup).getChildCount(); i++) {
                    notifySkinChanged(viewGroup.getChildAt(i), skin);
                }
            }
        }
    }

    public float getTitleBarHeight() {
//        float density = getResources().getDisplayMetrics().density;
        if (isLollipop()) {
            return getDimensionPixelOffset(R.dimen.title_bar_height);
            //return density * 65f;
        } else {
            //return density * 40f;
            return getDimensionPixelOffset(R.dimen.action_bar_height);
        }
    }

    /**
     * 注册IView生命周期的回调
     */
    public void registerLifecycler(ILifecycle lifecycle) {
        if (!mILifecycleList.contains(lifecycle)) {
            mILifecycleList.add(lifecycle);
        }
    }

    /**
     * 保持屏幕常亮
     */
    public void keepScreenOn(boolean keep) {
        if (mActivity != null) {
            Window window = mActivity.getWindow();
            if (keep) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    /**
     * 强制主线程执行
     */
    public void runOnUiThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (RUtils.isMainThread()) {
            runnable.run();
        } else {
            if (mActivity == null) {
                ThreadExecutor.instance().onMain(runnable);
            } else {
                mActivity.runOnUiThread(runnable);
            }
        }
    }

    /**
     * 设置窗口键盘弹出模式 (默认是RESIZE, ADJUST)
     */
    public void adjustPan(boolean adjust) {
        if (adjust) {
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        } else {
            mActivity.getWindow().setSoftInputMode(mBaseSoftInputMode);
        }
    }

    public UIIViewImpl setOnUIViewListener(OnUIViewListener onUIViewListener) {
        mOnUIViewListener = onUIViewListener;
        return this;
    }

    /**
     * 显示软键盘
     *
     * @param editText 尽量是EditText
     */
    public void showSoftInput(final View editText) {
        RSoftInputLayout.showSoftInput(editText);
    }

    /**
     * 隐藏软键盘
     */
    public void hideSoftInput(final View view) {
        RSoftInputLayout.hideSoftInput(view);
    }

    public void requestBackPressed() {
        if (mILayout != null) {
            mILayout.requestBackPressed();
        }
    }

    public void setIViewNeedLayout(boolean layout) {
        if (mILayout instanceof UILayoutImpl) {
            ((UILayoutImpl) mILayout).setIViewNeedLayout(mRootView, layout);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.getClass().getSimpleName();
    }

    @Override
    public boolean needTransitionStartAnim() {
        return false;
    }

    @Override
    public boolean needTransitionExitAnim() {
        return false;
    }

    @Override
    public boolean needForceMeasure() {
        return false;
    }

    @Override
    public boolean needForceVisible() {
        return false;
    }

    @Override
    public boolean hideSoftInputOnTouchDown() {
        return false;
    }

    @Override
    public void onViewShowOnDialogFinish() {
        L.d(this.getClass().getSimpleName(), "onViewShowOnDialogFinish: " + mIViewStatus);
    }

    @Override
    public boolean enableTouchBack() {
        return false;
    }

    @Override
    public int getOffsetScrollTop() {
        return 0;
    }

    /**
     * 获取屏幕方向
     *
     * @see android.content.res.Configuration#ORIENTATION_LANDSCAPE
     * @see android.content.res.Configuration#ORIENTATION_PORTRAIT
     */
    public int getScreenOrientation() {
        return getResources().getConfiguration().orientation;
    }

    /**
     * {@link android.app.Activity#setRequestedOrientation(int)}
     */
    public void requestedOrientation(int orientation) {
        if (orientation != -1) {
            mActivity.setRequestedOrientation(orientation);
        }
    }

    public void requestedDefaultOrientation() {
        requestedOrientation(getDefaultRequestedOrientation());
    }

    /**
     * 请求横屏
     */
    public void requestedLandscape() {
        requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
    }

    /**
     * 请竖屏
     */
    public void requestedPortrait() {
        requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public int getDefaultRequestedOrientation() {
        int orientation;
        if (getScreenOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE; //横屏
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // 竖屏
        }
        return orientation;//ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    public RBaseViewHolder get$() {
        return $;
    }

    /**
     * 初始化下拉返回layout
     */
    protected void initTouchBackLayout(TouchBackLayout layout) {
        if (enableTouchBack()) {
            layout.setEnableTouchBack(true);
            layout.setOffsetScrollTop(getOffsetScrollTop());
            layout.setHandleTouchType(TouchLayout.HANDLE_TOUCH_TYPE_DISPATCH);
            layout.setOnTouchBackListener(new TouchBackLayout.OnTouchBackListener() {
                @Override
                public void onTouchBackListener(TouchBackLayout layout, int oldScrollY, int scrollY, int maxScrollY) {
                    if (oldScrollY != scrollY) {
                        layout.setBackgroundColor(
                                AnimUtil.evaluateColor(scrollY * 1f / maxScrollY,
                                        getColor(R.color.transparent_dark80),
                                        Color.TRANSPARENT));
                    }
                    if (scrollY >= maxScrollY) {
                        finishIView(new UIParam(false, true, false));
                    }
                }
            });
        }
    }

    @Override
    public void onIViewLayout(ViewPattern viewPattern, UIBaseView.LayoutState layoutState, IViewShowState viewShowState, View rootView) {
        //L.i2(160, this.getClass().getSimpleName(), " " + layoutState + " " + viewShowState + " w:" + rootView.getMeasuredWidth() + " h:" + rootView.getMeasuredHeight());
    }

    @Override
    public boolean isLightStatusBar() {
        return false;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public boolean isInterruptTask() {
        return interruptTask;
    }

    @Override
    public void setInterruptTask(boolean interruptTask) {
        this.interruptTask = interruptTask;
    }

    /**
     * 是否请求拦截所有touch事件
     */
    public void interceptTouchEvent(boolean intercept) {
        if (mParentILayout instanceof UILayoutImpl) {
            ((UILayoutImpl) mParentILayout).setInterceptTouchEvent(intercept);
        }
    }

    /**
     * 获取音频焦点
     */
    public void audioFocus(boolean focus) {
        if (focus) {
            RUtils.requestAudioFocus(mActivity);
        } else {
            RUtils.abandonAudioFocus(mActivity);
        }
    }

    /**
     * 开始倒计时
     */
    public void startCountDown(int maxCount, OnCountDown onCountDown) {
        stopCountDown();
        removeCallbacks(countDownRunnable);
        if (maxCount > 0) {
            mOnCountDown = onCountDown;
            currentCount = maxCount + 1;
            post(countDownRunnable);
        }
    }

    public void stopCountDown() {
        removeCallbacks(countDownRunnable);
        mOnCountDown = null;
        maxCount = -1;
    }

    public UIIViewImpl setAnimationType(IViewAnimationType animationType) {
        mAnimationType = animationType;
        return this;
    }

    public boolean isShowInViewPager() {
        return isShowInViewPager;
    }

    public UIIViewImpl setShowInViewPager(boolean showInViewPager) {
        isShowInViewPager = showInViewPager;
        return this;
    }

    @Override
    public void setPluginPackage(DLPluginPackage pluginPackage) {
        mPluginPackage = pluginPackage;
    }

    @Override
    public boolean isInPlugin() {
        return mPluginPackage != null;
    }

    public void injectPluginPackage() {
        injectPluginPackage(mPluginPackage);
    }

    public void injectCancelPluginPackage() {
        injectPluginPackage(null);
    }

    /**
     * 注入插件, 那么接下来的所有资源操作, 都会被接管
     */
    public void injectPluginPackage(DLPluginPackage pluginPackage) {
        if (mActivity != null) {
            if (mActivity instanceof ProxyActivity) {
                ((ProxyActivity) mActivity).setPluginPackage(pluginPackage);
            } else if (mActivity instanceof ProxyCompatActivity) {
                ((ProxyCompatActivity) mActivity).setPluginPackage(pluginPackage);
            }
        }
    }

    /**
     * @see UILayoutImpl#finishActivity()
     */
    public void finishActivity() {
        if (mActivity != null) {
            if (mActivity instanceof UILayoutActivity) {
                ((UILayoutActivity) mActivity).finishSelf();
            } else {
                mActivity.finish();
            }
        }
    }

    /**
     * @see UILayoutActivity#checkPermissions(String[], Action1)
     */
    public void checkPermissions(String[] permissions, final Action1<Boolean> onResult) {
        if (mActivity != null) {
            if (mActivity instanceof UILayoutActivity) {
                ((UILayoutActivity) mActivity).checkPermissions(permissions, onResult);
            } else {
                if (mActivity.isDestroyed()) {
                    return;
                }

                checkPermissionsResult(permissions, new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (s.contains("0")) {
                            //有权限被拒绝
                            onResult.call(false);
                        } else {
                            //所欲权限通过
                            onResult.call(true);
                        }
                    }
                });
            }
        }
    }

    /**
     * @see UILayoutActivity#checkPermissionsResult(String[], Action1)
     */
    public void checkPermissionsResult(String[] permissions, final Action1<String> onResult) {
        if (mRxPermissions == null) {
            mRxPermissions = new RxPermissions(mActivity);
        }
        mRxPermissions.requestEach(permissions)
                .map(new Func1<Permission, String>() {
                    @Override
                    public String call(Permission permission) {
                        if (permission.granted) {
                            return permission.name + "1";
                        }
                        return permission.name + "0";
                    }
                })
                .scan(new Func2<String, String, String>() {
                    @Override
                    public String call(String s, String s2) {
                        return s + ":" + s2;
                    }
                })
                .last()
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        L.e("\n" + this.getClass().getSimpleName() + " 权限状态-->\n"
                                + s.replaceAll("1", " 允许").replaceAll("0", " 拒绝").replaceAll(":", "\n"));
                        onResult.call(s);
                    }
                });
//                .subscribe(new Action1<Permission>() {
//                    @Override
//                    public void call(Permission permission) {
//                        if (permission.granted) {
//                            T.show(UILayoutActivity.this, "权限允许");
//                        } else {
//                            notifyAppDetailView();
//                            T.show(UILayoutActivity.this, "权限被拒绝");
//                        }
//                    }
//                });
    }

    public UIIViewImpl setDelayFinish(long delayFinish) {
        this.delayFinish = delayFinish;
        return this;
    }


    @Override
    public boolean needHideSoftInputForStart() {
        return true;
    }

    @Override
    public boolean needHideSoftInputForFinish() {
        return true;
    }

    @Override
    public boolean onHideSoftInputByTouchDown(View touchInView) {
        //让LayoutImpl自动处理
        return false;
    }

    public TitleBarPattern createTitleBarPattern() {
        return TitleBarPattern.build()
                .setTitleBarBGColor(SkinHelper.getSkin().getThemeColor())
                .setShowBackImageView(!haveParentILayout() && ((UILayoutImpl) mILayout).getValidAttachViewSize() >= 1)
                .setOnBackListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (UIIViewImpl.this instanceof UIBaseView) {
                            ((UIBaseView) UIIViewImpl.this).onTitleBackListener();
                        } else {
                            UIIViewImpl.this.requestBackPressed();
                        }
                    }
                })
                ;
    }

    public interface OnCountDown {
        void onCountDown(int count /*MaxCount 到 0 的值*/);
    }
}
