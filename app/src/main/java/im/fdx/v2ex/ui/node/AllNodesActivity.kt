package im.fdx.v2ex.ui.node

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import im.fdx.v2ex.R
import im.fdx.v2ex.databinding.ActivityAllNodesBinding
import im.fdx.v2ex.network.NetManager
import im.fdx.v2ex.network.Parser
import im.fdx.v2ex.network.start
import im.fdx.v2ex.network.vCall
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.dealError
import im.fdx.v2ex.utils.extensions.initTheme
import im.fdx.v2ex.utils.extensions.setUpToolbar
import okhttp3.Call
import okhttp3.Callback
import org.jetbrains.anko.startActivity
import java.io.IOException
import kotlin.concurrent.thread


class AllNodesActivity : BaseActivity() {

  private lateinit var mAdapter: AllNodesAdapterNew

  private lateinit var binding: ActivityAllNodesBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAllNodesBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setUpToolbar(getString(R.string.all_nodes))
    //这里是后续不卡的关键，但是第一次滑动还是卡
    val linearLayoutManager = object : androidx.recyclerview.widget.LinearLayoutManager(this) {
      override fun getExtraLayoutSpace(state: androidx.recyclerview.widget.RecyclerView.State?): Int {
        return 300
      }
    }
    binding.rvNode.apply {
      setHasFixedSize(true) //要做filter，不能用
      setItemViewCacheSize(20)
      layoutManager = linearLayoutManager
    }

    binding.swipeContainer.initTheme()
    binding.swipeContainer.setOnRefreshListener {
      getAllNodes()
    }

    val isChoose = intent.getBooleanExtra(Keys.KEY_TO_CHOOSE_NODE, false)

    mAdapter = AllNodesAdapterNew(this) {
      if (isChoose) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(Keys.KEY_NODE, it) })
        finish()
      } else {
        startActivity<NodeActivity>(Keys.KEY_NODE_NAME to it.name)
      }
    }
    binding.rvNode.adapter = mAdapter
    binding.swipeContainer.isRefreshing = true
    thread {
      getAllNodes()
    }
  }

  private fun getAllNodes() {

    vCall(NetManager.URL_ALL_NODE_WEB)
        .start(object : Callback {
          override fun onFailure(call: Call, e: IOException) {
            dealError(-1, binding.swipeContainer)
          }

          @Throws(IOException::class)
          override fun onResponse(call: Call, response: okhttp3.Response) {

            if (response.code != 200) {
              dealError(response.code)
              return
            }

            val htmlStr = response.body!!.string()
            val nodeModels = Parser(htmlStr).getAllNode()

            mAdapter.setData(nodeModels)
            runOnUiThread {
              mAdapter.notifyDataSetChanged()
              binding.swipeContainer.isRefreshing = false
            }

          }
        })
  }


  override fun onCreateOptionsMenu(menu: Menu): Boolean {

    menuInflater.inflate(R.menu.menu_all_node, menu)
    val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
    val menuItemCompat = menu.findItem(R.id.search_node)
    val searchView = menuItemCompat.actionView as SearchView
    val et = searchView.findViewById<EditText>(R.id.search_src_text)
    et.setTextColor(ContextCompat.getColor(this, R.color.toolbar_text))
    searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

      override fun onQueryTextSubmit(query: String) = false
      override fun onQueryTextChange(newText: String): Boolean {
        mAdapter.filter(newText)
        return true
      }
    })
    return true
  }
}

fun listToMap(nodes: List<Node>): MutableMap<String, MutableList<Node>> {
  val map = mutableMapOf<String, MutableList<Node>>()
  for (node in nodes) {
    if (map.containsKey(node.category)) {
      map[node.category]?.add(node)
    } else {
      val value = mutableListOf<Node>()
      value.add(node)
      node.category?.let {
        map[it] = value
      }
    }
  }

  return map
}