package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.api.HttpFilebrowserApi
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.anjo.filebrowser.ui")
class AppModule

@Singleton
fun filebrowserApi(): FilebrowserApi = HttpFilebrowserApi(HttpClient(), BuildKonfig.backendUrl)
