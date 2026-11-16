package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.api.GrpcFilebrowserApi
import com.anjo.filebrowser.ui.api.HttpFilebrowserApi
import com.anjo.filebrowser.ui.files.FilesViewModel
import com.anjo.filebrowser.ui.trash.TrashViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.koin.dsl.koinApplication
import org.koin.plugin.module.dsl.module

class AppModuleTest :
    FunSpec({
        test("should build the view models on the configured transport from the annotated module") {
            // given
            val koin = koinApplication { module<AppModule>() }.koin
            val transport = if (BuildKonfig.transport == "grpc") GrpcFilebrowserApi::class else HttpFilebrowserApi::class

            // when & then
            try {
                koin.get<FilebrowserApi>()::class shouldBe transport
                koin.get<FilesViewModel>()
                koin.get<TrashViewModel>()
            } finally {
                koin.close()
            }
        }
    })
