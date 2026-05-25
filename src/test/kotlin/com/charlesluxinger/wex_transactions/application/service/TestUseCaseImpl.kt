package com.charlesluxinger.wex_transactions.application.service

import org.springframework.stereotype.Component

@Suppress("FunctionOnlyReturningConstant", "TooGenericExceptionThrown")
@Component
class TestUseCaseImpl {
    fun execute(): String = "done"

    fun throwError(): String = throw RuntimeException("test error")
}
