package miyatin.util.model

import reactivemongo.bson._
import twitter4j.{User => TwitterUser, TwitterFactory, Twitter, Query, Paging, _}
import twitter4j.auth._
import play.api.cache._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import miyatin.util.Implicits._
import scala.util.{Try, Success, Failure}
import collection.JavaConversions._
import miyatin.util.Implicits._
import miyatin.util._
import java.util.Date

case class User(
    id: String,                 // Twitter-finance上での識別ID
    twitterId: Long,            // twitterのId
    token: String,              // アクセスするためのトークン
    tokenSecret: String,        // アクセスするための秘密鍵
    lastTweetId: Option[Long],  // 記録した最後のツイート
    records: List[Record],      // 今までのレコード
    settings: UserSettings
) {
    lazy val account: Future[TwitterUser] = twitter.map(_.verifyCredentials())

    def twitter: Future[Twitter] = Future {
        controllers.Application.getCache(id) match {
            case Some(t) => t
            case None =>
                val accessToken = new AccessToken(token, tokenSecret, twitterId)
                val twitter = User.twitterFactory.getInstance()
                twitter.setOAuthAccessToken(accessToken);
                controllers.Application.setCache(id, twitter)
                twitter
        }
    }

    def todayItems = Await.result(todayWithItems.map(_._1), Duration.Inf)

    def todayWithItems: Future[(List[Item], List[Status])] = today map { list =>
        // 前回記録したツイートから最新のツイートまでtwifiのデータを抜き出す
        val items = list
        .filter { x =>
            // ハッシュタグにtwifiが含まれるものを検索
                x.getHashtagEntities().map(_.getText())
                .contains(settings.hashTag)
        }
        .map { status =>
                // ハッシュタグの中から値段を探す
                // さらに円以外の部分をStringからIntに変換
                val t = for {
                    m <- Try(status.getHashtagEntities().find(_.getText().contains("円")).get)
                    price <- Try(m.getText().replace("円", "").toInt)
                } yield {
                    Item(
                        Util.getUniqueID(),
                        status.getId(),
                        price.toInt,
                        status.getText().split("#")(0),
                        status.getMediaEntities().headOption.map(_.getMediaURL()),
                        status.getCreatedAt().getTime())
                }

            t match {
                case Success(item) => List(item)
                case Failure(e) => Nil
            }
        }
        .foldLeft(List[Item]())(_ ++ _)
        (items, list)
    }

    def runRecord: Future[Unit] = for {
        (items, list) <- todayWithItems
        r = {
            val d = new Date()
            Record(Util.getUniqueID(), id, d.toIntDate, items)
        }
        _ <- notifyRecordToTwitter(r)
        _ <- Mongo.insertOrUpdate(r)
        _ <- Mongo.updateUser(copy(lastTweetId = Some(list.head.getId)))
    } yield ()

    def notifyRecordToTwitter(r: Record): Future[Unit] = {
        if (settings.timelineNotify)
            for {
                t <- twitter
                _ <- Future(t.updateStatus(s"本日は${r.sum}円のお買い物をしました。\n#twifi"))
            } yield ()
        else Future(())
    }

    def today: Future[List[Status]] =
        lastTweetId match {
            case Some(sinceId) =>
                val paging = new Paging(sinceId)
                twitter.map(_.getUserTimeline(paging).toList)

            case None => Future(Nil)
        }

    def recordByDate(target: Date) = Mongo.findRecord(id, target.toIntDate)

}
object User {
    val twitterFactory = new TwitterFactory()

    implicit val bsonWriter = new BSONDocumentWriter[User]() {
        def write(u: User) =
            BSONDocument (
                "id" -> u.id,
                "twitterId" -> u.twitterId,
                "token" -> u.token,
                "tokenSecret" -> u.tokenSecret,
                "lastTweetId" -> u.lastTweetId,
                "settings" -> u.settings
            )
    }

    implicit val bsonReader = new BSONDocumentReader[User]() {
        def read(b: BSONDocument) = {
            val userId = b.getAs[String]("id").get
            val records = Await.result(Mongo.findRecords(userId), Duration.Inf)
            User(
                userId,
                b.getAs[Long]("twitterId").getOrElse(-1L),
                b.getAs[String]("token").getOrElse("undefind"),
                b.getAs[String]("tokenSecret").getOrElse("undefind"),
                b.getAs[Long]("lastTweetId"),
                records,
                b.getAs[UserSettings]("settings").getOrElse(UserSettings.default(userId))
            )
        }
    }
}