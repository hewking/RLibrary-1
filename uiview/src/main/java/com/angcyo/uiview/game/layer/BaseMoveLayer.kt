package com.angcyo.uiview.game.layer

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.angcyo.library.utils.L
import com.angcyo.uiview.kotlin.getBoundsWith

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：用来控制从一个点, 移动到另一个点的Layer
 * 创建人员：Robi
 * 创建时间：2017/12/15 15:34
 * 修改人员：Robi
 * 修改时间：2017/12/15 15:34
 * 修改备注：
 * Version: 1.0.0
 */
open class BaseMoveLayer : BaseFrameLayer() {

}

open class MoveBean(val drawables: Array<Drawable>,
                    val startPoint: Point /*开始的点*/,
                    val endPoint: Point /*结束的点*/) : FrameBean(drawables, startPoint) {

    val drawRect = Rect()

    private var startDrawTime = 0L

    /**起点移动到终点所需要的时间 毫秒*/
    var maxMoveTime: Int = 1000
        get() {
            return field / 1000
        }

    /**是否是匀速移动*/
    var constantSpeed = true

    /**是否循环移动*/
    var isLoopMove = false

    init {
        loop = true
        drawDrawable.let {
            drawRect.set(it.getBoundsWith(centerPoint, parentRect))
        }
    }

    override fun draw(canvas: Canvas, gameStartTime: Long, lastRenderTime: Long, nowRenderTime: Long, onDrawEnd: () -> Unit) {
        val nowTime = System.currentTimeMillis()

        if (startDrawTime > 0L && (nowTime - startDrawTime) / 1000F > maxMoveTime) {
            if (isLoopMove) {
                onLoopMove()
            } else {
                onDrawEnd.invoke()
            }
        } else {
            if (startDrawTime == 0L) {
                startDrawTime = nowTime
            }
            val time = (nowTime - startDrawTime) / 1000F

            drawPoint.set(x(time).toInt(), y(time).toInt())

            //L.i("call: draw -> ${maxMoveTime} $time $drawPoint $startPoint $endPoint ${aX()} ${aY()}")
            super.draw(canvas, gameStartTime, lastRenderTime, nowRenderTime, onDrawEnd)
            L.w("${drawDrawable.bounds}")
        }
    }

    open fun onLoopMove() {
        startDrawTime = 0L
        drawPoint.set(startPoint.x, startPoint.y)
    }

    private fun aX(): Float = (endPoint.x - startPoint.x).toFloat() / (maxMoveTime * maxMoveTime)
    private fun aY(): Float = (endPoint.y - startPoint.y).toFloat() / (maxMoveTime * maxMoveTime)

    private fun vX() = ((endPoint.x - startPoint.x).toFloat() / maxMoveTime)
    private fun vY() = ((endPoint.y - startPoint.y).toFloat() / maxMoveTime)

    private fun x(t: Float) = startPoint.x + if (constantSpeed) vX() * t else aX() * t * t
    private fun y(t: Float) = startPoint.y + if (constantSpeed) vY() * t else aY() * t * t
}