package im.fdx.v2ex.ui.details

import android.content.*
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import im.fdx.v2ex.R
import im.fdx.v2ex.ui.BaseActivity
import im.fdx.v2ex.ui.main.Topic
import im.fdx.v2ex.utils.Keys
import im.fdx.v2ex.utils.extensions.setUpToolbar
import kotlinx.android.synthetic.main.activity_details.*
import org.jetbrains.anko.toast


class TopicActivity : BaseActivity() {


  private lateinit var vpAdapter: VpAdapter

  private lateinit var mTopicId :String

  private var topicList : MutableList<Topic>? = null
  private var position = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_details)

    vpAdapter = VpAdapter(supportFragmentManager)
    parseIntent(intent)
  }


  private fun parseIntent(intent: Intent) {
    val data = intent.data
    val topicModel = intent.getParcelableExtra<Topic>(Keys.KEY_TOPIC_MODEL)
    val topicId = intent.getStringExtra(Keys.KEY_TOPIC_ID)
    val list = intent.getParcelableArrayListExtra<Topic>(Keys.KEY_TOPIC_LIST)
    val pos = intent.getIntExtra(Keys.KEY_POSITION, 0)
    when {
      data != null -> {
        mTopicId = data.pathSegments[1]
      }
      topicModel != null -> {
        mTopicId = topicModel.id
      }
      topicId != null -> {
        mTopicId = topicId
      }
      else -> {
        mTopicId = ""
      }
    }

    if(list!=null) {
      topicList = list
      position = pos
    }

    if (mTopicId.isEmpty()) {
      toast("主题打开失败")
      finish()
      return
    }

    //如果是从首页打开，那么会有所有列表信息，那么就可以获取到列表信息，达到左右滑动
    val out = topicList?.map { topic ->
      TopicDetailFragment().apply {
        arguments = bundleOf(Keys.KEY_TOPIC_MODEL to topic, Keys.KEY_TOPIC_ID to topic.id)
      }
    } ?:
      mutableListOf(TopicDetailFragment().apply {
        arguments = bundleOf(Keys.KEY_TOPIC_MODEL to topicModel, Keys.KEY_TOPIC_ID to mTopicId)
      })


    vpAdapter.initList(out)
    viewPager_detail.adapter = vpAdapter
    viewPager_detail.setCurrentItem(position, false)

    viewPager_detail.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
      }

      override fun onPageSelected(position: Int) {
      }
    })

  }
}


/**
 * 管理topicfragment， 存储当前位置等信息。
 *
 * todo 需要加入和endlessScrollListener的联动，不然这里的list没法增加。
 */
class VpAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm) {

  private val fgList : MutableList<TopicDetailFragment> = mutableListOf()


  fun initList(list: List<TopicDetailFragment>) {
    fgList.addAll(list)
  }

  fun addList(list: List<TopicDetailFragment>) {
    fgList.addAll(list)
  }

  override fun getItem(position: Int): Fragment {
    return fgList[position]
  }

  override fun getCount(): Int {
    return fgList.size
  }

}