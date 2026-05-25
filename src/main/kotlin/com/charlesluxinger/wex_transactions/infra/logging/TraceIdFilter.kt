package com.charlesluxinger.wex_transactions.infra.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.toTraceId
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class TraceIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = extractTraceId(request)
        MDC.put(TRACE_ID_KEY, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return path.contains(ACTUATOR_PATH)
    }

    private fun extractTraceId(request: HttpServletRequest): String {
        val header = request.getHeader(TRACE_ID_HEADER)
        return if (!header.isNullOrBlank()) header.trim() else generateTraceId()
    }

    private fun generateTraceId(): String = UUID.randomUUID().toTraceId()

    companion object {
        const val TRACE_ID_KEY = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
        private const val ACTUATOR_PATH = "/actuator/"
    }
}
