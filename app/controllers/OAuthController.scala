package controllers

import play.api.mvc._
import play.api.libs.json.Json
import twitter4j.{User => TwitterUser, TwitterFactory, Twitter}
import twitter4j.auth._
import play.api.cache._
import play.api.Play.current
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConversions._
import miyatin.util._
import miyatin.util.model._
import reactivemongo.core.commands.LastError

object OAuthController extends Controller {

    def twitterLogin = Action { implicit request =>
        println("login..")
        val twitter: Twitter = (new TwitterFactory()).getInstance()
        val hash: String = Util.getUniqueID()
        val requestToken: RequestToken = twitter.getOAuthRequestToken("http://" + request.host + "/twitter/callback/" + hash)
        println("get token...")
        Cache.set("twitter_" + hash, twitter, 3600 * 34 * 7)
        Cache.set("requestToken_" + hash, requestToken, 120)
        println("redirect to authorized url")
        Redirect(requestToken.getAuthorizationURL())
    }

    def twitterOAuthCallback(hash: String) = Action.async { implicit request =>
        println("callback...")
        val result: Option[Future[User]] = for {
            twitter <- Cache.getAs[Twitter]("twitter_" + hash)
            requestToken <- Cache.getAs[RequestToken]("requestToken_" + hash)
            authTokenSeq <- request.queryString.get("oauth_token")
            authVerifierSeq <- request.queryString.get("oauth_verifier")
            authToken <- authTokenSeq.headOption
            authVerifier <- authVerifierSeq.headOption
        }
        yield {
            println("clear twitter...")
            Cache.remove("requestToken_" + hash)
            for {
                accessToken <- Future(twitter.getOAuthAccessToken(requestToken, authVerifier))
                (accessTokenSt, accessTokenSecretSt) <- Future {
                    (accessToken.getToken, accessToken.getTokenSecret)
                }
                userOpt <- Mongo.findUser(twitter.getId())
                user <- userOpt match {
                    case None =>
                        val paging = new twitter4j.Paging(1, 1)
                        val user = User(
                            hash,
                            twitter.getId(),
                            accessTokenSt,
                            accessTokenSecretSt,
                            twitter.getUserTimeline(paging).headOption.map(_.getId()),
                            Nil,
                            UserSettings.default(hash)
                        )
                        Mongo.insert(user).map(_ => user)

                    case Some(user) => {
                        Cache.remove("twitter_" + hash)
                        Future(user)
                    }
                }
            }
            yield user
        }
        println("redirect...")
        result match {
            case Some(f) =>
                f.map { user =>
                    println("go...")
                    Redirect(routes.Application.index)
                    .withCookies(Cookie("id", user.id, Some(3600 * 24 * 7)))
                }
            case None => Future {
                println("faild...")
                Redirect(routes.Application.index)
            }
        }
    }

}