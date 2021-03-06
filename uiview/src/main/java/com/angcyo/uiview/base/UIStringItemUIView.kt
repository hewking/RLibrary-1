package com.angcyo.uiview.base

import com.angcyo.uiview.recycler.adapter.IExStringDataType

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：用String来当做Item的type, 用来实现items
 * 创建人员：Robi
 * 创建时间：2018/03/21 14:42
 * 修改人员：Robi
 * 修改时间：2018/03/21 14:42
 * 修改备注：
 * Version: 1.0.0
 */

abstract class UIStringItemUIView : UIExItemUIView<String, IExStringDataType>() {

    override fun getItemTypeFromData(data: IExStringDataType?, position: Int): String? {
        return data?.itemDataType
    }

}