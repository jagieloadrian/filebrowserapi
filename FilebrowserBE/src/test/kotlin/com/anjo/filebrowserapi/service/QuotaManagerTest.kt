package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.USER
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.QuotaExceededException
import com.anjo.filebrowserapi.model.Quota
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.unit.DataSize
import java.io.File

class QuotaManagerTest :
    FunSpec({
        fun service(
            workDir: File,
            quota: Long = 10,
            minFreeSpace: DataSize = DataSize.ofBytes(0),
        ) = QuotaManager(
            PathModifierUtils(AppConfiguration(workDir.path)),
            AppConfiguration(workDir.path, quota = DataSize.ofBytes(quota), minFreeSpace = minFreeSpace),
        )

        fun File.write(
            path: String,
            content: String,
        ) = File(this, path).apply { parentFile.mkdirs() }.writeText(content)

        beforeEach { signIn() }

        afterEach { SecurityContextHolder.clearContext() }

        test("should count files, trash and versions of the current user only") {
            // given
            val workDir = tempdir()
            workDir.write("$BASE_FOLDER/docs/a.txt", "abc")
            workDir.write("filebrowser-trash/$USER/t1", "de")
            workDir.write("filebrowser-versions/$USER/v1", "f")
            workDir.write("filebrowser/other-user/b.txt", "ignored")

            // when & then
            service(workDir).quota() shouldBe Quota(6, 10)
        }

        test("should allow a write that fits and refuse one that exceeds the quota") {
            // given
            val workDir = tempdir()
            workDir.write("$BASE_FOLDER/a.txt", "abcdef")
            val service = service(workDir)

            // when
            service.reserve(4)
            val exception = shouldThrow<QuotaExceededException> { service.reserve(5) }

            // then
            exception.message shouldBe "Quota exceeded: 6 of 10 bytes used, 5 more requested"
        }

        test("should refuse a write that leaves less than the minimum free disk space") {
            // given
            val service = service(tempdir(), quota = Long.MAX_VALUE, minFreeSpace = DataSize.ofBytes(Long.MAX_VALUE))

            // when & then
            shouldThrow<QuotaExceededException> { service.reserve(1) }.message shouldContain "free disk space"
        }
    })
