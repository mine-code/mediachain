package org.mediachain.translation.tate


object TateTranslator {

  import cats.data.Xor
  import org.mediachain.translation.TranslationError, TranslationError.InvalidFormatError
  import org.mediachain.Types.{Person, PhotoBlob}

  import org.json4s._
  implicit val formats = org.json4s.DefaultFormats


  case class Contributor(fc: String, role: String)
  case class Artwork(title: String,
                     medium: Option[String],
                     dateText: Option[String],
                     contributors: List[Contributor])


  def loadArtwork(obj: JObject): Xor[TranslationError,  (PhotoBlob, List[Person])] = {
    val artwork = obj.extractOpt[Artwork]
    val result = artwork.map { a =>

      val artists = for {
        c <- a.contributors
        if c.role == "artist"
      } yield Person(None, c.fc)

      val blob = PhotoBlob(None,
        a.title,
        a.medium.getOrElse(""),
        a.dateText.getOrElse(""),
        artists.headOption)
      (blob, artists.toList)
    }

    Xor.fromOption(result, InvalidFormatError())
  }

}
