package im.fdx.v2ex.ui.node

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import com.elvishew.xlog.XLog
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import im.fdx.v2ex.MyApp
import im.fdx.v2ex.R
import im.fdx.v2ex.network.HttpHelper
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.main.NewTopicActivity
import im.fdx.v2ex.ui.main.TopicsFragment
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.handleAlphaOnTitle
import im.fdx.v2ex.utils.extensions.load
import im.fdx.v2ex.utils.extensions.logd
import im.fdx.v2ex.utils.extensions.setUpToolbar
import kotlinx.android.synthetic.main.activity_node.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import java.io.IOException


class NodeActivity : BaseActivity() {

    private var token: String? = null
    private var isFollowed = false
    private lateinit var nodeName: String
    private var mNode: Node? = null
    private lateinit var mMenu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_node)
        setUpToolbar()
        supportActionBar?.setDisplayShowTitleEnabled(false) //很关键，不会一闪而过一个东西

      appbar_node.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout1, verticalOffset ->
            val maxScroll = appBarLayout1.totalScrollRange
            val percentage = Math.abs(verticalOffset).toDouble() / maxScroll.toDouble()
            handleAlphaOnTitle(rl_node_header, divider, percentage.toFloat())
        })

        fab_node.setOnClickListener {
            startActivity<NewTopicActivity>(Keys.KEY_NODE_NAME to nodeName)
        }

        if (!MyApp.get().isLogin) {
            fab_node.hide()
        }

        nodeName = when {
            intent.data != null -> intent.data!!.pathSegments[1]
            intent.getStringExtra(Keys.KEY_NODE_NAME) != null -> intent.getStringExtra(Keys.KEY_NODE_NAME)
            else -> ""
        }
        val fragment: TopicsFragment = TopicsFragment().apply {
            arguments = bundleOf(Keys.KEY_NODE_NAME to nodeName)
        }
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment, "MyActivity")
                .commit()
        getNodeInfo()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_node, menu)
        mMenu = menu!!
        if (!MyApp.get().isLogin) {
            menu.findItem(R.id.menu_follow)?.isVisible = false
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_follow -> {
                switchFollowAndRefresh(isFollowed)
            }
        }
        return true
    }

    private fun switchFollowAndRefresh(isFavorite: Boolean) {
        HttpHelper.OK_CLIENT.newCall(Request.Builder()
                .url("${NetManager.HTTPS_V2EX_BASE}/${if (isFavorite) "un" else ""}$token")
                .build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                NetManager.dealError(this@NodeActivity)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.code() == 302) {
                    getNodeInfo()
                    runOnUiThread { toast("${if (isFavorite) "取消" else ""}关注成功") }
                }
            }
        })
    }

    private fun getNodeInfo() {
        val requestURL = "${NetManager.HTTPS_V2EX_BASE}/go/$nodeName"
        logd("url:$requestURL")
        vCall(requestURL).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                NetManager.dealError(this@NodeActivity)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: okhttp3.Response) {

                val code = response.code()
                if (code != 200) {
                    NetManager.dealError(this@NodeActivity, errorCode =  code)
                    return
                }
                val html = response.body()?.string()!!
                val parser = Parser(html)
                try {
                    mNode = parser.getOneNode()
                } catch (e: Exception) {
                    NetManager.dealError(this@NodeActivity, errorMsg = e.message ?: "unknown error" )
                }
                isFollowed = parser.isNodeFollowed()
                token = parser.getOnce()

                runOnUiThread {
                    iv_node_image.load(mNode?.avatarLargeUrl)
                    logd(mNode?.title)
                    ctl_node?.title = mNode?.title
                    tv_node_details.text = mNode?.header
                    tv_topic_num.text = getString(R.string.topic_number, mNode?.topics)
                    if (isFollowed) {
                        mMenu.findItem(R.id.menu_follow).setIcon(R.drawable.ic_favorite_white_24dp)
                    } else {
                        mMenu.findItem(R.id.menu_follow).setIcon(R.drawable.ic_favorite_border_white_24dp)
                    }
                }
            }
        })
    }
}
