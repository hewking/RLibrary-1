package com.angcyo.uiview.dynamicload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.base.UILayoutActivity;
import com.angcyo.uiview.dynamicload.internal.DLPluginPackage;
import com.angcyo.uiview.utils.Reflect;
import com.angcyo.uiview.utils.T_;
import com.angcyo.uiview.view.IView;

/**
 * Created by angcyo on 2018/04/01 17:36
 */
public class ProxyStartActivity extends UILayoutActivity {

    /**
     * 需要启动那个包
     */
    public static final String START_PACKAGE_NAME = "start_package_name";

    /**
     * 启动的类名
     */
    public static final String START_CLASS_NAME = "start_class_name";
    /**
     * 需要启动的插件包名
     */
    String pluginPackageName;
    /**
     * 需要启动的插件类名
     */
    String pluginClassName;

    private DLPluginPackage mDLPluginPackage;

    public static void start(Activity activity, String packageName, String className) {
        Intent intent = new Intent(activity, ProxyStartActivity.class);
        intent.putExtra(START_PACKAGE_NAME, packageName);
        intent.putExtra(START_CLASS_NAME, className);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Intent intent = getIntent();
        pluginPackageName = intent.getStringExtra(START_PACKAGE_NAME);
        pluginClassName = intent.getStringExtra(START_CLASS_NAME);

        L.w("ProxyStartActivity", "mClass=" + pluginClassName + " mPackageName=" + pluginPackageName);

        mDLPluginPackage = RPlugin.INSTANCE.getPluginPackage(pluginPackageName);

        super.onCreate(savedInstanceState);
    }

    public void onProxyCreate() {
        if (mDLPluginPackage == null) {
            T_.error("插件启动失败.");
            finish();
        } else {
//            this.pluginPackage = pluginPackage;
            //setPluginPackage(pluginPackage);
            //getTheme().setTo(pluginPackage.resources.newTheme());
            Class<?> pluginClass = RPlugin.INSTANCE.loadPluginClass(mDLPluginPackage.classLoader, pluginClassName);

            if (pluginClass == null) {
                T_.error("插件类启动失败.");
                finish();
            } else {
                IView iView = Reflect.newObject(pluginClass);
                iView.setPluginPackage(mDLPluginPackage);
////                iView.setPluginPackage(pluginPackage);
//                setPluginPackage(pluginPackage);
                startIView(iView);
//                //startIView(new DynamicLoadUIView());
            }

        }
        //setTheme(R.style.BaseWhiteAppTheme);
    }

    @Override
    protected void onLoadView(Intent intent) {
        onProxyCreate();
    }

//    @Override
//    protected void onLoadView(Intent intent) {
//        if (pluginPackage != null) {
//            Class<?> pluginClass = RPlugin.INSTANCE.loadPluginClass(pluginPackage.classLoader, pluginClassName);
//            if (pluginClass == null) {
//                T_.error("插件类启动失败.");
//                finish();
//            } else {
//                IView iView = Reflect.newObject(pluginClass);
//////                iView.setPluginPackage(pluginPackage);
////                setPluginPackage(pluginPackage);
//                //startIView(iView);
////                //startIView(new DynamicLoadUIView());
//            }
//        } else {
//            Tip.tip("插件加载失败!");
//        }
//
//    }
}
