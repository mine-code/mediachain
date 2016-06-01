# mediachain-transactor

Consensus/journal cluster server for the mediachain network. To run a test node:

* `brew install dynamodb-local && brew services start dynamodb-local`
* Prep DynamoDB tables
```scala
import com.amazonaws.auth.BasicAWSCredentials
val awscreds = new BasicAWSCredentials($AWSACCESS, $AWSSECRET)
​
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import scala.collection.JavaConversions._
import com.amazonaws.services.dynamodbv2.model._
​
val dynamo = new AmazonDynamoDBClient(awscreds)
dynamo.setEndpoint("http://localhost:8000")
​
val mainTableAttrs = List(new AttributeDefinition("multihash", "S"))
val mainTableSchema = List(new KeySchemaElement("multihash", KeyType.HASH))
val pth = new ProvisionedThroughput(10, 10)
dynamo.createTable(mainTableAttrs, "Test", mainTableSchema, pth)
​
val chunkTableAttrs = List(new AttributeDefinition("chunkId", "S"))
val chunkTableSchema = List(new KeySchemaElement("chunkId", KeyType.HASH))
dynamo.createTable(chunkTableAttrs, "TestChunks", chunkTableSchema, pth)
```
* Create config file
```
io.mediachain.transactor.server.rootdir: /path/to/transactor-directory
io.mediachain.transactor.server.address: 127.0.0.1:10000
io.mediachain.transactor.dynamo.awscreds.access: $AWSACCESS
io.mediachain.transactor.dynamo.awscreds.secret: $AWSSECRET
io.mediachain.transactor.dynamo.baseTable: Test
io.mediachain.transactor.dynamo.endpoint: http://localhost:8000
```

If you want to enable SSL, add the following to the config file:
```
io.mediachain.transactor.ssl.enabled: true
io.mediachain.transactor.ssl.keyStorePath: /path/to/keystore.jks
io.mediachain.transactor.ssl.keyStorePassword: $KEYSTOREPASS
io.mediachain.transactor.ssl.keyStoreKeyPassword: $KEYSTOREKEYPASS
```

* `sbt transactor/assembly`
* `scala -cp $BIG_JAR_FROM_ABOVE io.mediachain.transactor.JournalServer path/to/config.conf`