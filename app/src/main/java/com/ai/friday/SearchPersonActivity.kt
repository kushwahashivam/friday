package com.ai.friday

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.friday.config.Config
import com.ai.friday.utils.PersonData
import com.ai.friday.searchperson.SearchPersonAdapter
import com.ai.friday.utils.ServerRequestLoader
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_search_person.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.collections.ArrayList


class SearchPersonActivity : AppCompatActivity(), SearchPersonAdapter.ItemClickListener {

    private lateinit var accessToken: String
    private val searchRequestLoader = ServerRequestLoader(Config.URL_SEARCH_PERSON)
    private val addStarRequestLoader = ServerRequestLoader(Config.URL_ADD_STAR_TO_PERSON)
    private val removeStarRequestLoader = ServerRequestLoader(Config.URL_REMOVE_STAR_FROM_PERSON)
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: SearchPersonAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchResult: ArrayList<PersonData>
    private lateinit var personDetailsLayout: LinearLayout
    private lateinit var popupWindow: PopupWindow
    private var searchResultClickable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_person)

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            Utils.gotoLogin(this)
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Initiate recycler view */
        viewManager = LinearLayoutManager(this)
        viewAdapter = SearchPersonAdapter(this)
        recyclerView = rv_search_person_result.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Inflate person details layout */
        personDetailsLayout = layoutInflater.inflate(R.layout.person_details, null) as LinearLayout
        popupWindow = PopupWindow(this)
        personDetailsLayout.findViewById<ImageButton>(R.id.ib_close_person_details)
            .setOnClickListener {
                popupWindow.dismiss()
                searchResultClickable = true
            }
        popupWindow.width = ViewGroup.LayoutParams.MATCH_PARENT
        popupWindow.height = 900
        popupWindow.animationStyle = -1
        /* ------------------------------------------------------------------------------------------- */
    }

    fun search(view: View) {
        val name = et_person_name.text.trim()
        if (name.isEmpty())
            return
        CoroutineScope(Dispatchers.IO).launch {
            displaySearchProgressBar()
            val requestData = "{'access_token': '$accessToken', 'name': '$name'}"
            val requestJsonObject = JSONObject(requestData)
            try {
                val responseJsonObject = searchRequestLoader.makeRequest(requestJsonObject)
                if (responseJsonObject == null) {
                    Utils.displayToast(this@SearchPersonActivity, "Server connection error")
                    return@launch
                }
                when (responseJsonObject.getInt("status_code")) {
                    Config.CODE_SUCCESS -> {
                        val list = ArrayList<PersonData>()
                        val results = responseJsonObject.getJSONArray("result")
                        for (i in 0 until results.length()) {
                            val res = results.get(i) as JSONObject
                            val imageStr = res.getString("image")
                            val personImage = Utils.base64ToBitmap(imageStr)
                            val personId = res.getString("id")
                            val personName = res.getJSONObject("data").getString("name")
                            val star = res.getInt("star")
                            var personData = ""
                            val iterator: Iterator<String> = res.getJSONObject("data").keys()
                            for (key in iterator) {
                                if (key == "name")
                                    continue
                                personData += "$key: "
                                personData += res.getJSONObject("data").getString(key)
                                personData += "\n\n"
                            }
                            list.add(
                                PersonData(
                                    personId,
                                    personName,
                                    personImage,
                                    personData,
                                    star
                                )
                            )
                        }
                        updateSearchResult(list)
                        hideProgressBar()
                    }
                    in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@SearchPersonActivity)
                    else -> {
                        Utils.displayToast(this@SearchPersonActivity, responseJsonObject.getString("message"))
                        hideProgressBar()
                    }
                }
            } catch (e: SocketTimeoutException) {
                Utils.displayToast(this@SearchPersonActivity, "Socket timeout error.")
                hideProgressBar()
            } catch (e: ConnectException) {
                Utils.displayToast(this@SearchPersonActivity, "Server may be offline.")
                hideProgressBar()
            }
        }
    }

    private fun updateSearchResult(list: ArrayList<PersonData>) {
        CoroutineScope(Dispatchers.Main).launch {
            searchResult = list
            viewAdapter.data = list
            viewAdapter.notifyDataSetChanged()
        }
    }

    private fun displaySearchProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            btn_search_person.visibility = View.INVISIBLE
            pb_search_person.visibility = View.VISIBLE
        }
    }

    private fun hideProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            pb_search_person.visibility = View.INVISIBLE
            btn_search_person.visibility = View.VISIBLE
        }
    }

    private fun setStarOnPopupWindow(resource: Int){
        CoroutineScope(Dispatchers.Main).launch {
            personDetailsLayout.findViewById<ImageView>(R.id.iv_star_person)
                .setImageResource(resource)
        }
    }

    override fun onItemClick(position: Int) {
        if (!searchResultClickable)
            return
        personDetailsLayout.apply {
            findViewById<ImageView>(R.id.iv_person_image).setImageBitmap(searchResult[position].image)
            findViewById<TextView>(R.id.tv_person_name).text = searchResult[position].name
            findViewById<TextView>(R.id.tv_person_data).text = searchResult[position].data
            findViewById<ImageView>(R.id.iv_star_person).apply {
                if(searchResult[position].star == 1)
                    setImageResource(R.drawable.star_red)
                else
                    setImageResource(R.drawable.star_green)
                setOnClickListener{toggleStar(position)}
            }
        }
        popupWindow.contentView = personDetailsLayout
        popupWindow.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
        popupWindow.update()
        searchResultClickable = false
    }

    override fun toggleStar(position: Int) {
        val requestData = "{ 'access_token': '$accessToken', 'id': '${searchResult[position].id}' }"
        val requestJsonObject = JSONObject(requestData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (searchResult[position].star == 1) {
                    val responseJsonObject = removeStarRequestLoader.makeRequest(requestJsonObject)
                    if (responseJsonObject == null) {
                        Utils.displayToast(this@SearchPersonActivity, "Server connection error")
                        return@launch
                    }
                    when (responseJsonObject.getInt("status_code")) {
                        Config.CODE_SUCCESS -> {
                            setStarOnPopupWindow(R.drawable.star_green)
                            searchResult[position].star = 0
                            updateSearchResult(searchResult)
                        }
                        in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@SearchPersonActivity)
                        else -> Utils.displayToast(this@SearchPersonActivity, responseJsonObject.getString("message"))
                    }
                } else {
                    val responseJsonObject = addStarRequestLoader.makeRequest(requestJsonObject)
                    if (responseJsonObject == null) {
                        Utils.displayToast(this@SearchPersonActivity, "Server connection error")
                        return@launch
                    }
                    when (responseJsonObject.getInt("status_code")) {
                        Config.CODE_SUCCESS -> {
                            setStarOnPopupWindow(R.drawable.star_red)
                            searchResult[position].star = 1
                            updateSearchResult(searchResult)
                        }
                        in Config.CODES_AUTH_ERROR -> Utils.gotoLogin(this@SearchPersonActivity)
                        else -> Utils.displayToast(this@SearchPersonActivity, responseJsonObject.getString("message"))
                    }
                }
            } catch (e: SocketTimeoutException) {
                Utils.displayToast(this@SearchPersonActivity, "Socket timeout error.")
                hideProgressBar()
            } catch (e: ConnectException) {
                Utils.displayToast(this@SearchPersonActivity, "Server may be offline.")
                hideProgressBar()
            }
        }
    }
}