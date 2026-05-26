package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RetrieveConvertedQueryValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    @DisplayName("RetrieveConvertedQuery fails validation when targetCurrency is empty")
    fun `retrieve converted query fails when target currency is empty`() {
        val query = RetrieveConvertedQuery(purchaseId = 1L, targetCurrency = "")

        val violations = validator.validate(query)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().propertyPath.toString()).isEqualTo("targetCurrency")
        assertThat(violations.first().message).isEqualTo("Target currency must not be blank")
    }

    @Test
    @DisplayName("RetrieveConvertedQuery fails validation when targetCurrency is blank")
    fun `retrieve converted query fails when target currency is blank`() {
        val query = RetrieveConvertedQuery(purchaseId = 1L, targetCurrency = "   ")

        val violations = validator.validate(query)

        assertThat(violations).hasSize(1)
        assertThat(violations.first().propertyPath.toString()).isEqualTo("targetCurrency")
        assertThat(violations.first().message).isEqualTo("Target currency must not be blank")
    }

    @Test
    @DisplayName("RetrieveConvertedQuery passes validation when targetCurrency is descriptor")
    fun `retrieve converted query passes when target currency is descriptor`() {
        val query = RetrieveConvertedQuery(purchaseId = 1L, targetCurrency = "Canada-Dollar")

        val violations = validator.validate(query)

        assertThat(violations).isEmpty()
    }
}
