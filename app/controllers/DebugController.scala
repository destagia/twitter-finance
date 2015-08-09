package controllers

import java.util.{Calendar, Date}

import play.api.mvc.{Controller, Action}
import twitter4j.{User => TwitterUser, TwitterFactory, Twitter}
import twitter4j.auth._
import play.api.cache._
import play.api.Play.current
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import miyatin.util._
import miyatin.util.model._
import scalaz._, Scalaz._
import Implicits._

object DebugController extends Controller {

    def item = Action(Ok(views.html.itemView(Item(
        "hogehoge",
        1L,
        999999,
        "とってもおいしいご飯",
        Some("http://pbs.twimg.com/media/CL885fxUsAA_r8e.jpg"),
        20140908
    ))))

}
