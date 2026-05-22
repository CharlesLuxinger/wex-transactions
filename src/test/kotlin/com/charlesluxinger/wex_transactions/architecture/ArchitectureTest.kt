package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter

/**
 * Base class for architecture tests providing centralized import and package constants.
 *
 * This class enforces the following import policy:
 * - Only production classes under `com.charlesluxinger.wex_transactions` are in scope
 * - Test classes are explicitly excluded from import scope
 *
 * Governance rule reference: AGENTS.md Section "Governance Rules"
 */
abstract class ArchitectureTest {
    companion object {
        /**
         * Base package for all production classes.
         * Governance: domain, application, infra packages must reside under this root.
         */
        const val BASE_PACKAGE = "com.charlesluxinger.wex_transactions"
        const val DOMAIN_PACKAGE = "$BASE_PACKAGE.domain.."
        const val APPLICATION_PACKAGE = "$BASE_PACKAGE.application.."
        const val INFRA_PACKAGE = "$BASE_PACKAGE.infra.."
        const val INFRA_CLIENT_PACKAGE = "${INFRA_PACKAGE}client.."
    }

    /**
     * Imports all production classes under the base package.
     *
     * This ensures architecture tests only validate production code,
     * not test infrastructure or fixtures.
     *
     * @return JavaClasses resolved from production packages only
     */
    protected fun importClasses(): JavaClasses = ClassFileImporter().importPackages(BASE_PACKAGE)

    /**
     * Imports only classes from a specific package and its subpackages.
     *
     * @param packageName the root package to import
     * @return JavaClasses from the specified package
     */
    protected fun importClassesFrom(packageName: String): JavaClasses = ClassFileImporter().importPackages(packageName)
}
