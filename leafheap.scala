package com.fotopedia.LeafHeap

import redis.clients.jedis.Jedis

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node._
import com.fasterxml.jackson.databind._

import wabisabi._

import scala.concurrent.duration._
import scala.concurrent.Await

import org.joda.time.Instant
import java.util.Calendar
import java.util.TimeZone
import java.util.GregorianCalendar
import java.lang.Thread

import org.kohsuke.args4j.{ CmdLineParser, CmdLineException, Option => Args4jOption }

import collection.JavaConversions._


object Settings {
    @Args4jOption(name = "--help", usage = "Print help")
    var showHelp = false

    @Args4jOption(name = "-r", usage = "Redis hostname")
    var redisHostname:String = null

    @Args4jOption(name = "-p", usage = "Redis port")
    var redisPort = 6379

    @Args4jOption(name = "-l", usage = "Redis Queues, comma separated list")
    var redisQueues = "prod,testing,staging,infrabox"


    @Args4jOption(name = "-e", usage = "ElasticSearch HTTP Url")
    var esUrl = "http://localhost:9201/"

    def es():Client = {
        return new Client(esUrl)
    }

    @Args4jOption(name = "-d", usage = "run with debug logging parameters")
    var debugMode = false

    @Args4jOption(name = "--logback", usage = "overrides -d")
    var logbackConfigFile:String = null

}

class QueueProcessor(queueName: String) extends Runnable {
    val queue = queueName
    def run() {
        System.out.println("Waking up QueueProcessor for " + queueName)
        LeafHeap.processQueue(queue)
    }
}


object LeafHeap {
    def timestamp_ms(logLine:JsonNode, from:String, to:String) {
        val fields = logLine.get("@fields")
        val date = fields.get(from)
        if (date != null) {
            val jInstant = new Instant(date.longValue)
            logLine.asInstanceOf[ObjectNode].set(to, new TextNode(jInstant.toString))
        }

    }
    def rename(logLine: JsonNode, from:String, to:String) {
        val fields = logLine.get("@fields")
        val source = fields.asInstanceOf[ObjectNode].remove(from)
        if(source != null) {
            logLine.asInstanceOf[ObjectNode].set(to, source)
        }
    }

    def processQueue(queueName: String) {
        val jedis = new Jedis(Settings.redisHostname, Settings.redisPort)

        val l = jedis.llen(queueName)
        val prefix = "[" + queueName + "] "

        System.out.println(prefix + "Queue has size:" + l )
        var count = 0
        var log_line =  jedis.lpop(queueName)

        var batch = scala.collection.mutable.ArrayBuffer[Object]()

        while(true) {
            try {
                if (log_line != null) {
                    // Process the data
                    val logLineObject = mapper.readTree(log_line)

                    timestamp_ms(logLineObject, "date", "@timestamp")
                    rename(logLineObject, "instance", "host")
                    logLineObject.asInstanceOf[ObjectNode].set("type", new TextNode(queueName))

                    count = count + 1
                    var indexName = String.format("logstash-%1$tY.%1$tm.%1$td.%1$tH", new GregorianCalendar)
                    batch += Map[String, Object]("index" -> Map[String, String]("_index" -> indexName, "_type" -> "logs"))
                    batch += logLineObject
                }
                // Next tick
                log_line = jedis.lpop(queueName)

                // If redis is empty, or we have reached our maximum capacity
                // ship the logs
                if (log_line == null || count == 1000) {
                    System.out.println(prefix + "Sending "+ count +" objects.")
                    val res = Await.result(Settings.es.bulk(data = (batch.map { v => mapper.writeValueAsString(v) }.mkString("\n"))+"\n"), Duration(8, "second")).getResponseBody
                    val responseObject = mapper.readTree(log_line)
                    System.out.println("took " + responseObject.get("took"))
                    count = 0
                    batch = scala.collection.mutable.ArrayBuffer[Object]()
                }

                // Move to next non-empty log line if needed
                while(log_line == null) {
                    Thread.sleep(500)
                    log_line = jedis.lpop(queueName)
                }
            } catch {
                case e:Throwable => {
                    System.out.println("Something wrong happened:")
                    System.out.println(log_line)
                    System.out.println(e.toString)
                    e.printStackTrace(System.out)
                    log_line = null
                    Thread.sleep(1000)
                }
            }
        }
    }

    val mapper = {
        val o = new ObjectMapper()
        o.registerModule(DefaultScalaModule)
        o
    }

    def main(args:Array[String]) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val parser = new CmdLineParser(Settings)
        try {
            parser.parseArgument(args:_*)
        } catch {
            case e:CmdLineException => {
                System.err.println(e)
                parser.printUsage(System.err)
                System.exit(-1)
            }
        }

        if (Settings.showHelp) {
            parser.printUsage(System.err)
            System.exit(0)
        }

        if (Settings.logbackConfigFile != null) {
            System.setProperty("logback.configurationFile",Settings.logbackConfigFile);
        } else if (System.getProperty("logback.configurationFile") == null) {
            System.setProperty("logback.configurationFile",
                if(Settings.debugMode) "logback.debug.xml" else "logback.xml"
                )
        }

        Settings.redisQueues.split(",").
        foreach{ queue =>
            val t = new Thread(new QueueProcessor(queue))
            t.setDaemon(false)
            t.start()
        }
    }
}
