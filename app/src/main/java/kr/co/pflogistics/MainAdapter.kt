/*
 * Create by hbim on 2021.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tab_item.view.*

class MainAdapter(
    val dataList: MutableList<Data>,
    private val listener: ItemDragListener
) : RecyclerView.Adapter<MainAdapter.ViewHolder>(), ItemActionListener {

    private val logUtil = LogUtil(TAG!!)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tab_item, parent, false)
        return ViewHolder(view, listener)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])
    }

    override fun onItemMoved(from: Int, to: Int) {
        if (from == to) {
            //logUtil.d("${items[itemCount].addr.toString()}, ${items[itemCount].lonlat.toString()}")
            return
        }

        val fromItem = dataList.removeAt(from)
        dataList.add(to, fromItem)
        logUtil.d("move -> $dataList")
        notifyItemMoved(from, to)
    }

    override fun onItemSwiped(position: Int) {
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(itemView: View, listener: ItemDragListener) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.drag_handle.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    listener.onStartDrag(this)
                }
                v.performClick()

                //if (adapterPosition != RecyclerView.NO_POSITION){
                    //logUtil.d("${items[adapterPosition].addr.toString()}, ${items[adapterPosition].lonlat.toString()}")
                //}

                false
            }

        }

        fun bind(item: Data) {
            itemView.txt1.text = item.addr
            itemView.txt2.text = item.memo
        }
    }
    companion object {
        private val TAG: String? = MainAdapter::class.simpleName;
    }

}