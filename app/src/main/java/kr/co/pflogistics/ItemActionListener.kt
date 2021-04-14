/*
 * Create by hbim on 2021.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

interface ItemActionListener {
    fun onItemMoved(from: Int, to: Int)
    fun onItemSwiped(position: Int)
}