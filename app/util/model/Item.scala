package miyatin.util.model

import reactivemongo.bson._
import collection.JavaConversions._
import twitter4j._

case class Item(
    id: String,
    parentTweetId: Long,
    price: Int,
    sentence: String,
    image: Option[String],
    date: Long
)
object Item {

    implicit val bsonWriter = new BSONDocumentWriter[Item] {
        def write(i: Item) = {
            BSONDocument(
                "id" -> i.id,
                "parentTweetId" -> i.parentTweetId,
                "price" -> i.price,
                "sentence" -> i.sentence,
                "image" -> i.image,
                "date" -> i.date
            )
        }
    }

    implicit val bsonReader = new BSONDocumentReader[Item] {
        def read(b: BSONDocument) = Item(
            b.getAs[String]("id").getOrElse("undefind"),
            b.getAs[Long]("parentTweetId").getOrElse(-1L),
            b.getAs[Int]("price").getOrElse(0),
            b.getAs[String]("sentence").getOrElse("undefind"),
            b.getAs[String]("image"),
            b.getAs[Long]("date").getOrElse(19700101)
        )
    }
}