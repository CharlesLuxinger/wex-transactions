package com.charlesluxinger.wex_transactions.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

object WireMockConfig {
    fun createServer(port: Int = 0): WireMockServer =
        if (port == 0) {
            WireMockServer(options().dynamicPort())
        } else {
            WireMockServer(options().port(port))
        }

    fun startAndReset(server: WireMockServer): WireMockServer {
        if (!server.isRunning) {
            server.start()
        }
        server.resetAll()
        return server
    }

    fun stopQuietly(server: WireMockServer) {
        if (server.isRunning) {
            server.stop()
        }
    }
}
