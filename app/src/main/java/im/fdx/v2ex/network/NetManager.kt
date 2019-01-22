package im.fdx.v2ex.network

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import im.fdx.v2ex.R
import im.fdx.v2ex.myApp
import okhttp3.*
import org.jetbrains.anko.toast
import java.io.IOException

/**
 * Created by a708 on 15-8-13.
 * 用于对API和网页处理的类
 *
 * 这是最苦力活的一个类，而且很有可能会继续改变.
 * 由于v2ex提供的接口不够实时，采用解析网页方式 。
 */

object NetManager {

    const val HTTPS_V2EX_BASE = "https://www.v2ex.com"

    const val API_TOPIC = "$HTTPS_V2EX_BASE/api/topics/show.json"

    const val DAILY_CHECK = "$HTTPS_V2EX_BASE/mission/daily"

    const val SIGN_UP_URL = "$HTTPS_V2EX_BASE/signup"

    const val SIGN_IN_URL = "$HTTPS_V2EX_BASE/signin"
    //以下,接受以下参数之一：
    //    username: 用户名
    //    id: 用户在 V2EX 的数字 ID
    const val API_USER = "$HTTPS_V2EX_BASE/api/members/show.json"
    //以下接收参数：
    //     topic_id: 主题ID
    // fdx_comment: 坑爹，官网没找到。怪不得没法子
    @Deprecated("不是实时的")
    const val API_REPLIES = "$HTTPS_V2EX_BASE/api/replies/show.json"

    @Deprecated("不用API，用html解析。虽慢，但统一")
    const val URL_ALL_NODE = "$HTTPS_V2EX_BASE/api/nodes/all.json"

    const val URL_ALL_NODE_WEB = "$HTTPS_V2EX_BASE/planes"

    const val URL_FOLLOWING = "$HTTPS_V2EX_BASE/my/following"

    /**
     * 第三方提供的好用的搜索api, 在app中默认使用最近帖子 todo 以后增加多filter功能
     */
    const val API_SEARCH = "https://www.sov2ex.com/api/search"

    var myGson = Gson()

    @JvmOverloads
    fun dealError(context: Context?, errorCode: Int = -1, swipe: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null) {

        if (context is Activity && !context.isFinishing) {
            context.runOnUiThread {
                swipe?.isRefreshing = false
                when (errorCode) {
                    -1 -> context.toast(context.getString(R.string.error_network))
                    302 -> context.toast(context.getString(R.string.error_auth_failure))
                    else -> context.toast(context.getString(R.string.error_network))
                }
            }
        }
    }

    /**
     * 两步验证，对话框
     */
    fun showTwoStepDialog(activity: Activity) {
        val dialogEt = LayoutInflater.from(activity).inflate(R.layout.dialog_et, null)
        val etCode = dialogEt.findViewById<EditText>(R.id.et_two_step_code)
        AlertDialog.Builder(activity, R.style.AppTheme_Simple)
                .setTitle("您开启了两步验证")
                .setPositiveButton("验证") { _, _ ->
                    NetManager.finishLogin(etCode.text.toString(), activity)
                }
                .setNegativeButton("退出登录") { _, _ ->
                    HttpHelper.myCookieJar.clear()
                    myApp.setLogin(false)

                }
                .setView(dialogEt).show()
    }

    /**
     * 两步验证，完成登录
     */
    private fun finishLogin(code: String, activity: Activity) {
        val twoStepUrl = "https://www.v2ex.com/2fa"
        vCall(twoStepUrl).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                dealError(activity)
            }

            override fun onResponse(call: Call?, response: Response?) {
                if (response?.code() == 200) {
                    val bodyStr = response.body()?.string()!!
                    val once = Parser(bodyStr).getOnceNum()
                    val body: RequestBody = FormBody.Builder()
                            .add("code", code)
                            .add("once", once).build()
                    HttpHelper.OK_CLIENT.newCall(Request.Builder()
                            .post(body)
                            .url(twoStepUrl)
                            .build()).enqueue(object : Callback {
                        override fun onFailure(call: Call?, e: IOException?) {
                            dealError(activity)
                        }

                        override fun onResponse(call: Call?, response: Response?) {
                            activity.runOnUiThread {
                                if (response?.code() == 302) {
                                    activity.toast("登录成功")
                                    myApp.setLogin(true)
                                } else activity.toast("登录失败")
                            }
                        }
                    })
                }
            }
        })
    }
}