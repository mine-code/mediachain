package io.mediachain.transactor

import io.mediachain.BaseSpec
import org.specs2.matcher.Matcher

object TypeSerializationSpec extends BaseSpec {
  import io.mediachain.transactor.Types._
  import io.mediachain.transactor.TypeSerialization._
  import io.mediachain.util.cbor.CborAST._
  import io.mediachain.transactor.Dummies.DummyReference

  def is =
    s2"""
         - encodes the CBOR type name correctly $encodesTypeName

         round-trip converts to/from CBOR
          - entity $roundTripEntity
          - artefact $roundTripArtefact
          - entity chain cell $roundTripEntityChainCell
          - artefact chain cell $roundTripArtefactChainCell
          - entity chain reference $roundTripEntityChainRef
          - artefact chain reference $roundTripArtefactChainRef
          - canonical journal entry $roundTripCanonicalEntry
          - chain journal entry $roundTripChainEntry
          - journal block $roundTripJournalBlock
      """

  private object Fixtures {

    val entity = Entity(meta = Map("foo" -> CString("bar")))
    val artefact = Artefact(meta = Map("bar" -> CString("baz")))

    val entityChainCell = EntityChainCell(
      entity = new DummyReference(0),
      chain = None,
      meta = Map("created" -> CString("the past"))
    )

    val artefactChainCell = ArtefactChainCell(
      artefact = new DummyReference(1),
      chain = None,
       meta = Map("created" -> CString("the past"))
    )

    val entityChainRef = EntityChainReference(
      chain = Some(new DummyReference(2))
    )

    val artefactChainRef = ArtefactChainReference(
      chain = Some(new DummyReference(3))
    )

    val canonicalEntry = CanonicalEntry(
      index = 42,
      ref = new DummyReference(0)
    )

    val chainEntry = ChainEntry(
      index = 43,
      ref = new DummyReference(0),
      chain = new DummyReference(2),
      chainPrevious = None
    )

    val journalBlock = JournalBlock(
      index = 44,
      chain = Some(new DummyReference(3)),
      entries = Array(canonicalEntry, chainEntry)
    )
  }

  def matchTypeName(typeName: String): Matcher[CValue] =
    beLike {
      case m: CMap =>
        m.asStringKeyedMap must havePair ("type" -> CString(typeName))
    }

  def encodesTypeName = {
    Fixtures.entity.toCbor must matchTypeName(CBORTypeNames.Entity)
    Fixtures.artefact.toCbor must matchTypeName(CBORTypeNames.Artefact)
    Fixtures.entityChainCell.toCbor must matchTypeName(CBORTypeNames.EntityChainCell)
    Fixtures.artefactChainCell.toCbor must matchTypeName(CBORTypeNames.ArtefactChainCell)
    Fixtures.entityChainRef.toCbor must matchTypeName(CBORTypeNames.EntityChainReference)
    Fixtures.artefactChainRef.toCbor must matchTypeName(CBORTypeNames.ArtefactChainReference)
    Fixtures.canonicalEntry.toCbor must matchTypeName(CBORTypeNames.CanonicalEntry)
    Fixtures.chainEntry.toCbor must matchTypeName(CBORTypeNames.ChainEntry)
    Fixtures.journalBlock.toCbor must matchTypeName(CBORTypeNames.JournalBlock)
  }

  def roundTripEntity =
    fromCbor(Fixtures.entity.toCbor) must beRightXor { entity =>
      entity.asInstanceOf[Entity].meta must havePair("foo" -> CString("bar"))
    }

  def roundTripArtefact =
    fromCbor(Fixtures.artefact.toCbor) must beRightXor { entity =>
      entity.asInstanceOf[Artefact].meta must havePair("bar" -> CString("baz"))
    }

  def roundTripEntityChainCell =
    fromCbor(Fixtures.entityChainCell.toCbor) must beRightXor { cell =>
      cell.asInstanceOf[EntityChainCell] must_== Fixtures.entityChainCell
    }

  def roundTripArtefactChainCell =
    fromCbor(Fixtures.artefactChainCell.toCbor) must beRightXor { cell =>
      cell.asInstanceOf[ArtefactChainCell] must_== Fixtures.artefactChainCell
    }

  def roundTripEntityChainRef =
    fromCbor(Fixtures.entityChainRef.toCbor) must beRightXor { ref =>
      ref.asInstanceOf[EntityChainReference] must_== Fixtures.entityChainRef
    }

  def roundTripArtefactChainRef =
    fromCbor(Fixtures.artefactChainRef.toCbor) must beRightXor { ref =>
      ref.asInstanceOf[ArtefactChainReference] must_== Fixtures.artefactChainRef
    }

  def roundTripCanonicalEntry =
    fromCbor(Fixtures.canonicalEntry.toCbor) must beRightXor { cell =>
      cell.asInstanceOf[CanonicalEntry] must_== Fixtures.canonicalEntry
    }

  def roundTripChainEntry =
    fromCbor(Fixtures.chainEntry.toCbor) must beRightXor { cell =>
      cell.asInstanceOf[ChainEntry] must_== Fixtures.chainEntry
    }

  def roundTripJournalBlock =
    fromCbor(Fixtures.journalBlock.toCbor) must beRightXor { cell =>
      val expected = Fixtures.journalBlock

      cell must beLike {
        case block: JournalBlock => {
          block.index must_== expected.index

          block.chain must_== expected.chain

          block.entries.toList must containTheSameElementsAs(expected.entries.toList)
        }
      }
    }
}