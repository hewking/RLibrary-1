package com.angcyo.uiview.recycler;

/**
 * Created by angcyo on 2016-01-30.
 */

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.utils.ScreenUtil;
import com.angcyo.uiview.view.DelayClick;
import com.angcyo.uiview.view.RClickListener;
import com.angcyo.uiview.view.UIIViewImpl;
import com.angcyo.uiview.widget.Button;
import com.angcyo.uiview.widget.ExEditText;
import com.angcyo.uiview.widget.GlideImageView;
import com.angcyo.uiview.widget.ItemInfoLayout;
import com.angcyo.uiview.widget.ItemSubInfoLayout;
import com.angcyo.uiview.widget.RExTextView;
import com.angcyo.uiview.widget.RImageView;
import com.angcyo.uiview.widget.RTextView;
import com.angcyo.uiview.widget.TimeTextView;
import com.angcyo.uiview.widget.viewpager.RViewPager;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 通用ViewHolder
 */
public class RBaseViewHolder extends RecyclerView.ViewHolder {
    private SparseArray<WeakReference<View>> sparseArray;
    private int viewType = -1;

    public RBaseViewHolder(View itemView) {
        this(itemView, -1);
    }

    public RBaseViewHolder(View itemView, int viewType) {
        super(itemView);
        sparseArray = new SparseArray();
        this.viewType = viewType;
    }

    /**
     * 填充两个字段相同的数据对象
     */
    public static void fill(Object from, Object to) {
        Field[] fromFields = from.getClass().getDeclaredFields();
        Field[] toFields = to.getClass().getDeclaredFields();
        for (Field f : fromFields) {
            String name = f.getName();
            for (Field t : toFields) {
                String tName = t.getName();
                if (name.equalsIgnoreCase(tName)) {
                    try {
                        f.setAccessible(true);
                        t.setAccessible(true);
                        t.set(to, f.get(from));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

        }
    }

    /**
     * 从object对象中, 获取字段name的get方法
     */
    public static Method getMethod(Object object, String name) {
        Method result = null;
        Method[] methods = object.getClass().getDeclaredMethods();
        for (Method method : methods) {
            method.setAccessible(true);
            String methodName = method.getName();
//            L.e("方法名:" + methodName);
            if (methodName.equalsIgnoreCase("realmGet$" + name)) {
                //优先从realm中获取方法名
//                L.e("方法名匹配到:" + methodName);
                result = method;
                break;
            } else if (methodName.equalsIgnoreCase("get" + name)) {
//                L.e("方法名匹配到:" + methodName);
                result = method;
                break;
            }
        }
        return result;
    }

    /**
     * 清理缓存
     */
    public void clear() {
        sparseArray.clear();
    }

    public int getViewType() {
        return viewType == -1 ? super.getItemViewType() : viewType;
    }

    public <T extends View> T v(@IdRes int resId) {
        WeakReference<View> viewWeakReference = sparseArray.get(resId);
        View view;
        if (viewWeakReference == null) {
            view = itemView.findViewById(resId);
            sparseArray.put(resId, new WeakReference<>(view));
        } else {
            view = viewWeakReference.get();
            if (view == null) {
                view = itemView.findViewById(resId);
                sparseArray.put(resId, new WeakReference<>(view));
            }
        }
        return (T) view;
    }

    public <T extends View> T tag(String tag) {
        View view = itemView.findViewWithTag(tag);
        return (T) view;
    }

    public <T extends View> T v(String idName) {
        return (T) viewByName(idName);
    }

    public View view(@IdRes int resId) {
        return v(resId);
    }

    public boolean isVisible(@IdRes int resId) {
        return v(resId).getVisibility() == View.VISIBLE;
    }

    public View visible(@IdRes int resId) {
        return visible(v(resId));
    }

    public RBaseViewHolder visible(@IdRes int resId, boolean visible) {
        View view = v(resId);
        if (visible) {
            visible(view);
        } else {
            gone(view);
        }
        return this;
    }

    public RBaseViewHolder invisible(@IdRes int resId, boolean visible) {
        View view = v(resId);
        if (visible) {
            visible(view);
        } else {
            invisible(view);
        }
        return this;
    }

    public View visible(View view) {
        if (view != null) {
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
            }
        }
        return view;
    }

    public RBaseViewHolder enable(@IdRes int resId, boolean enable) {
        View view = v(resId);
        if (view != null) {
            if (view.isEnabled() != enable) {
                view.setEnabled(enable);
            }
        }
        return this;
    }

    public void invisible(@IdRes int resId) {
        invisible(v(resId));
    }

    public View invisible(View view) {
        if (view != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                view.clearAnimation();
                view.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    public void gone(@IdRes int resId) {
        gone(v(resId));
    }

    public RBaseViewHolder gone(View view) {
        if (view != null) {
            if (view.getVisibility() != View.GONE) {
                view.clearAnimation();
                view.setVisibility(View.GONE);
            }
        }
        return this;
    }

    public RRecyclerView reV(@IdRes int resId) {
        return (RRecyclerView) v(resId);
    }

    public RRecyclerView rv(@IdRes int resId) {
        return reV(resId);
    }

    public ViewPager pager(@IdRes int resId) {
        return v(resId);
    }

    public RViewPager rpager(@IdRes int resId) {
        return v(resId);
    }

    public RLoopRecyclerView loopV(@IdRes int resId) {
        return (RLoopRecyclerView) v(resId);
    }

    public RRecyclerView reV(String idName) {
        return (RRecyclerView) viewByName(idName);
    }

    public ItemSubInfoLayout sub(@IdRes int id) {
        return v(id);
    }

    public ItemInfoLayout item(@IdRes int id) {
        return v(id);
    }

    public Button button(@IdRes int id) {
        return v(id);
    }

    /**
     * 返回 TextView
     */
    public TextView tV(@IdRes int resId) {
        return (TextView) v(resId);
    }

    public TextView tv(@IdRes int resId) {
        return (TextView) v(resId);
    }

    public TimeTextView timeV(@IdRes int resId) {
        return (TimeTextView) v(resId);
    }

    public RTextView rtv(@IdRes int resId) {
        return (RTextView) v(resId);
    }

    public RExTextView rxtv(@IdRes int resId) {
        return (RExTextView) v(resId);
    }

    public RExTextView extv(@IdRes int resId) {
        return rxtv(resId);
    }

    public TextView tV(String idName) {
        return (TextView) v(idName);
    }

    public TextView textView(@IdRes int resId) {
        return tV(resId);
    }

    /**
     * 返回 CompoundButton
     */
    public CompoundButton cV(@IdRes int resId) {
        return (CompoundButton) v(resId);
    }

    public CompoundButton cb(@IdRes int resId) {
        return cV(resId);
    }

    public CompoundButton cV(String idName) {
        return (CompoundButton) v(idName);
    }

    /**
     * 返回 EditText
     */
    public EditText eV(@IdRes int resId) {
        return (EditText) v(resId);
    }

    public ExEditText exV(@IdRes int resId) {
        return (ExEditText) v(resId);
    }

    public EditText editView(@IdRes int resId) {
        return eV(resId);
    }

    /**
     * 返回 ImageView
     */
    public ImageView imgV(@IdRes int resId) {
        return (ImageView) v(resId);
    }

    public RImageView rimgV(@IdRes int resId) {
        return (RImageView) v(resId);
    }

    public GlideImageView glideImgV(@IdRes int resId) {
        return (GlideImageView) v(resId);
    }

    public GlideImageView gIV(@IdRes int resId) {
        return gv(resId);
    }

    public GlideImageView giv(@IdRes int resId) {
        return gv(resId);
    }

    public GlideImageView gv(@IdRes int resId) {
        return (GlideImageView) v(resId);
    }

    public ImageView imageView(@IdRes int resId) {
        return imgV(resId);
    }

    /**
     * 返回 ViewGroup
     */
    public ViewGroup groupV(@IdRes int resId) {
        return group(resId);
    }

    public ViewGroup group(@IdRes int resId) {
        return (ViewGroup) v(resId);
    }

    public ViewGroup viewGroup(@IdRes int resId) {
        return groupV(resId);
    }

    public ViewGroup vg(@IdRes int resId) {
        return groupV(resId);
    }

    public RecyclerView r(@IdRes int resId) {
        return (RecyclerView) v(resId);
    }

    public void click(@IdRes int id, final View.OnClickListener listener) {
        click(id, UIIViewImpl.DEFAULT_CLICK_DELAY_TIME, listener);
    }

    public void longClick(@IdRes int id, final View.OnClickListener listener) {
        longClick(v(id), listener);
    }

    public void longClick(View view, final View.OnClickListener listener) {
        if (view != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onClick(v);
                    return true;
                }
            });
        }
    }

    public void longItem(final View.OnClickListener listener) {
        longClick(itemView, listener);
    }

    public void click(View view, final View.OnClickListener listener) {
        click(view, UIIViewImpl.DEFAULT_CLICK_DELAY_TIME, listener);
    }

    public void clickItem(final View.OnClickListener listener) {
        click(itemView, UIIViewImpl.DEFAULT_CLICK_DELAY_TIME, listener);
    }

    public void click(@IdRes int id, int delay, final View.OnClickListener listener) {
        click(v(id), delay, listener);
    }

    public void click(View view, int delay, final View.OnClickListener listener) {
        if (view != null) {
            if (listener instanceof RClickListener) {
                view.setOnClickListener(listener);
            } else {
                view.setOnClickListener(new RClickListener(delay) {
                    @Override
                    public void onRClick(View view) {
                        if (listener != null) {
                            listener.onClick(view);
                        }
                    }
                });
            }
        }
    }

    public void delayClick(@IdRes int id, DelayClick listener) {
        click(id, listener);
    }

    /**
     * 单击某个View
     */
    public void clickView(View view) {
        if (view != null) {
            view.performClick();
        }
    }

    public void clickView(@IdRes int id) {
        clickView(view(id));
    }

    public void text(@IdRes int id, String text, View.OnClickListener listener) {
        View view = v(id);
        if (view != null) {
            view.setVisibility(View.VISIBLE);

            click(id, listener);

            if (view instanceof TextView) {
                ((TextView) view).setText(text);
            }
        }
    }

    public View viewByName(String name) {
        View view = v(getIdByName(name, "id"));
        return view;
    }

    /**
     * 根据name, 在主题中 寻找资源id
     */
    private int getIdByName(String name, String type) {
        Context context = itemView.getContext();
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public void fillView(Object bean) {
        fillView(bean, false);
    }

    public void fillView(Object bean, boolean hideForEmpty) {
        fillView(bean, hideForEmpty, false);
    }

    public void fillView(Object bean, boolean hideForEmpty, boolean withGetMethod) {
        fillView(null, bean, hideForEmpty, withGetMethod);
    }

    /**
     * 请勿在bean相当复杂的情况下, 使用此方法, 会消耗很多CPU性能.
     *
     * @param clz           为了效率, 并不会遍历父类的字段, 所以可以指定类
     * @param hideForEmpty  如果数据为空时, 是否隐藏View
     * @param withGetMethod 是否通过get方法获取对象字段的值
     */
    public void fillView(Class<?> clz, Object bean, boolean hideForEmpty, boolean withGetMethod) {
        if (bean == null) {
            return;
        }
        Field[] fields;
        if (clz == null) {
            fields = bean.getClass().getDeclaredFields();
        } else {
            fields = clz.getDeclaredFields();
        }
        for (Field f : fields) {
            f.setAccessible(true);
            String name = f.getName();
            try {
                View view = viewByName(name);
                if (view == null) {
                    view = viewByName(name + "_view");
                }

                if (view != null) {
                    String value = null;
                    if (withGetMethod) {
                        value = String.valueOf(getMethod(bean, name).invoke(bean));
                    } else {
                        try {
                            value = f.get(bean).toString();
                        } catch (Exception e) {
                            L.w("the clz=" + clz + " bean=" + bean.getClass().getSimpleName() + " field=" + name + " is null");
                        }
                    }
                    if (view instanceof TextView) {
                        if (TextUtils.isEmpty(value) && hideForEmpty) {
                            view.setVisibility(View.GONE);
                        } else {
                            view.setVisibility(View.VISIBLE);
                        }
                        ((TextView) view).setText(value);
                    } else if (view instanceof GlideImageView) {
                        ((GlideImageView) view).reset();
                        ((GlideImageView) view).setUrl(value);
                    } else if (view instanceof AppCompatImageView) {

                    } else if (view instanceof ImageView) {
//                        Glide.with(RApplication.getApp())
//                                .load(value)
//                                .placeholder(R.drawable.default_image)
//                                .error(R.drawable.default_image)
//                                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                                .centerCrop()
//                                .into(((ImageView) view));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void post(Runnable runnable) {
        itemView.post(runnable);
    }

    public void postDelay(Runnable runnable, long delayMillis) {
        itemView.postDelayed(runnable, delayMillis);
    }

    public void postDelay(long delayMillis, Runnable runnable) {
        postDelay(runnable, delayMillis);
    }

    public Context getContext() {
        return itemView.getContext();
    }

    /**
     * ItemView是否在屏幕中显示
     */
    public boolean isItemShowInScreen() {
        if (itemView == null) {
            return false;
        }

        if (itemView.getLeft() >= 0 && itemView.getRight() <= ScreenUtil.getScreenWidth()
                && itemView.getTop() >= 0 && itemView.getBottom() <= ScreenUtil.getScreenHeight()) {
            return true;
        }
        return false;
    }
}
