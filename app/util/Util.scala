package miyatin.util

import java.util.{TimeZone, Calendar, Date}

import scala.util.Random

object Util {
    private lazy val random = new Random()
    private lazy val chars = ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')

    def getUniqueID(): String = {
        val list = for (i <- (0 to 20)) yield chars(random.nextInt(chars.length))
        list.foldRight("")(_ + _)
    }

    def getCurrentDate: Calendar = {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
        calendar.setTime(new Date())
        calendar
    }
}