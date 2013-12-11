leafheap
========

Logstash compatible feeder (redis to Elasticsearch). Tailored for our usage at fotopedia.

It was born after we found out about Kibana and wanted a more ligthweight approach than using Logstash. It's a followup of [logstash_light](https://github.com/fotonauts/logstash_light) and [logstash_benchmark](https://github.com/octplane/logstash_benchmark).

This specific implementation reads one or several redis lists and injects them in an ElasticSearch. It runs forever.

Build
=====

The build system uses sbt.

```
./sbt assembly
```

Building
========

Assuming you have a working Java 1.7 setup:

```
./sbt assembly
```

Usage
=====

```
java -jar target/scala-2.10/LeafHeap-assembly-0.1-SNAPSHOT.jar --help
 --help        : Print help
 --logback VAL : overrides -d
 -d            : run with debug logging parameters
 -e VAL        : ElasticSearch HTTP Url
 -l VAL        : Redis Queues, comma separated list
 -p N          : Redis port
 -r VAL        : Redis hostname
```

```
java -jar LeafHeap-assembly-0.1-SNAPSHOT.jar \
 -e http://es.server.internal:9201/ \
 -l master,staging,testing \
 -r redis.server.internal
```

Credits
=======

From Fotonauts:

- Pierre Baillet @octplane.
- kali


Copyright (c) 2013 Fotonauts released under the MIT license
