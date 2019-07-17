package im.fdx.v2ex.ui.details

import androidx.room.*

@Entity(tableName = "my_reply")
data class MyReply(

        @PrimaryKey
        @ColumnInfo(name = "topic_id")
        var topicId :String,
        @ColumnInfo(name = "content")
        var content : String
)


