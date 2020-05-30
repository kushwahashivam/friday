package com.ai.friday

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.friday.config.Config
import com.ai.friday.utils.PersonData
import com.ai.friday.starredpersons.StarredPersonAdapter
import com.ai.friday.utils.ServerRequestLoader
import com.ai.friday.utils.Utils
import kotlinx.android.synthetic.main.activity_starred_persons.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.collections.ArrayList


class StarredPersonsActivity : AppCompatActivity(), StarredPersonAdapter.ItemClickListener {

    private lateinit var accessToken: String
    private val starredRequestLoader = ServerRequestLoader(Config.URL_GET_STARRED_PERSONS)
    private val addStarRequestLoader = ServerRequestLoader(Config.URL_ADD_STAR_TO_PERSON)
    private val removeStarRequestLoader = ServerRequestLoader(Config.URL_REMOVE_STAR_FROM_PERSON)
    private lateinit var viewManager: LinearLayoutManager
    private lateinit var viewAdapter: StarredPersonAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var starredResult: ArrayList<PersonData>
    private lateinit var personDetailsLayout: LinearLayout
    private lateinit var popupWindow: PopupWindow
    private var starredResultClickable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starred_persons)

        /* Check for access token */
        val temp = Utils.getAccessToken(filesDir)
        if (temp == null) {
            gotoLogin()
        } else {
            accessToken = temp
        }
        /* ------------------------------------------------------------------------------------------ */

        /* Initiate recycler view */
        viewManager = LinearLayoutManager(this)
        viewAdapter = StarredPersonAdapter(this)
        recyclerView = rv_starred_person_result.apply {
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
                starredResultClickable = true
            }
        popupWindow.width = ViewGroup.LayoutParams.MATCH_PARENT
        popupWindow.height = 900
        popupWindow.animationStyle = -1
        /* ------------------------------------------------------------------------------------------- */
        /* Load starred persons list */
        refresh()
        /* ------------------------------------------------------------------------------------------- */
    }

    private fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            val requestData = "{'access_token': '$accessToken'}"
            val requestJsonObject = JSONObject(requestData)
            try {
                val responseJsonObject = starredRequestLoader.makeRequest(requestJsonObject)
                if (responseJsonObject == null) {
                    displayToast("Server connection error")
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
                        updateStarredResult(list)
                    }
                    in Config.CODES_AUTH_ERROR -> gotoLogin()
                    else -> {
                        displayToast(responseJsonObject.getString("message"))
                    }
                }
            } catch (e: SocketTimeoutException) {
                displayToast("Socket timeout error.")
            } catch (e: ConnectException) {
                displayToast("Server may be offline.")
            }
        }
    }

    private fun updateStarredResult(list: ArrayList<PersonData>) {
        CoroutineScope(Dispatchers.Main).launch {
            starredResult = list
            viewAdapter.data = list
            viewAdapter.notifyDataSetChanged()
        }
    }

    private fun gotoLogin() {
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(this@StarredPersonsActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun displayToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val toast = Toast.makeText(this@StarredPersonsActivity, message, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun setStarOnPopupWindow(resource: Int){
        CoroutineScope(Dispatchers.Main).launch {
            personDetailsLayout.findViewById<ImageView>(R.id.iv_star_person)
                .setImageResource(resource)
        }
    }

    override fun onItemClick(position: Int) {
        if (!starredResultClickable)
            return
        personDetailsLayout.apply {
            findViewById<ImageView>(R.id.iv_person_image).setImageBitmap(starredResult[position].image)
            findViewById<TextView>(R.id.tv_person_name).text = starredResult[position].name
            findViewById<TextView>(R.id.tv_person_data).text = starredResult[position].data
            findViewById<ImageView>(R.id.iv_star_person).apply {
                if(starredResult[position].star == 1)
                    setImageResource(R.drawable.star_red)
                else
                    setImageResource(R.drawable.star_green)
                setOnClickListener{toggleStar(position)}
            }
        }
        popupWindow.contentView = personDetailsLayout
        popupWindow.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
        popupWindow.update()
        starredResultClickable = false
    }

    override fun toggleStar(position: Int) {
        val requestData = "{ 'access_token': '$accessToken', 'id': '${starredResult[position].id}' }"
        val requestJsonObject = JSONObject(requestData)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (starredResult[position].star == 1) {
                    val responseJsonObject = removeStarRequestLoader.makeRequest(requestJsonObject)
                    if (responseJsonObject == null) {
                        displayToast("Server connection error")
                        return@launch
                    }
                    when (responseJsonObject.getInt("status_code")) {
                        Config.CODE_SUCCESS -> {
                            setStarOnPopupWindow(R.drawable.star_green)
                            starredResult[position].star = 0
                            updateStarredResult(starredResult)
                        }
                        in Config.CODES_AUTH_ERROR -> gotoLogin()
                        else -> displayToast(responseJsonObject.getString("message"))
                    }
                } else {
                    val responseJsonObject = addStarRequestLoader.makeRequest(requestJsonObject)
                    if (responseJsonObject == null) {
                        displayToast("Server connection error")
                        return@launch
                    }
                    when (responseJsonObject.getInt("status_code")) {
                        Config.CODE_SUCCESS -> {
                            setStarOnPopupWindow(R.drawable.star_red)
                            starredResult[position].star = 1
                            updateStarredResult(starredResult)
                        }
                        in Config.CODES_AUTH_ERROR -> gotoLogin()
                        else -> displayToast(responseJsonObject.getString("message"))
                    }
                }
            } catch (e: SocketTimeoutException) {
                displayToast("Socket timeout error.")
            } catch (e: ConnectException) {
                displayToast("Server may be offline.")
            }
        }
    }
}