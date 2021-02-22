package com.sumaiya.customswipecard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sumaiya.customswipecard.customview.SwipeFlingAdapterView
import java.util.*

class MainActivity : AppCompatActivity() {
    private var cardItemArrayList: ArrayList<CardItem?>? = null
    private var cardAdapter: CardAdapter? = null
    private var flingContainer: SwipeFlingAdapterView? = null
    private val name =
        arrayOf("Dhaka", "Delhi", "London", "Paris", "New York")
    private val imgArray = intArrayOf(R.drawable.dhaka, R.drawable.delhi, R.drawable.london, R.drawable.paris, R.drawable.newyork)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUI()
        loadData()
    }

    private fun loadData() {
        for (i in name.indices) {
            val cardItem = CardItem()
            cardItem.setName(name[i])
            cardItem.setImgRsc(imgArray[i])
            cardItemArrayList!!.add(cardItem)
        }
        cardAdapter!!.notifyDataSetChanged()
    }

    private fun initUI() {
        cardItemArrayList = ArrayList<CardItem?>()
        flingContainer = findViewById<View>(R.id.frame) as SwipeFlingAdapterView
        cardAdapter = CardAdapter(this, cardItemArrayList!!)
        flingContainer!!.setAdapter(cardAdapter)
        flingContainer!!.setFlingListener(object : SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                Log.d("LIST", "removed object!")
                cardItemArrayList!!.removeAt(0)
                cardAdapter!!.notifyDataSetChanged()
            }

            override fun onScroll(v: Float) {}
            override fun onBottomCardExit(dataObject: Any?) {
                val skippedItem = dataObject as CardItem?
                Toast.makeText(this@MainActivity, "Botom!", Toast.LENGTH_SHORT).show()
            }

            override fun onLeftCardExit(dataObject: Any?) {
                Toast.makeText(this@MainActivity, "Left!", Toast.LENGTH_SHORT).show()
            }

            override fun onRightCardExit(dataObject: Any?) {
                Toast.makeText(this@MainActivity, "Right!", Toast.LENGTH_SHORT).show()
            }

            override fun onAdapterAboutToEmpty(itemsInAdapter: Int) {
                loadData();
            }
        })
        flingContainer!!.setOnItemClickListener(object :
            SwipeFlingAdapterView.OnItemClickListener {
            override fun onItemClicked(
                itemPosition: Int,
                dataObject: Any?
            ) {
            }
        })
    }

}