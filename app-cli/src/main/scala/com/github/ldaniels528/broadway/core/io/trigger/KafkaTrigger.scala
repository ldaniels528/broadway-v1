package com.github.ldaniels528.broadway.core.io.trigger

import com.github.ldaniels528.broadway.core.StoryConfig
import com.github.ldaniels528.broadway.core.io.device.OutputSource
import com.github.ldaniels528.broadway.core.io.layout.json.JsonFieldSet
import com.github.ldaniels528.broadway.core.io.layout.{Field, Layout}
import com.github.ldaniels528.broadway.core.io.{Data, Scope}
import kafka.consumer.{Consumer, ConsumerConfig}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Kafka Trigger
  */
case class KafkaTrigger(id: String,
                        topic: String,
                        parallelism: Int,
                        consumerConfig: ConsumerConfig,
                        output: OutputSource,
                        layout: Layout)
  extends Trigger {

  private val consumer = Consumer.create(consumerConfig)
  private val fieldSet = JsonFieldSet(Seq(Field("key", "key"), Field("message", "message")))

  override def execute(config: StoryConfig)(implicit ec: ExecutionContext) {
    val streamMap = consumer.createMessageStreams(Map(topic -> parallelism))

    for {
      streams <- streamMap.get(topic)
      stream <- streams
    } {
      Future {
        implicit val scope = Scope()
        val it = stream.iterator()
        while (it.hasNext()) {
          val mam = it.next()
          val dataSet = Seq(Data(fieldSet, mam.message()))
          layout.out(scope, output, dataSet, isEOF = false) foreach { outdata =>
            output.write(scope, outdata)
          }
        }
      }
    }

    logger.info("Done")
  }

}

