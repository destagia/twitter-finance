package controllers

import java.util.{Calendar, Date}

import play.api.mvc.{Controller, Action}
import twitter4j.{User => TwitterUser, TwitterFactory, Twitter}
import twitter4j.auth._
import play.api.cache._
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import scala.concurrent.duration._
import scala.concurrent.Future
import miyatin.util.Implicits._
import miyatin.util._
import miyatin.util.model._
import scalaz._, Scalaz._
import miyatin.util.Implicits._

object Application extends Controller {

    /*
        1日に一回，ユーザーの購入したものを集計する必要があるので
        クローラーを起動させる。
    */
    Recorder.run()

    def getCache(id: String): Option[Twitter] = {
        Cache.getAs[Twitter]("twitter_" + id)
    }

    def setCache(id:String, t: Twitter) {
        Cache.set("twitter_" + id, t, 3600 * 24 * 7)
    }

    def index = Action.async { implicit request =>
        Mongo.findUserAndM { user =>
            for {
                account <- user.account
            }
            yield {
                Ok(views.html.index(user, account))
            }
        }
    }

    case class SettingForm(timelineNotify: Boolean, hashTag: String)
    val settingForm = Form(mapping("timelineNotify" -> of[Boolean], "hashTag" -> of[String])(SettingForm.apply)(SettingForm.unapply))
    def updateSetting = Action.async{ implicit request =>
        val form = settingForm.bindFromRequest.get
        Mongo.findUserAndM(user => for {
            account <- user.account
            newUser = user.copy(settings = UserSettings(
                user.id, form.timelineNotify, if (form.hashTag == "") user.settings.hashTag else form.hashTag))
            _ <- Mongo.updateUser(newUser)
        } yield {
            Ok(views.html.index(newUser, account))
        })
    }

    def setting = Action.async { implicit request =>
        Mongo.findUserAndM { user =>
            for (account <- user.account) yield {
                Ok(views.html.setting(user, account, settingForm))
            }
        }
    }

    def items = {
        val c = Util.getCurrentDate
        itemsWithDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    def itemsWithDate(y: Int, m: Int, d: Int) = Action.async { implicit request =>
        Mongo.findUserAndM { user =>
            for {
                rOpt <- user.recordByDate((y, m, d).toDate)
                account <- user.account
                twitter <- user.twitter
            } yield {
                Ok(views.html.record(rOpt, Some(account), y, m, d))
            }
        } recover {
            case e: twitter4j.TwitterException => Unauthorized(views.html.unauthorized())
            case e: Exception => Unauthorized
        }
    }



}
