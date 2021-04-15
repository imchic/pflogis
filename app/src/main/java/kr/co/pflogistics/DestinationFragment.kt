/*
 * Create by hbim on 2021.
 * Copyright (c) 2021. hbim. All rights reserved.
 */

package kr.co.pflogistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class DestinationFragment() : Fragment(), ItemDragListener{

    lateinit var recyclerView: RecyclerView
    lateinit var layoutManager:LinearLayoutManager
    lateinit var itemTouchHelper: ItemTouchHelper
    lateinit var mainAdapter:MainAdapter
    var delArr = ArrayList<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_destination, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logUtil = LogUtil(TAG!!)
        recyclerView = view.findViewById(R.id.rv_tab_list) as RecyclerView
        layoutManager = LinearLayoutManager(this.context)
        layoutManager.orientation = LinearLayoutManager.VERTICAL;
        recyclerView.layoutManager = layoutManager

        val dataList = ArrayList<Data>()

        val gson = Gson()
        val db  = PfDB.getInstance(context!!)
        db!!.dao().getAll().observe(this) { values ->
            (JsonParser.parseString(gson.toJson(values)) as JsonArray).forEach { element ->
                logUtil.d(element.toString())
                dataList.add(Data((element as JsonObject).get("seq").asString.toInt(), element.get("addr").asString, element.get("lonlat").asString, element.get("memo").asString))
            }

            if(dataList.size > 0){

                mainAdapter = MainAdapter(context!!, dataList, this)

                recyclerView.apply {
                    adapter = mainAdapter
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
                }

                itemTouchHelper = ItemTouchHelper(ItemTouchHelperCallback(context!!, mainAdapter, dataList))
                itemTouchHelper.attachToRecyclerView(recyclerView)

            } else {
                Toast.makeText(context, "저장된 데이터가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            }

        }

        recyclerView.setOnClickListener { logUtil.d("rv click") }

    }

    companion object{
        private val TAG: String? = DestinationFragment::class.simpleName
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }


}