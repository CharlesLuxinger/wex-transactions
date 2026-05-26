package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption

abstract class ArchitectureTest {
    companion object {
        const val BASE_PACKAGE = "com.charlesluxinger.wex_transactions"
        const val DOMAIN_PACKAGE = "$BASE_PACKAGE.domain.."
        const val APPLICATION_PACKAGE = "$BASE_PACKAGE.application.."
        const val INFRA_PACKAGE = "$BASE_PACKAGE.infra.."
        const val INFRA_CLIENT_PACKAGE = "${INFRA_PACKAGE}client.."
    }

    protected fun importClasses(): JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE)

    protected fun importClassesFrom(packageName: String): JavaClasses = ClassFileImporter().importPackages(packageName)
}
