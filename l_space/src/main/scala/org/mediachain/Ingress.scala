package org.mediachain

import cats.data.Xor
import org.mediachain.Types._
import org.mediachain.GraphError._
import gremlin.scala._

object Ingress {
  import Traversals.{GremlinScalaImplicits, VertexImplicits}

  def attachRawMetadata(blobV: Vertex, raw: RawMetadataBlob):
  Xor[TooManyRawBlobsError, Unit] = {
    val graph = blobV.graph

    // only allow one TranslatedFrom edge from each blob vertex
    if (blobV.lift.findRawMetadataXor.isLeft) {
      // add the raw metadata to the graph if it doesn't already exist
      val rawV = Traversals.rawMetadataBlobsWithExactMatch(graph.V, raw)
        .headOption
        .getOrElse(graph + raw)

      blobV --- TranslatedFrom --> rawV
      Xor.right({})
    } else {
      Xor.left(TooManyRawBlobsError(blobV))
    }
  }

  def defineAuthorship(blobV: Vertex, authorCanonical: Canonical):
  Xor[AuthorNotFoundError, Unit] = {
    authorCanonical.vertex(blobV.graph).map { authorCanonicalV =>
      val existingAuthor = Traversals.getAuthor(blobV.lift).headOption

      if (!existingAuthor.contains(authorCanonicalV)) {
        blobV --- AuthoredBy --> authorCanonicalV
      }

      Xor.right({})
    }.getOrElse {
      Xor.left(AuthorNotFoundError(blobV))
    }
  }

  // throws?
  def addPerson(graph: Graph,
                author: Person,
                raw: Option[RawMetadataBlob] = None):
  Xor[TooManyRawBlobsError, Canonical] = {
    // If there's an exact match already, return it,
    // otherwise create a new Person vertex and canonical
    // and return the canonical
    val q = Traversals.personBlobsWithExactMatch(graph.V, author)

    val personV: Vertex = q.headOption.getOrElse(graph + author)

    for {
      _ <- raw.map(attachRawMetadata(personV, _)).getOrElse(Xor.right({}))
    } yield {
      graph.V(personV.id)
        .findCanonicalXor
        .getOrElse {
          val canonicalV = graph + Canonical.create()
          canonicalV --- DescribedBy --> personV
          canonicalV.toCC[Canonical]
        }
    }
  }

  def addPhotoBlob(graph: Graph,
                   photo: PhotoBlob,
                   raw: Option[RawMetadataBlob] = None):
  Xor[GraphError, Canonical] = {
    // extract author & add if they don't exist in the graph already
    val author = photo.author.map { p =>
      addPerson(graph, p, raw)
    }

    // check to see if a duplicate entry exists
    val photoV = Traversals.photoBlobsWithExactMatch(graph.V, photo)
        .headOption.getOrElse(graph + photo)

    for {
      _ <- raw
        .map(attachRawMetadata(photoV, _))
        .getOrElse(Xor.right({}))
      _ <- author
        .map(x => x.flatMap(defineAuthorship(photoV, _)))
        .getOrElse(Xor.right({}))
    } yield {
      // return existing canonical for photo vertex, or create one
      graph.V(photoV.id)
        .findCanonicalXor
        .getOrElse {
          val canonicalVertex = graph + Canonical.create
          canonicalVertex --- DescribedBy --> photoV
          canonicalVertex.toCC[Canonical]
        }
    }
  }


  def modifyPhotoBlob(graph: Graph, parentVertex: Vertex, photo: PhotoBlob):
  Xor[GraphError, Canonical] = {
    Traversals.photoBlobsWithExactMatch(graph.V, photo)
      .findCanonicalXor
      .map(Xor.right)
      .getOrElse {
        val childVertex = graph + photo
        parentVertex --- ModifiedBy --> childVertex

        // TODO: don't swallow errors
        for {
          author         <- photo.author.flatMap(addPerson(graph, _).toOption)
          existingAuthor <- Traversals.getAuthor(childVertex.lift)
            .toCC[Canonical].headOption
          if author.canonicalID != existingAuthor.canonicalID
        } yield defineAuthorship(childVertex, author)

        childVertex.lift.findCanonicalXor
          .map(Xor.right)
          .getOrElse(Xor.left(CanonicalNotFound()))
      }
  }
}

