package com.charlesluxinger.wex_transactions.infra.adapter.event.config

import com.charlesluxinger.wex_transactions.infra.adapter.event.ExchangeRateFetchedEventListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration

@Configuration
class ExchangeRateEventsStreamConfig {

    @Bean
    fun exchangeRateEventsStreamProperties(): ExchangeRateEventsStreamProperties =
        ExchangeRateEventsStreamProperties()

    @Bean
    fun exchangeRateEventsStreamOffset(properties: ExchangeRateEventsStreamProperties): StreamOffset<String> =
        StreamOffset.create(properties.key, ReadOffset.lastConsumed())

    @Bean
    fun exchangeRateEventsStreamListenerContainer(
        redisConnectionFactory: RedisConnectionFactory,
        streamOffset: StreamOffset<String>,
        eventListener: ExchangeRateFetchedEventListener,
        stringRedisTemplate: StringRedisTemplate,
        properties: ExchangeRateEventsStreamProperties,
    ): StreamMessageListenerContainer<String, MapRecord<String, String, String>> {
        val streamOps = stringRedisTemplate.opsForStream<String, String>()
        createConsumerGroupIfMissing(streamOps, properties.key, properties.group)
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofSeconds(2))
            .build()
        val container = StreamMessageListenerContainer.create(redisConnectionFactory, options)
        container.receive(eventListener.consumer, streamOffset, eventListener)
        container.start()
        return container
    }
}

private fun createConsumerGroupIfMissing(
    streamOps: StreamOperations<String, String, String>,
    streamKey: String,
    groupName: String,
) {
    try {
        streamOps.createGroup(streamKey, ReadOffset.latest(), groupName)
    } catch (ex: RedisSystemException) {
        val cause = ex.cause
        if (cause?.message?.contains("BUSYGROUP", ignoreCase = true) != true) {
            logger.warn("Failed to create Redis consumer group group={} stream={}", groupName, streamKey, ex)
        }
    }
}

private val logger = LoggerFactory.getLogger("ExchangeRateEventsStreamConfig")
