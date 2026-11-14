package com.anjo.filebrowser.ui

import com.anjo.filebrowser.ui.api.FilebrowserApi
import com.anjo.filebrowser.ui.api.HttpFilebrowserApi
import com.anjo.filebrowser.ui.files.FilesViewModel
import com.anjo.filebrowser.ui.trash.TrashViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.dsl.koinApplication
import org.koin.plugin.module.dsl.module

class AppModuleTest :
    FunSpec({
        test("should build the view models from the annotated module") {
            // given
            val koin = koinApplication { module<AppModule>() }.koin

            // when & then
            try {
                koin.get<FilebrowserApi>().shouldBeInstanceOf<HttpFilebrowserApi>()
                koin.get<FilesViewModel>()
                koin.get<TrashViewModel>()
            } finally {
                koin.close()
            }
        }
    })
