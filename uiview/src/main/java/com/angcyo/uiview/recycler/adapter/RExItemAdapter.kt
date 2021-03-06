package com.angcyo.uiview.recycler.adapter

import android.content.Context
import android.view.View
import com.angcyo.uiview.base.UIBaseRxView
import com.angcyo.uiview.recycler.RBaseViewHolder
import com.angcyo.uiview.utils.RUtils

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：实现Item Layout, Type , Bind 分发
 * 创建人员：Robi
 * 创建时间：2018/03/16 14:01
 * 修改人员：Robi
 * 修改时间：2018/03/16 14:01
 * 修改备注：
 * Version: 1.0.0
 */
open class RExItemAdapter<ItemType, DataType> : RExBaseAdapter<String, DataType, String> {

    var itemFactory: RExItemFactory<ItemType, DataType>

    constructor(context: Context, itemFactory: RExItemFactory<ItemType, DataType>) : super(context) {
        this.itemFactory = itemFactory
    }

    constructor(context: Context, datas: List<DataType>, itemFactory: RExItemFactory<ItemType, DataType>) : super(context, datas) {
        this.itemFactory = itemFactory
    }

    override fun getItemLayoutId(viewType: Int): Int {
        return itemFactory.getItemLayoutId(viewType)
    }

    override fun onChildViewAttachedToWindow(view: View, adapterPosition: Int, layoutPosition: Int) {
        super.onChildViewAttachedToWindow(view, adapterPosition, layoutPosition)
        getItemHolderByPosition(adapterPosition - headerCount)?.let {
            it.onChildViewAttachedToWindow(view, adapterPosition, layoutPosition)
        }
    }

    override fun onChildViewDetachedFromWindow(view: View, adapterPosition: Int, layoutPosition: Int) {
        super.onChildViewDetachedFromWindow(view, adapterPosition, layoutPosition)
        getItemHolderByPosition(adapterPosition - headerCount)?.let {
            it.onChildViewDetachedFromWindow(view, adapterPosition, layoutPosition)
        }
    }

    /**必须调用此方法*/
    open fun initItemFactory(uiview: UIBaseRxView?) {
        itemFactory.initItemFactory(uiview, this)
    }

    /**根据position, 返回Adapter对应的item类型*/
    override fun getItemType(position: Int): Int {
        val data: DataType? = if (position >= mAllDatas.size || position < 0) {
            //return NO_ITEM_TYPE
            null
        } else {
            mAllDatas[position]
        }
        return itemFactory.getItemType(data, position)
    }

    override fun onBindDataView(holder: RBaseViewHolder, posInData: Int, dataBean: DataType?) {
        super.onBindDataView(holder, posInData, dataBean)
        val itemHolder = itemFactory.getItemLayoutHolder(holder.itemViewType)
        itemHolder?.onBindItemDataView(holder, posInData, dataBean)
    }

    /**根据位置, 返回处理的ItemHolder*/
    open fun getItemHolderByPosition(position: Int): RExItemHolder<DataType>? {
        return itemFactory.getItemLayoutHolder(getItemType(position))
    }

    /**根据类型, 返回处理的ItemHolder*/
    fun getItemHolderByItemType(itemType: ItemType): RExItemHolder<DataType>? {
        var result: RExItemHolder<DataType>? = null
        mAllDatas.forEachIndexed { index, dataType ->
            if (itemType == itemFactory.getItemTypeFromData(dataType, index)) {
                result = getItemHolderByPosition(index)
            }
        }
        return result
    }

    /**根据类型, 返回相同类型对应的数据列表*/
    fun getDataByItemType(itemType: ItemType): MutableList<DataType> {
        val result = mutableListOf<DataType>()
        mAllDatas.forEachIndexed { index, dataType ->
            if (itemType == itemFactory.getItemTypeFromData(dataType, index)) {
                result.add(dataType)
            }
        }
        return result
    }

    /**根据类型, 返回相同类型对应的数据索引列表*/
    fun getIndexByItemType(itemType: ItemType): MutableList<Int> {
        val result = mutableListOf<Int>()
        mAllDatas.forEachIndexed { index, dataType ->
            if (itemType == itemFactory.getItemTypeFromData(dataType, index)) {
                result.add(index)
            }
        }
        return result
    }

    /**返回数据在相同类型数据列表中的索引*/
    fun indexOf(itemType: ItemType, data: DataType): Int {
        return getDataByItemType(itemType).indexOf(data)
    }

    fun removeDataByItemType(itemType: ItemType) {
        if (!RUtils.isListEmpty(mAllDatas)) {
            for (index in mAllDatas.size - 1 downTo 0) {
                if (itemType == itemFactory.getItemTypeFromData(mAllDatas[index], index)) {
                    deleteItem(index)
                }
            }
        }
    }
}