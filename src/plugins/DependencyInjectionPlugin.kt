package org.expenny.service.plugins

import org.expenny.service.app.di.AppModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.installDependencyInjectionPlugin() {
    install(Koin) {
        // SLF4JLogger()
        modules(
            AppModule.configurationModule,
            AppModule.authenticationModule,
            AppModule.databaseModule,
            AppModule.schedulerModule,
            AppModule.repositoryModule,
            AppModule.serviceModule,
            AppModule.controllerModule,
            AppModule.clientModule,
        )
    }
}