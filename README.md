leafheap
========

Logstash compatible feeder (multi-redis to Elasticsearch). Tailored for our usage at fotopedia.

It reads one or several redis List and inject them in an ElasticSearch.

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
java -jar LeafHeap-assembly-0.1-SNAPSHOT.jar -e http://es.server.internal:9201/ -l master,staging,testing -r redis.server.internal
```

Credits
=======

From Fotonauts:

- Pierre Baillet @octplane.
- Kali


Copyright (c) 2013 Fotonauts released under the MIT license