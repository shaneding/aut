/*
 * Copyright © 2017 The Archives Unleashed Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.archivesunleashed.app

import java.io.File
import java.nio.file.{Files, Paths}
import org.apache.spark.sql.Row
import org.apache.spark.{SparkConf, SparkContext}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class WriteGEXFTest extends FunSuite with BeforeAndAfter {
  private var sc: SparkContext = _
  private val master = "local[4]"
  private val appName = "example-spark"
  private val network = Seq(
    ("Date1", "Source1", "Destination1", 3),
    ("Date2", "Source2", "Destination2", 4),
    ("Date3", "Source3", "Destination3", 100)
  )
  private val testFile = "temporaryTestFile.gexf"

  before {
    val conf = new SparkConf()
      .setMaster(master)
      .setAppName(appName)
    conf.set("spark.driver.allowMultipleContexts", "true");
    sc = new SparkContext(conf)
  }

  test("Creates the GEXF file from Array[Row]") {
    val testLines = (0, 12, 22, 34)
    if (Files.exists(Paths.get(testFile))) {
      new File(testFile).delete()
    }
    val networkarray = Array(
      Row.fromTuple(network(0)),
      Row.fromTuple(network(1)),
      Row.fromTuple(network(2))
    )
    val ret = WriteGEXF(networkarray, testFile)
    assert(ret)
    val lines = Source.fromFile(testFile).getLines.toList
    assert(lines(testLines._1) == """<?xml version="1.0" encoding="UTF-8"?>""")
    assert(
      lines(
        testLines._2
      ) == """<node id="8d3ab53ec817a1e5bf9ffd6e749b3983" label="Destination2" />"""
    )
    assert(lines(testLines._3) == """</attvalues>""")
    assert(lines(testLines._4) == """</edges>""")
    assert(!WriteGEXF(networkarray, ""))
  }

  test("Test if GEXF path is empty") {
    val networkGraph = sc.parallelize(network)
    val networkarray = Array(
      Row.fromTuple(network(0)),
      Row.fromTuple(network(1)),
      Row.fromTuple(network(2))
    )
    val gexf = WriteGEXF(networkarray, testFile)
    assert(gexf)
    assert(!WriteGEXF(networkarray, ""))
  }

  after {
    if (sc != null) {
      sc.stop()
    }
    if (Files.exists(Paths.get(testFile))) {
      new File(testFile).delete()
    }
  }
}
