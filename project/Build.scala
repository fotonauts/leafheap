import sbt._
import sbt.Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

import spray.revolver.RevolverPlugin._

object MyBuild extends Build
{
        val LeafHeap = Project("LeafHeap", file("."),
            settings = Defaults.defaultSettings ++ assemblySettings ++ Revolver.settings ++
                net.virtualvoid.sbt.graph.Plugin.graphSettings ++
                Seq(
                    resolvers ++= Seq( "gphat" at "https://raw.github.com/gphat/mvn-repo/master/releases/"),

                    libraryDependencies ++= Seq(
                        "redis.clients"                   % "jedis"                      % "1.5.2",
                        "joda-time"                       % "joda-time"                  % "2.3",
                        "org.joda"                        % "joda-convert"               % "1.2",
                        "com.fasterxml.jackson.core"      % "jackson-core"               % "2.3.0",
                        "com.fasterxml.jackson.core"      % "jackson-databind"           % "2.3.0",
                        "com.fasterxml.jackson.module"    % "jackson-module-scala_2.10"  % "2.3.0",
                        "wabisabi"                        %% "wabisabi"                  % "2.0.8",
                        "ch.qos.logback"                  % "logback-classic"            % "0.9.29",
                        "args4j"                          % "args4j"                     % "2.0.12"
                    ),

                    scalaVersion := "2.10.0",
                    scalacOptions ++= Seq("-Xfatal-warnings",  "-deprecation", "-feature"),

                    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
                            case "about.html"     => MergeStrategy.discard
                            case PathList("org", "apache", "lucene", _*) => MergeStrategy.first
                            case x => old(x)
                        }
                    }
                )
        )
}
