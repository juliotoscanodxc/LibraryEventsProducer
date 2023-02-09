# LibraryEventsProducer
Demo of kafka using Spring boot

* CREATE A TOPIC 
``` BASH
kafka-topics --create --bootstrap-server broker1:19092 --replication-factor 1 --partitions 1 --topic test
```
* PRODUCE A MESSAGE
``` BASH
kafka-console-producer --broker-list broker1:19092 --topic test
```
* READ A MESSAGE ON THE TOPIC
``` bash
kafka-console-consumer --bootstrap-server broker1:19092 --topic test --from-beginning
```

## General Broker properties

* broker.id = Define a broker ID to be more easy to identify the broker in a cluster.
* port = Define a comunication port with the broker.
* zookeeper.connect = connection witht he zookeeper ensemble.
* log.dirs = directory where kafka will store all logs.
* num.recovery.threads.per.data.dir = Number of threads configured for handling logs (for each directory).
* auto.create.topics.enable = configuratin specifies that the broker should automatically create a topic under the some circumstances.

## Topic properties
* num.partitions = number of partition per topic (default = 6 GB is satisfactory)
* log.retention.ms = the time for log retention. (default = 7 days)
* log.retention.bytes = size of logs by topic
* log.segment.bytes = size of log segment and not size of message (default = 1 GB)
* log.segment.ms = another way to control log segment by time
* message.max.bytes = maximum size of message (default = 1MB)

### How to select a hardware
* Disk throughput (impact directly in producer clients)
* Disk capacity (always considering the level of messages per day, and by default 7 days)
* Memory (is not recomended that kafka stay in other server sharing the cache memory)
* Networking
* CPU

## Producer properties
* bootstrap.servers
* key.serializer
* value.serializer

Follow a example:
``` JAVA
public KafkaProducer<String, String> kafkaTemplate() {
	Properties props = new Properties();
	
	props.put("bootstrap-servers", "broker1:9092, broker2:9092");
	
	props.put("key.serializer", org.apache.kafka.common.serialization.StringSerializer);
	
	props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer);
	
	return new KafkaTemplate<String, String> (props);
}
```

Once we instantiate a producer, it is time to start sending messages. There are three primary methods of sending message: Fire-and-forget, Synchronous send and Asynchronous send.

### Sending a message
``` JAVA
ProducerRecord<String, String> record = new ProducerRecord<>("CustomerCountry", "Precision Products", "France");
try {
	producer.send(record);
} catch (Exception e) {
	e.printStackTrace();
}
```
### Sending a message synchronously
``` JAVA
ProducerRecord<String, String> record = new ProducerRecord<>("CustomerCountry", "Precision Products", "France");
try {
	producer.send(record).get();
} catch (Exception e) {
	e.printStackTrace();
}
```
### Sending a message asynchronously
``` JAVA
private class DemoProducerCallback implements Callback {
	@Override
	public void onCompletation(RecordMetadata recordMetadata, Exception e) {
		if(e != null) {
			e.printStackTrace();
		}
	}
}

ProducerRecord<String, String> record = new ProducerRecord<>("CustomerCountry", "Precision Products", "France");

producer.send(record, new DemoProducerCallback());
```
### Configuring Producers
* **acks** = this params controls how many partitions replicas must received the record before the producer can consider the write successful.
	* acks = 0  means that the producer will not wait for a reply from broker, assuming that the message was sent with successfully. If something went wrong so the producer will not know about it and the message will be lost. (_option used to gain high throughput_)
	* acks = 1 the producer will receive a success response from broker the moment the leader replica received the message.
	* acks = all the producer will receive a success response from the broker once all in-sync replicas received the message (_the safest mode_).
* **buffer.memory** = This sets the amount of memory the producer will use to buffer messages waiting to be sent to brokers.
* **compression.type** = Byt default the data is uncompression. Normally used to send message where bandwidth is more restricted. By enabling compressiong, you reduce network utilization and storage, wich is often a bottleneck when sending messages to kafka (_google created Gzip_)
* **retries** = when the producer receives an error message from the server, the error could be transient (e.g., a lack of leader for a partition).  This params will control how many times the producer will retry sending the message before giving up and notifying the client of an issue.
* **batch.size** = when multiple records are sent to same partition, the producer will batch them together. This params controls the amount of memory in bytes (_not messages_) that will be used for each batch. When the batch is full, all messages in the batch will be sent. However, this does not mean that the producer will waiti for the batch to become full (_obs: setting the batch size too small will add some overhead because the producer will need to send messages more frequently_).
* **linger.ms** = controls the amount of time to wait for a additional message before sending the current batch. KafkaProducer sends a batch of message either when the current batch is full or when the linger.ms is reached. (_By setting linger.ms higher than 0, we instruct the producer to wait a few milliseconds to add addtional messages to the batch before sending it to the brokers. This increases latency and throughput_)
* **client.id** = this can be any string, and will be used by the broker to identify messages sent from the client. It is used in logging and metrics and for quotas.
* **max.in.flight.requests.per.connection** = this controls how many messages the producer will send to the server without receiving responses (_Setting this high so increase memory usage and improve throughtput. But too high, reduce throughtput as batching comes less efficient_)
* **timeout.ms, request.timeout.ms, and metadata.fetch.timeout.ms** = these parameters control how long the producer will waitin for a reply from the server when sending data and request metadata
* **max.blocks.ms** = Controls how long the producer will block when calling send() and when explicitly requesting metadatavia partitionsFor().
* **max.request.size** = this setting controls the size of a produce request sent by the producer.
* **receive.buffer.bytes and send.buffer.bytes** = these are the sizes of the TCP send and receive buffers used by the sockets when writing and reading data. (_It is a good idea to increase those whe producers or consumers communicate with brokers in different datacenter because those network links typically have higher latency and lower bandwidth_)
* **retry.backoff.ms** - The amount of time to wait before attempting to retry a failed request to a given topic partition. This avoids repeatedly sending requests in a tight loop under some failure scenarios.
## Schema registry
A place responsible to store the schema records about the entities to be serialized or unserialized.
``` JAVA
Properties props = new Properties();
props.put("bootstrap-servers", "localhost:9092");
props.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
props.put("schema.registry.url", schemaUrl);

String topic = "customerContacts";
int wait = 500;

Producer<String, Customer> producer = new KafkaProducer<>(props);
Customer customer = CustomerGenerator.getNext();
ProducerRecord<String, Customer> record = new ProducerRecord<>(topic, customer.getId, customer);

producer.send(record);
```

Or you can generate your own schema:
``` JAVA
String schemaString = "{\"namespace\": \"customerManagement.avro\", \"type\": \"record\", " + "\"name\": \"Customer\"," + "\"fields\": [" + "{\"name\": \"id\", \"type\": \"int\"}," + "{\"name\": \"name\", \"type\": \"string\"}," + "{\"name\": \"email\", \"type\": [\"null\",\"string \"], \"default\":\"null\" }" + "]}";

Schema.Parser parser = new Schema.Parser();
Schema schema = parser.parse(schemaString);

GenericRecord genericRecord = new GenericData.Record(schema);
genericRecord.put("id", customer.getId());
genericRecord.put("name", name);
genericRecord.put("email", email);

ProducerRecord<String, GenericRecord> producerRecord = new ProducerRecord<>(topic, name, genericRecord);

Producer<String, GenericRecord> producer = new KafkaProducer<>(props);

producer.send(producerRecord);

```
## Partition

Always when you create a producer, you can select a specific partition to send the message. Consequently, we will have the same client consuming that. But if the key is equals to null, so the algorithm round-robin will be used to select the best partition. When we provide a key, normaly we use the hash algorithm to select the partition (_but it only happen if you don't specify a partition for that record_).

## Kafka Consumer

How to create a kafka consumer:
``` Java
Properties props = new Properties();
props.put("bootstrap.servers", "kafka1:19092:kafka:19093");
props.put("group.id", "CountryCustomer");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
```
Subscribe to a group:
``` JAVA
consumer.subscribe(Collections.singletonList("customerCountries"));
```
### Configuring consumers
* fetch.min.bytes = this property allows a consumer to specify the minimum amount of data that it wants to received from the broker when fetching records (_this reduce load on both the consumer and the broker as they have to handler fewer back-and-forth_).
* fetch.max.wait.ms = Control how long to wait to get some data.
* max.partition.fetch.bytes = The max amount returned by the partition (_default is 1 MB_)
* session.timeout.ms = the amount of time a consumer can be out of contact with the brokers while still considered alive defaults to 3 seconds.
* auto.offset.reset = this property controls the behavior of the consumer when it starts reading a parttion for which it doesn't have a committed offset or if the commited offset it has is invalid (_default is latest and the other alternative is newest_)
* enable.auto.commit = this paramater controls wheter the consumer will commit offsets automatically, and defaults to true (_if you set false, you consider the consumer treat this behavior manually_).
* partition.assignment.strategy = You select which parttion assignor you want to use in case of decide the partition to direct the message (_we also can implement our own strategy_).
	* range = Assigns to each consumer a consecutive subset of partitions from each topic it subscribes to.
	* roundRobin = Takes all the partitions from all subscribed topics and assigns them to consumer senquentially, one by one.
* client.id = this can be any string to identify the client in brokers. Its important to logs and metrics.
* max.poll.records = The maximum of records that a single call to poll() will return.
* receive.buffer.bytes and send.buffer.bytes = These are the size of the TCP send and receive buffers used by the sockets when writing and reading data.

### Commits and Offsets

We call the action of updating the current position in the partition a commit.

Offset is a position in partition.

* automatic commit -> enable.auto.commit=true
* auto.commit.interval.ms = the time used to consumer commit the largest offset your client received from poll() (_default is 5 seconds_).
  Allow the use of automatic commit offset is more easy because the kafka will treat the behavior to identify in which position the application need continue reading the message. however, there is a risk in read twice the messages or lost some message due the rebalance of the consumer group. To avoid that, some developers prefer to keep the control in your hand to not depend of the commit interval. commitSync() is the method more reliable in case of auto.commit.offset=false.
  The commitSync() allow you to sync with the commit offset present in kafka. Pay attention to have sure that you processed the lastest messages, because exist the risk of you are missing to process some message.
  The best option is to combine, commitAsync() with commitSync():
```JAVA
try {
	while(true) {
		ConsumerRecords<String, String> records = consumer.poll(100);
		for(ConsumerRecord record : records) {
			// process this
		}
		consumer.commitAsync();
	}
} catch (Exception e) {
	log.error("Unexpected error: {}", e);
} finally {
	try {
		consumer.commitSync();
	} finally {
		consumer.close();
	}
}
```

if you are in the middle of processing huge batches, so you can do:

``` JAVA
private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
int count = 0;
...
while (true) {
	ConsumerRecords<String, String> consumerRecords = consumer.poll(100);
	for (ConsumerRecord<String, String> record : consumerRecords) {
		// process this
		currentOffsets.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset()+1, "no metadata"));
		if (count % 1000 == 0) {
			consumer.commitAsync(currentOffsets, null);
		}
		count++;
	}
}
```

to exit from the infinite loop we only need create another thread and call the poll() using consumer.wakeup(). With this, the pool() will cause the WakeupException.
If we want to point for a service and ignore if there is a consumer group or not, and we know the partition, you can do:

``` JAVA
List<PartitionInfo> partitionInfos = consumer.partitionsFor("topic");

if ( partitionInfos != null) {
	for (PartitionInfo partition : partitionInfos) {
		partition.add(new TopicPartition(partition.topic(), partition.partition()));
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(1000);
			for (ConsumerRecord record : records) {
				// process this
			}
			consumer.commitSync();
		}
	}
}
```