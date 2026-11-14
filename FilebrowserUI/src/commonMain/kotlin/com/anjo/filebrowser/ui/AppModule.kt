package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.api.HttpFilebrowserApi
import com.anjo.filebrowser.ui.api.grpcFilebrowserApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

private const val GRPC = "grpc"

@Module
@ComponentScan("com.anjo.filebrowser.ui")
class AppModule

@Singleton
fun filebrowserApi(): FilebrowserApi =
    if (BuildKonfig.transport == GRPC) {
        grpcFilebrowserApi(BuildKonfig.backendUrl)
    } else {
        HttpFilebrowserApi(HttpClient(), BuildKonfig.backendUrl)
    }
