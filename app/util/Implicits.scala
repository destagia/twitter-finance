package miyatin.util

import scala.concurrent._
import java.util.concurrent.Executors
import scala.language.implicitConversions

import java.util.{Date, Calendar, TimeZone}

object Implicits {
    implicit def stringToCharSequence(s: String): CharSequence = s.asInstanceOf[CharSequence]

    private val pool = Executors.newCachedThreadPool()
    implicit val executionContext = ExecutionContext.fromExecutorService(pool)

    implicit class DateExtension(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"))

        def toIntDate: Int = {
            calendar.get(Calendar.YEAR) * 10000 +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)
        }
    }

    implicit class DateTupleExtension(dt: (Int, Int, Int)) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, dt._1)
        calendar.set(Calendar.MONTH, dt._2 - 1)
        calendar.set(Calendar.DAY_OF_MONTH, dt._3)
        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"))

        def toDate: Date = calendar.getTime
    }
}