package miyatin.util.model

import reactivemongo.bson._

case class Record (
    id: String,
    userId: String,
    date: Int, // ex. 20150122
    items: List[Item] // items bought at the date
) {
    lazy val sum = items.map(_.price).sum
    def year = (date / 10000).toInt
    def month = ((date - year) / 100).toInt
    def day = date - year - month
}
object Record {

    implicit val bsonWriter = new BSONDocumentWriter[Record] {
        def write(r: Record) = BSONDocument(
            "id" -> r.id,
            "userId" -> r.userId,
            "date" -> r.date,
            "items" -> r.items
        )
    }

    implicit val bsonReader = new BSONDocumentReader[Record] {
        def read(b: BSONDocument) = Record(
            b.getAs[String]("id").getOrElse("undefind"),
            b.getAs[String]("userId").getOrElse("undefind"),
            b.getAs[Int]("date").getOrElse(-1),
            b.getAs[List[Item]]("items").getOrElse(Nil)
        )
    }

}