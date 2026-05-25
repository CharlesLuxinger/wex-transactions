package com.charlesluxinger.wex_transactions.infra.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class ApplicationFlowLoggingAspect {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")
    fun logControllerExecution(joinPoint: ProceedingJoinPoint): Any? = logExecutionStep("CONTROLLER", joinPoint)

    @Around("execution(public * com.charlesluxinger.wex_transactions.application.service..*UseCaseImpl.*(..))")
    fun logUseCaseExecution(joinPoint: ProceedingJoinPoint): Any? = logExecutionStep("USECASE", joinPoint)

    @Around("execution(public * com.charlesluxinger.wex_transactions.infra.adapter..*Adapter.*(..))")
    fun logAdapterExecution(joinPoint: ProceedingJoinPoint): Any? = logExecutionStep("ADAPTER", joinPoint)

    @Suppress("TooGenericExceptionCaught")
    private fun logExecutionStep(
        layer: String,
        joinPoint: ProceedingJoinPoint,
    ): Any? {
        val signature = "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
        val args = joinPoint.args.joinToString(prefix = "[", postfix = "]") { arg -> arg?.toString() ?: "null" }
        val startedAt = System.currentTimeMillis()

        logger.info("[$layer][START] {} args={}", signature, args)

        return try {
            val result = joinPoint.proceed()
            val elapsedMs = System.currentTimeMillis() - startedAt
            logger.info("[$layer][SUCCESS] {} elapsedMs={}", signature, elapsedMs)
            result
        } catch (ex: Throwable) {
            val elapsedMs = System.currentTimeMillis() - startedAt
            logger.error("[$layer][ERROR] {} elapsedMs={} message={}", signature, elapsedMs, ex.message, ex)
            throw ex
        }
    }
}
