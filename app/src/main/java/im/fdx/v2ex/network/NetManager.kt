package im.fdx.v2ex.network

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import com.google.gson.Gson
import im.fdx.v2ex.R
import im.fdx.v2ex.model.MemberModel
import im.fdx.v2ex.model.NotificationModel
import im.fdx.v2ex.network.NetManager.Source.FROM_HOME
import im.fdx.v2ex.network.NetManager.Source.FROM_NODE
import im.fdx.v2ex.ui.details.ReplyModel
import im.fdx.v2ex.ui.main.TopicModel
import im.fdx.v2ex.ui.node.NodeModel
import im.fdx.v2ex.utils.ContentUtils
import im.fdx.v2ex.utils.TimeUtil
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Integer.parseInt
import java.util.*
import java.util.regex.Pattern

/**
 * Created by a708 on 15-8-13.
 * 用于对API和网页处理的类
 *
 * 这是最苦力活的一个类，而且很有可能会继续改变
 */

object NetManager {

    private val TAG = NetManager::class.java.simpleName

    val HTTPS_V2EX_BASE = "https://www.v2ex.com"

    val API_HOT = HTTPS_V2EX_BASE + "/api/topics/hot.json"
    val API_LATEST = HTTPS_V2EX_BASE + "/api/topics/latest.json"

    //以下,接受参数： name: 节点名
    val API_NODE = HTTPS_V2EX_BASE + "/api/nodes/show.json"


    val API_TOPIC = HTTPS_V2EX_BASE + "/api/topics/show.json"

    val DAILY_CHECK = HTTPS_V2EX_BASE + "/mission/daily"

    val SIGN_UP_URL = HTTPS_V2EX_BASE + "/signup"

    val SIGN_IN_URL = HTTPS_V2EX_BASE + "/signin"
    //以下,接受以下参数之一：
    //    username: 用户名
    //    id: 用户在 V2EX 的数字 ID
    val API_USER = HTTPS_V2EX_BASE + "/api/members/show.json"
    //以下接收参数：
    //     topic_id: 主题ID
    // fdx_comment: 坑爹，官网没找到。怪不得没法子
    @Deprecated("")
    val API_REPLIES = HTTPS_V2EX_BASE + "/api/replies/show.json"

    val URL_ALL_NODE = HTTPS_V2EX_BASE + "/api/nodes/all.json"

    fun parseToNotifications(html: Document): List<NotificationModel> {
        val body = html.body()
        val items = body.getElementsByAttributeValueStarting("id", "n_") ?: return emptyList()

        val notificationModels = ArrayList<NotificationModel>()
        for (item in items) {
            val notification = NotificationModel()

            val notificationId = item.attr("id").substring(2)
            notification.id = notificationId

            val contentElement = item.getElementsByClass("payload").first()
            var content = ""
            if (contentElement != null) {
                content = contentElement.text()
            }

            val time = item.getElementsByClass("snow").first().text()
            notification.time = time// 2/6


            val memberElement = item.getElementsByTag("a").first()
            val username = memberElement.attr("href").replace("/member/", "")
            val avatarUrl = memberElement.getElementsByClass("avatar").first().attr("src")
            val memberModel = MemberModel()
            memberModel.username = username
            memberModel.avatar_normal = avatarUrl
            notification.member = memberModel // 3/6

            val topicModel = TopicModel()

            val topicElement = item.getElementsByClass("fade").first()
            //            <a href="/t/348757#reply1">交互式《线性代数》学习资料</a>
            val href = topicElement.getElementsByAttributeValueStarting("href", "/t/").first().attr("href")

            topicModel.title = topicElement.getElementsByAttributeValueStarting("href", "/t/").first().text()

            val p = Pattern.compile("(?<=/t/)\\d+(?=#)")
            val matcher = p.matcher(href)
            if (matcher.find()) {
                val topicId = matcher.group()
                topicModel.id = topicId
            }

            val p2 = Pattern.compile("(?<=reply)d+\\b")
            val matcher2 = p2.matcher(href)
            if (matcher2.find()) {
                val replies = matcher2.group()
                notification.replyPosition = replies
            }
            notification.topic = topicModel //4/6
            val type = topicElement.ownText()
            notification.type = type
            notification.content = content //1/6
            notificationModels.add(notification)
        }
        return notificationModels
    }

    enum class Source {
        FROM_HOME, FROM_NODE
    }

    var myGson = Gson()

    fun parseTopicLists(html: Document, source: Source): List<TopicModel> {
        val topics = ArrayList<TopicModel>()

        //用okhttp模拟登录的话获取不到Content内容，必须再一步。

        val body = html.body()

        val items = when (source) {
            FROM_HOME -> body.getElementsByClass("cell item")
            FROM_NODE -> body.getElementsByAttributeValueStarting("class", "cell from")
        }
        for (item in items!!) {
            val topicModel = TopicModel()
            val title = item.getElementsByClass("item_title").first().text()

            val linkWithReply = item.getElementsByClass("item_title").first()
                    .getElementsByTag("a").first().attr("href")
            val replies = Integer.parseInt(linkWithReply.split("reply".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
            val p = Pattern.compile("(?<=/t/)\\d+")
            val matcher = p.matcher(linkWithReply)

            val id: String
            if (matcher.find()) {
                id = matcher.group()
            } else {
                return emptyList()
            }


            val nodeModel = NodeModel()
            if (source == FROM_HOME) {
                //  <a class="node" href="/go/career">职场话题</a>
                val nodeTitle = item.getElementsByClass("node").text()
                val nodeName = item.getElementsByClass("node").attr("href").substring(4)
                nodeModel.title = nodeTitle
                nodeModel.name = nodeName

            } else if (source == FROM_NODE) {
                val header = body.getElementsByClass("header").first()
                val strHeader = header.text()
                var nodeTitle = ""
                if (strHeader.contains("›")) {
                    nodeTitle = strHeader.split("›".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].trim { it <= ' ' }
                }

                val elements = html.head().getElementsByTag("script")
                val script = elements.last()
                //注意，script 的tag 不含 text。
                val strScript = script.html()
                val nodeName = strScript.split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                nodeModel.title = nodeTitle
                nodeModel.name = nodeName
            }


            topicModel.node = nodeModel

            val memberModel = MemberModel()
            //            <a href="/member/wineway">
            // <img src="//v2" class="avatar" border="0" align="default" style="max-width: 48px; max-height: 48px;"></a>
            val username = item.getElementsByTag("a").first().attr("href").substring(8)

            val avatarLarge = item.getElementsByClass("avatar").attr("src")
            memberModel.username = username
            memberModel.avatar_large = avatarLarge
            memberModel.avatar_normal = avatarLarge.replace("large", "normal")


            val smallItem = item.getElementsByClass("small fade").first().text()
            val created: Long
            if (!smallItem.contains("最后回复")) {
                created = -1L
            } else {
                var createdOriginal = ""
                when (source) {
                    FROM_HOME -> createdOriginal = smallItem.split("•".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
                    FROM_NODE -> createdOriginal = smallItem.split("•".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                }
                created = TimeUtil.toLong(createdOriginal)
            }
            topicModel.replies = replies
            topicModel.content = ""
            topicModel.setContentRendered("")
            topicModel.title = title

            topicModel.member = memberModel
            topicModel.id = id
            topicModel.created = created
            topics.add(topicModel)
        }
        return topics
    }

    fun parseToNode(html: Document): NodeModel {

        val nodeModel = NodeModel()
        //        Document html = Jsoup.parse(response);
        val body = html.body()
        val header = body.getElementsByClass("header").first()
        val contentElement = header.getElementsByClass("f12 gray").first()
        val content = if (contentElement == null) "" else contentElement.text()
        val number = header.getElementsByTag("strong").first().text()
        val strHeader = header.ownText().trim { it <= ' ' }

        if (header.getElementsByTag("img").first() != null) {
            val avatarLarge = header.getElementsByTag("img").first().attr("src")
            nodeModel.avatar_large = avatarLarge
            nodeModel.avatar_normal = avatarLarge.replace("large", "normal")
        }

        val elements = html.head().getElementsByTag("script")
        val script = elements.last()
        //注意，script 的tag 不含 text。
        val strScript = script.html()
        val nodeName = strScript.split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

        nodeModel.name = nodeName
        nodeModel.title = strHeader
        nodeModel.topics = Integer.parseInt(number)
        nodeModel.header = content

        return nodeModel
    }

    fun getPageValue(body: Element): IntArray {

        var currentPage = 0
        var totalPage = 0
        val pageInput = body.getElementsByClass("page_input").first() ?: return intArrayOf(-1, -1)
        try {
            currentPage = Integer.parseInt(pageInput.attr("value"))
            totalPage = Integer.parseInt(pageInput.attr("max"))
        } catch (ignored: NumberFormatException) {

        }

        return intArrayOf(currentPage, totalPage)


    }

    /**
     * @param body    网页
     * *
     * @param topicId todo 可以省略
     * *
     * @return TopicModel
     */
    fun parseResponseToTopic(body: Element, topicId: String): TopicModel {
        val topicModel = TopicModel(topicId)

        val title = body.getElementsByTag("h1").text()
        val contentElement = body.getElementsByClass("topic_content").first()

        val content = if (contentElement == null) "" else contentElement.text()
        val contentRendered = if (contentElement == null) "" else contentElement.html()
        val createdUnformed = body.getElementsByClass("header").first().getElementsByClass("gray").first().ownText() // · 44 分钟前用 iPhone 发布 · 192 次点击 &nbsp;

        val time = createdUnformed.split("·".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        val created = TimeUtil.toLong(time)

        var replyNum = ""
        val grays = body.getElementsByClass("gray")
        var hasReply = false
        for (gray in grays) {
            if (gray.text().contains("回复") && gray.text().contains("|")) {
                val wholeText = gray.text()
                val index = wholeText.indexOf("回复")
                replyNum = wholeText.substring(0, index - 1)
                if (!TextUtils.isEmpty(replyNum)) {
                    hasReply = true
                }
                break
            }
        }

        val replies = when {
            hasReply -> parseInt(replyNum)
            else -> 0
        }

        topicModel.setContentRendered(ContentUtils.format(contentRendered)) //done

        val member = MemberModel()
        val username = body.getElementsByClass("header").first()
                .getElementsByAttributeValueStarting("href", "/member/").text()
        member.username = username
        val largeAvatar = body.getElementsByClass("header").first()
                .getElementsByClass("avatar").attr("src")
        member.avatar_large = largeAvatar
        member.avatar_normal = largeAvatar.replace("large", "normal")
        val nodeElement = body.getElementsByClass("header").first()
                .getElementsByAttributeValueStarting("href", "/go/").first()
        val nodeName = nodeElement.attr("href").replace("/go/", "")
        val nodeTitle = nodeElement.text()
        val nodeModel = NodeModel()
        nodeModel.name = nodeName
        nodeModel.title = nodeTitle

        topicModel.member = member //done
        topicModel.replies = replies //done
        topicModel.created = created //done
        topicModel.title = title //done
        topicModel.content = content //done
        topicModel.node = nodeModel//done
        return topicModel
    }

    fun parseResponseToReplay(body: Element): ArrayList<ReplyModel> {

        val replyModels = ArrayList<ReplyModel>()

        val items = body.getElementsByAttributeValueStarting("id", "r_")
        for (item in items) {

            //            <div id="r_4157549" class="cell">
            val id = item.id().substring(2)

            val replyModel = ReplyModel()
            val memberModel = MemberModel()
            val avatar = item.getElementsByClass("avatar").attr("src")
            val username = item.getElementsByTag("strong").first().getElementsByAttributeValueStarting("href", "/member/").first().text()

            memberModel.avatar_large = avatar
            memberModel.avatar_normal = avatar.replace("large", "normal")
            memberModel.username = username

            val thanksOriginal = item.getElementsByClass("small fade").text()
            val thanks = when (thanksOriginal) {
                "" -> 0
                else -> parseInt(thanksOriginal.replace("♥ ", "").trim())
            }
            val thanked = item.getElementsByClass("thank_area thanked").first()
            replyModel.isThanked = thanked != null && "感谢已发送" == thanked.text()

            val createdOriginal = item.getElementsByClass("fade small").text()
            val replyContent = item.getElementsByClass("reply_content").first()
            replyModel.created = TimeUtil.toLong(createdOriginal)
            replyModel.member = memberModel
            replyModel.thanks = thanks

            replyModel.id = id
            replyModel.content = replyContent.text()
            replyModel.content_rendered = ContentUtils.format(replyContent.html())
            replyModels.add(replyModel)
        }
        return replyModels

    }

    @JvmOverloads fun dealError(context: Context, errorCode: Int = -1) {

        if (context is Activity)
            context.runOnUiThread {
                when (errorCode) {
                    -1 -> Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_SHORT).show()
                    302 ->

                        Toast.makeText(context, context.getString(R.string.error_auth_failure), Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, context.getString(R.string.error_network), Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun parseOnce(body: Element) = body.getElementsByAttributeValue("name", "once").first()?.attr("value")


    fun parseToVerifyCode(body: Element): String? {

        //        <a href="/favorite/topic/349111?t=eghsuwetutngpadqplmlnmbndvkycaft" class="tb">加入收藏</a>
        val element: Element?
        try {
            element = body.getElementsByClass("topic_buttons").first().getElementsByTag("a").first()
        } catch (e: NullPointerException) {
            return null
        }

        if (element != null) {
            val p = Pattern.compile("(?<=favorite/topic/\\d{1,10}\\?t=)\\w+")

            val matcher = p.matcher(element.outerHtml())
            if (matcher.find()) {
                return matcher.group()
            }
        }
        return null
    }

    fun parseToNode(string: String): ArrayList<NodeModel> {
        val element = Jsoup.parse(string).body()

        val nodeModels = ArrayList<NodeModel>()
        val items = element.getElementsByClass("grid_item")

        for (item in items) {
            val nodeModel = NodeModel()
            val id = item.attr("id").substring(2)
            nodeModel.id = id

            val title = item.getElementsByTag("div").first().ownText().trim { it <= ' ' }
            nodeModel.title = title
            val name = item.attr("href").replace("/go/", "")
            nodeModel.name = name

            val num = item.getElementsByTag("span").first().ownText().trim { it <= ' ' }

            nodeModel.topics = parseInt(num)

            val imageUrl = item.getElementsByTag("img").first().attr("src")
            nodeModel.avatar_large = imageUrl
            nodeModels.add(nodeModel)
        }

        return nodeModels
    }
}