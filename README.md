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
* acks = this params controls how many partitions replicas must received the record before the producer can consider the write successful.
	* acks = 0  means that the producer will not wait for a reply from broker, assuming that the message was sent with successfully. If something went wrong so the producer will not know about it and the message will be lost. (_option used to gain high throughput_)
	* acks = 1 the producer will receive a success response from broker the moment the leader replica received the message.
	* acks = all the producer will receive a success response from the broker once all in-sync replicas received the message (_the safest mode_).
* buffer.memory = This sets the amount of memory the producer will use to buffer messages waiting to be sent to brokers.
* compression.type = Byt default the data is uncompression. Normally used to send message where bandwidth is more restricted. By enabling compressiong, you reduce network utilization and storage, wich is often a bottleneck when sending messages to kafka (_google created Gzip_)
* retries = when the producer receives an error message from the server, the error could be transient (e.g., a lack of leader for a partition).  This params will control how many times the producer will retry sending the message before giving up and notifying the client of an issue.
* batch.size = when multiple records are sent to same partition, the producer will batch them together. This params controls the amount of memory in bytes (_not messages_) that will be used for each batch. When the batch is full, all messages in the batch will be sent. However, this does not mean that the producer will waiti for the batch to become full (_obs: setting the batch size too small will add some overhead because the producer will need to send messages more frequently_).
* linger.ms = controls the amount of time to wait for a additional message before sending the current batch. KafkaProducer sends a batch of message either when the current batch is full or when the linger.ms is reached. (_By setting linger.ms higher than 0, we instruct the producer to wait a few milliseconds to add addtional messages to the batch before sending it to the brokers. This increases latency and throughput_)
* client.id = this can be any string, and will be used by the broker to identify messages sent from the client. It is used in logging and metrics and for quotas.
* max.in.flight.requests.per.connection = this controls how many messages the producer will send to the server without receiving responses (_Setting this high so increase memory usage and improve throughtput. But too high, reduce throughtput as batching comes less efficient_)
* timeout.ms, request.timeout.ms, and metadata.fetch.timeout.ms = these parameters control how long the producer will waitin for a reply from the server when sending data and request metadata
* max.blocks.ms = Controls how long the producer will block when calling send() and when explicitly requesting metadatavia partitionsFor().
* max.request.size = this setting controls the size of a produce request sent by the producer.
* receive.buffer.bytes and send.buffer.bytes = these are the sizes of the TCP send and receive buffers used by the sockets when writing and reading data. (_It is a good idea to increase those whe producers or consumers communicate with brokers in different datacenter because those network links typically have higher latency and lower bandwidth_)
