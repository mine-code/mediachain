package org.mediachain

import shapeless.HList


object Traversals {
  import gremlin.scala._
  import Types._

  def canonicalWithID(q: GremlinScala[Vertex, _], canonicalID: String) = {
    q.hasLabel[Canonical]
      .has(Canonical.Keys.canonicalID, canonicalID)
  }

  def personWithExactMatch(q: GremlinScala[Vertex, _], p: Person) = {
    q.hasLabel[Person]
      .has(Person.Keys.name, p.name)
  }

  def photoBlobWithExactMatch(q: GremlinScala[Vertex, _], blob: PhotoBlob) = {
    q.hasLabel[PhotoBlob]
      .has(PhotoBlob.Keys.title, blob.title)
      .has(PhotoBlob.Keys.description, blob.description)
      .has(PhotoBlob.Keys.date, blob.date)
  }

  def rawMetadataWithExactMatch(q: GremlinScala[Vertex, _], raw: RawMetadataBlob) = {
    q.hasLabel[RawMetadataBlob]
      .has(RawMetadataBlob.Keys.blob, raw.blob)
  }


  def getCanonical(v: Vertex) = {
    v.lift
      .untilWithTraverser(t => t.get().in(DescribedBy).exists)
      .repeat(_.in(ModifiedBy))
      .in(DescribedBy)
  }

  def getAuthor(v: Vertex) = {
    v.lift
      .untilWithTraverser { t =>
        t.get().out(AuthoredBy).exists() || t.get().in().notExists()
      }
      .repeat(_.in(ModifiedBy))
      .out(AuthoredBy)
  }

  def getRootRevision(v: Vertex) = {
    v.lift
      .untilWithTraverser(t => t.get().in(DescribedBy).exists)
      .repeat(_.in(ModifiedBy))
      .in(DescribedBy)
  }

  def getRawMetadataForBlob(v: Vertex) = {
    v.out(TranslatedFrom)
  }

  implicit class VertexImplicits(v: Vertex) {
    /**
      * 'lift' a Vertex into a GremlinScala[Vertex, _] pipeline
      * @return a query pipeline based on the vertex
      */
    def lift: GremlinScala[Vertex, _] = v.graph.V(v.id)
  }

  implicit class GremlinScalaImplicits(gs: GremlinScala[Vertex, _]) {
    def canonicalOption: Option[Canonical] = {
      gs.flatMap(getCanonical)
        .toCC[Canonical]
        .headOption
    }

    def authorOption: Option[Canonical] = {
      gs.flatMap(getAuthor)
        .toCC[Canonical]
        .headOption
    }

    def rawMetadataOption: Option[RawMetadataBlob] = {
      gs.flatMap(getRawMetadataForBlob)
        .toCC[RawMetadataBlob]
        .headOption
    }
  }
}
