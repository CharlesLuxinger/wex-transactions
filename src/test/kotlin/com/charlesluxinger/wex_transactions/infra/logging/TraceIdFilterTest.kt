package com.charlesluxinger.wex_transactions.infra.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.MDC
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
    fun `should use traceId from X-Trace-Id header when present`() {
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("abc123def456")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, chain)

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY), "MDC must be cleared after request")
    }

    @Test
    fun `should generate UUID when X-Trace-Id header is absent`() {
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null)
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, chain)

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_KEY), "MDC must be cleared after request")
    }

    @Test
    fun `should generate UUID when X-Trace-Id header is blank`() {
        `when`(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn("")
        `when`(request.servletPath).thenReturn("/api/v1/purchases")
        `when`(request.method).thenReturn("POST")

        filter.doFilter(request, response, chain)

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
}
