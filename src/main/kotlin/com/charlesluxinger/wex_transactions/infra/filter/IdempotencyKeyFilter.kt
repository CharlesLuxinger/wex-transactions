package com.charlesluxinger.wex_transactions.infra.filter

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IdempotencyKeyFilter : Filter {
    companion object {
        const val IDEMPOTENCY_KEY_HEADER_NAME = "X-Idempotency-Key"
        const val REQUEST_ATTRIBUTE_NAME = "idempotencyKey"
    }

    @Suppress("SwallowedException")
    override fun doFilter(
        request: ServletRequest?,
        response: ServletResponse?,
        chain: FilterChain?,
    ) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            val headerValue = request.getHeader(IDEMPOTENCY_KEY_HEADER_NAME)
            if (!headerValue.isNullOrBlank()) {
                try {
                    val idempotencyKey = IdempotencyKey(UUID.fromString(headerValue.trim()))
                    request.setAttribute(REQUEST_ATTRIBUTE_NAME, idempotencyKey)
                    // Echo key back in response
                    response.addHeader(IDEMPOTENCY_KEY_HEADER_NAME, idempotencyKey.value.toString())
                } catch (e: IllegalArgumentException) {
                    // Invalid UUID format will be caught by controller validation
                    request.setAttribute("${REQUEST_ATTRIBUTE_NAME}_error", headerValue)
                }
            }
        }
        chain?.doFilter(request, response)
    }
}
