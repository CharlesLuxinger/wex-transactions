package com.charlesluxinger.wex_transactions.infra.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.MDC
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class TraceIdFilterTest {
    private val filter = TraceIdFilter()

    private val request: HttpServletRequest = mock()
    private val response: HttpServletResponse = mock()
    private val chain: FilterChain = mock()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `should use traceId from X-Trace-Id header when valid`() {
        var capturedTraceId: String? = null
        val capturingChain =
            FilterChain { _: ServletRequest?, _: ServletResponse? ->
                capturedTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY)
            }
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("abc123def456")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, capturingChain)

        kotlin.test.assertEquals("abc123def456", capturedTraceId)
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY), "MDC must be cleared after request")
    }

    @Test
    fun `should generate UUID when X-Trace-Id header is absent`() {
        var capturedTraceId: String? = null
        val capturingChain =
            FilterChain { _: ServletRequest?, _: ServletResponse? ->
                capturedTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY)
            }
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null)
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, capturingChain)

        kotlin.test.assertTrue((capturedTraceId ?: "").matches(Regex("^[a-zA-Z0-9\\-]{1,64}$")))
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY), "MDC must be cleared after request")
    }

    @Test
    fun `should generate UUID when X-Trace-Id header is blank`() {
        var capturedTraceId: String? = null
        val capturingChain =
            FilterChain { _: ServletRequest?, _: ServletResponse? ->
                capturedTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY)
            }
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, capturingChain)

        kotlin.test.assertTrue((capturedTraceId ?: "").matches(Regex("^[a-zA-Z0-9\\-]{1,64}$")))
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY))
    }

    @Test
    fun `should skip filter for actuator paths`() {
        `when`(request.servletPath).thenReturn("/actuator/health")
        `when`(request.method).thenReturn("GET")

        filter.doFilter(request, response, chain)

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY), "MDC must not be set for actuator paths")
    }

    @Test
    fun `should clear MDC after filter chain`() {
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("trace-123")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, chain)

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY))
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `should clear MDC even when exception thrown`() {
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("trace-123")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")
        `when`(chain.doFilter(request, response)).thenThrow(RuntimeException::class.java)

        try {
            filter.doFilter(request, response, chain)
        } catch (_: RuntimeException) {
        }

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY))
    }

    @Test
    fun `should reject invalid chars in X-Trace-Id header`() {
        var capturedTraceId: String? = null
        val capturingChain =
            FilterChain { _: ServletRequest?, _: ServletResponse? ->
                capturedTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY)
            }
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("abc$123")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, capturingChain)

        assertNotEquals("abc$123", capturedTraceId)
        kotlin.test.assertTrue((capturedTraceId ?: "").matches(Regex("^[a-zA-Z0-9\\-]{1,64}$")))
    }

    @Test
    fun `should reject overlong X-Trace-Id header`() {
        var capturedTraceId: String? = null
        val capturingChain =
            FilterChain { _: ServletRequest?, _: ServletResponse? ->
                capturedTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY)
            }
        val overlongTraceId = "a".repeat(65)
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(overlongTraceId)
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, capturingChain)

        assertNotEquals(overlongTraceId, capturedTraceId)
        kotlin.test.assertTrue((capturedTraceId ?: "").matches(Regex("^[a-zA-Z0-9\\-]{1,64}$")))
    }
}
