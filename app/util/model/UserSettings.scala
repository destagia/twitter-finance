package miyatin.util.model

import reactivemongo.bson._

case class UserSettings (
    userId: String,
    timelineNotify: Boolean,
    hashTag: String
)
object UserSettings {

    def default(parentId: String) = UserSettings(parentId, true, "twifi")

    implicit val bsonWriter = new BSONDocumentWriter[UserSettings] {
        def write(s: UserSettings) = BSONDocument (
            "userId" -> s.userId,
            "timelineNotify" -> s.timelineNotify,
            "hashTag" -> s.hashTag
        )
    }

    implicit val bsonReader = new BSONDocumentReader[UserSettings] {
        def read(b: BSONDocument) = UserSettings (
            b.getAs[String]("userId").get,
            b.getAs[Boolean]("timelineNotify").getOrElse(false),
            b.getAs[String]("hashTag").getOrElse("twifi")
        )
    }
}
