package com.charlesluxinger.wex_transactions.infra.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.read.ListAppender
import com.charlesluxinger.wex_transactions.application.service.TestUseCaseImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [
        ApplicationFlowLoggingAspectTest.TestAppConfig::class,
        AopLoggingConfig::class,
        ApplicationFlowLoggingAspect::class,
        TestUseCaseImpl::class,
    ],
)
class ApplicationFlowLoggingAspectTest {
    @Autowired
    private lateinit var testController: TestController

    @Autowired
    private lateinit var testUseCase: TestUseCaseImpl

    private lateinit var listAppender: ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>

    @BeforeEach
    fun setUp() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.level = Level.DEBUG
        listAppender = ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        listAppender.context = loggerContext
        listAppender.start()
        rootLogger.addAppender(listAppender)
    }

    @AfterEach
    fun tearDown() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.detachAppender(listAppender)
        MDC.clear()
    }

    @Test
    fun `should log controller start and success`() {
        val traceId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("traceId", traceId)

        testController.hello()

        val logEvents = listAppender.list
        val startLog = logEvents.firstOrNull { it.formattedMessage.contains("[CONTROLLER][START]") }
        val successLog = logEvents.firstOrNull { it.formattedMessage.contains("[CONTROLLER][SUCCESS]") }

        assertNotNull(startLog, "Should have CONTROLLER START log")
        assertNotNull(successLog, "Should have CONTROLLER SUCCESS log")
        assertTrue(successLog.formattedMessage.contains("elapsedMs="))
        assertTrue(startLog.mdcPropertyMap["traceId"] == traceId)
    }

    @Test
    fun `should log use case with elapsed time`() {
        val traceId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("traceId", traceId)

        testUseCase.execute()

        val logEvents = listAppender.list
        val startLog = logEvents.firstOrNull { it.formattedMessage.contains("[USECASE][START]") }
        val successLog = logEvents.firstOrNull { it.formattedMessage.contains("[USECASE][SUCCESS]") }

        assertNotNull(startLog, "Should have USECASE START log")
        assertNotNull(successLog, "Should have USECASE SUCCESS log")
        assertTrue(successLog.formattedMessage.contains("elapsedMs="))
        assertTrue(startLog.mdcPropertyMap["traceId"] == traceId)
    }

    @Test
    fun `should log error on exception`() {
        try {
            testUseCase.throwError()
        } catch (_: RuntimeException) {
        }

        val logEvents = listAppender.list
        val errorLog = logEvents.firstOrNull { it.formattedMessage.contains("[USECASE][ERROR]") }

        assertNotNull(errorLog, "Should have USECASE ERROR log")
        assertTrue(errorLog.formattedMessage.contains("elapsedMs="))
        assertTrue(errorLog.level == Level.ERROR)
    }

    @Test
    fun `should include traceId in log event MDC`() {
        val traceId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("traceId", traceId)

        testController.hello()

        val logEvents = listAppender.list
        val hasTraceId =
            logEvents.any {
                it.mdcPropertyMap["traceId"] == traceId
            }

        assertTrue(hasTraceId, "Log events should contain traceId in MDC")
    }

    @TestConfiguration
    class TestAppConfig {
        @Bean
        fun testController() = TestController()
    }

    @Test
    fun `should log only args summary at INFO and full args at DEBUG`() {
        val traceId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("traceId", traceId)

        testController.echo("very-sensitive-payload")

        val logEvents = listAppender.list
        val infoStartLog =
            logEvents.firstOrNull {
                it.level == Level.INFO &&
                    it.formattedMessage.contains("[CONTROLLER][START]")
            }
        val debugDetailLog =
            logEvents.firstOrNull {
                it.level == Level.DEBUG &&
                    it.formattedMessage.contains("[CONTROLLER][START][DETAIL]")
            }

        assertNotNull(infoStartLog, "Should have CONTROLLER START INFO log")
        assertNotNull(debugDetailLog, "Should have CONTROLLER START DETAIL DEBUG log")
        assertTrue(infoStartLog.formattedMessage.contains("argsSummary=count=1, types=[String]"))
        assertTrue(!infoStartLog.formattedMessage.contains("very-sensitive-payload"))
        assertTrue(debugDetailLog.formattedMessage.contains("very-sensitive-payload"))
    }

    @Suppress("FunctionOnlyReturningConstant")
    @RestController
    class TestController {
        @GetMapping("/test")
        fun hello(): String = "hello"

        fun echo(payload: String): String = payload
    }
}
