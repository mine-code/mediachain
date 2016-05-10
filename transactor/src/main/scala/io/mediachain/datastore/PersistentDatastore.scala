package io.mediachain.datastore

import java.util.Random
import java.util.concurrent.LinkedBlockingDeque
import com.amazonaws.AmazonServiceException
import io.mediachain.multihash.MultiHash

class PersistentDatastore(config: PersistentDatastore.Config)
  extends BinaryDatastore with AutoCloseable with Runnable {
  
  val dynamo = new DynamoDatastore(config.dynamo)
  val rocks = new RocksDatastore(config.rocks)
  val queue = new LinkedBlockingDeque[MultiHash]
  val writer = new Thread(this)
  val random = new Random
  val maxBackoffRetry = 60 // seconds

  override def putData(key: MultiHash, value: Array[Byte]) {
    rocks.putData(key, value)
    queue.putLast(key)
  }

  override def getData(key: MultiHash): Option[Array[Byte]] = {
    rocks.getData(key).orElse {dynamo.getData(key)}
  }
  
  override def close() {
    writer.interrupt()
    writer.join()
    rocks.close()
    dynamo.close()
  }
  
  // background writer
  override def run() {
    recover()
    loop(0)
  }
  
  private def recover() {
    // Crash recovery: schedule all keys still in rocks db for
    //  write-through to dynamo
    rocks.getKeys.foreach {key => queue.putLast(key)}
  }
  
  private def loop(backoff: Int) {
    if (!Thread.interrupted) {
      writeNext() match {
        case Some(key) => {
          rocks.removeData(key)
          loop(0)
        }
          
        case None => {
          val xbackoff = Math.min(maxBackoffRetry, Math.max(1, 2 * backoff))
          val sleep = random.nextInt(1000 * xbackoff)
          // XXX log sleep?
          Thread.sleep(sleep)
          loop(xbackoff)
        }
      }
    }
  }
  
  private def writeNext(): Option[MultiHash] = {
    val key = queue.takeFirst
    try {
      rocks.getData(key).map { data =>
        dynamo.putData(key, data)
        key
      }
    } catch {
      case e: AmazonServiceException => {
        // XXX log exception
        queue.addFirst(key)
        None
      }
    }
  }
}

object PersistentDatastore {
  case class Config(dynamo: DynamoDatastore.Config, rocks: String)
}
