package miyatin.util

import java.util.{Calendar, Date}

import miyatin.util.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import java.util.concurrent.TimeUnit

/*
    収支情報を記録するためのクローラーオブジェクト
*/
object Recorder {

    def run() = Future {

        // 1日に一回記録作業を行う。
        // TimeUnit.DAYS.sleep(1)
        // デバッグ用に1分に一回記録作業を行う。
        var recorded = false;
        while (true) {
            TimeUnit.MINUTES.sleep(1)
            val calendar = Util.getCurrentDate
            if (recorded ^ calendar.get(Calendar.HOUR_OF_DAY) == 0) {
                // 全ユーザーを取得，recordを開始。
                val record = for {
                    users <- Mongo.findUsers
                    _ <- Future(println("[Twifi] User count : " + users.size))
                    _ <- Future.sequence(users.map(_.runRecord.recover {
                        case e: Exception => e.printStackTrace()
                    }))
                }
                yield ()

                println("[Twifi] Recording...")
                Await.result(record, Duration.Inf)
                println("[Twifi] Record.")
                recorded = true
            }
            recorded = calendar.get(Calendar.HOUR_OF_DAY) == 0
        }

    }
}