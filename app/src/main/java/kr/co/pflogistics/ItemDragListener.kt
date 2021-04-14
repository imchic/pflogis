/*
 * Create by hbim on 2021.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

import androidx.recyclerview.widget.RecyclerView

interface ItemDragListener {
    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}