package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import miyatin.util._

import reactivemongo.bson.BSONDocument
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import collection.JavaConversions._
import scalaz._, Scalaz._
import scalaz.std.scalaFuture

object TwitterController extends Controller {

    def getTimeline = Action.async { implicit request =>
        Mongo.findUserTwitter { twitter =>
            twitter.map(_.getUserTimeline()) match {
                case None => BadRequest
                case Some(tweets) => Ok(tweets.mkString("\n\n"))
            }
        }
    }

    def getProfile = Action.async { implicit request =>
        Mongo.findUserAndM(_.account.map(account => Ok(account.getName())))
    }

    def getRecord = Action.async { implicit request =>
        for {
            userOpt <- Mongo.findUserWithRequest
        }
        yield userOpt match {
            case Some(user) => Ok(user.records.toString())
            case None => NotFound("ユーザーが見つかりません。")
        }
    }
}