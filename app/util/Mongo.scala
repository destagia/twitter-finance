package miyatin.util

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.api.collections._
import reactivemongo.core.commands._
import play.api.mvc.{Result, Request}
import play.api.mvc.Results.BadRequest
import play.api.libs.json.{JsObject, Json}
import twitter4j.Twitter
import miyatin.util.Implicits._
import scala.concurrent.Future
import scalaz._, Scalaz._
import model._
import scala.collection.mutable.{Map => Cache}
import java.util.Calendar

object Mongo {

    private val driver = new MongoDriver
    val ip = sys.env.getOrElse("TWIFIMONGO_PORT_27017_TCP_ADDR", "localhost") + ":27017"
    println("connecting... : " + ip)
    private val connection = driver.connection(List(ip))

    val db = connection.db("twitter-finance")
    val users = db.collection("users")
    val records = db.collection("records")

    def insert(u: User) = users.insert(u)

    def insert(r: Record) = records.insert(r)

    // If already record of the day exists, update item list.
    def insertOrUpdate(r: Record) = for {
        rOpt <- findRecord(r.userId, r.date)
        _ <- rOpt match {
            case Some(er) => pushRecordItem(er.id, r.items)
            case None => records.insert(r)
        }
    } yield ()

    def findUsers: Future[List[User]] =
        users.find(BSONDocument()).cursor[User].collect[List]()

    def findUser(query: BSONDocument): Future[Option[User]] = users.find(query).cursor[User].headOption.map(_ match {
        case Some(user) => User.getCache(user.id) match {
            case None => Some(user.updatePool)
            case cache => cache
        }
        case None => None
    })

    def findUser(id: String): Future[Option[User]] = User.getCache(id) match {
        case None => findUser(BSONDocument("id" -> id)).map(_.map(_.updatePool))
        case Some(user) => Future(Some(user))
    }

    def findUser(twitterId: Long): Future[Option[User]] = User.getCache(twitterId) match {
        case None => findUser(BSONDocument("twitterId" -> twitterId)).map(_.map(_.updatePool))
        case Some(user) => Future(Some(user))
    }

    def findRecord(query: BSONDocument): Future[Option[Record]] =
        records.find(query).cursor[Record].headOption

    def updateUser(user: User) = {
        users.update(BSONDocument("twitterId" -> user.twitterId), user)
    }

    def findRecord(userId: String, dateInt: Int): Future[Option[Record]] =
        findRecord(BSONDocument("userId" -> userId, "date" -> dateInt))

    def updateRecord(r: Record) =
        records.update(BSONDocument("id" -> r.id), r)

    // db.records.update({"id": ""}, {$push: {items: { "id" : "ODBScRevrEk5OKwc0r5Jw", "parentTweetId" : NumberLong("630240049104228352"), "price" : 300, "sentence" : "おかき\n", "date" : NumberLong("1439095911000") }}})
    def pushRecordItem(recordId: String, items: List[Item]) =
        records.update(
            BSONDocument("id" -> recordId),
            BSONDocument("$push" -> BSONDocument("items" -> BSONDocument("$each" -> items))))

    def findRecords(query: BSONDocument): Future[List[Record]] =
        records.find(query).cursor[Record].collect[List]()

    def findRecords(userId: String): Future[List[Record]] =
        findRecords(BSONDocument("userId" -> userId))

    def findUserAnd(f: => User => Result)(implicit request: Request[_]): Future[Result] =
        findUserWithRequest(request).map( _ match {
            case None => BadRequest(views.html.not_login())
            case Some(user) => f(user)
        })
    def findUserAndM(f: => User => Future[Result])(implicit request: Request[_]): Future[Result] =
        findUserWithRequest(request).flatMap(_ match {
            case Some(user) => f(user)
            case None => Future(BadRequest(views.html.not_login()))
        })

    def findUserTwitter(f: => Option[Twitter] => Result)(implicit request: Request[_])
    : Future[Result] = {
        for {
            userOpt <- findUserWithRequest(request)
            twitter <- userOpt.map(_.twitter).sequence
        }
        yield f(twitter)
    }

    def findUserWithRequest(implicit request: Request[_]): Future[Option[User]] =
        request.cookies.get("id") match {
            case Some(cookie) => Mongo.findUser(cookie.value)
            case None => Future(None)
        }


}
