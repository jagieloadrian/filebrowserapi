package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.api.HttpFilebrowserApi
import com.anjo.filebrowser.ui.api.bearer
import com.anjo.filebrowser.ui.api.grpcFilebrowserApi
import com.anjo.filebrowser.ui.auth.AuthSession
import com.anjo.filebrowser.ui.auth.Oidc
import com.anjo.filebrowser.ui.auth.authBrowser
import io.ktor.client.HttpClient
import kotlinx.coroutines.MainScope
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

private const val GRPC = "grpc"

@Module
@ComponentScan("com.anjo.filebrowser.ui")
class AppModule

@Singleton
fun authSession(): AuthSession =
    AuthSession(
        oidc =
            BuildKonfig.oidcIssuer.takeIf { it.isNotEmpty() }?.let {
                Oidc(HttpClient(), it, BuildKonfig.oidcClientId, BuildKonfig.oidcScopes)
            },
        browser = authBrowser(),
        scope = MainScope(),
    )

@Singleton
fun filebrowserApi(session: AuthSession): FilebrowserApi =
    if (BuildKonfig.transport == GRPC) {
        grpcFilebrowserApi(BuildKonfig.backendUrl, session)
    } else {
        HttpFilebrowserApi(HttpClient().bearer(session), BuildKonfig.backendUrl)
    }
