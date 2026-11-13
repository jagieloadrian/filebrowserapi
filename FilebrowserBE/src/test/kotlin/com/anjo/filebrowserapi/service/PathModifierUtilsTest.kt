package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.TestObjects.USER
import com.anjo.filebrowserapi.TestObjects.signIn
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.BadInputException
import com.anjo.filebrowserapi.exception.NotFoundException
import com.anjo.filebrowserapi.model.OperationType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import org.springframework.security.core.context.SecurityContextHolder
import java.io.File
import java.nio.file.Path

class PathModifierUtilsTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val workDir = tempdir()
        val utils = PathModifierUtils(AppConfiguration(workDir.path))
        val basePath = "${workDir.path}/$BASE_FOLDER"

        beforeEach { signIn() }

        afterEach { SecurityContextHolder.clearContext() }

        test("should create base folder when initialized") {
            // when
            val result = utils.generateDefaultPath()

            // then
            File(result).isDirectory shouldBe true
        }

        test("should keep base folder when it already exists") {
            // given
            File(basePath).mkdirs()

            // when
            val result = utils.generateDefaultPath()

            // then
            result shouldBe basePath
        }

        context("should return base path when path is null or blank") {
            withTests(mapOf("null" to null, "empty" to "", "blank" to " ")) { input ->
                // when
                val result = utils.mergePath(input)

                // then
                result shouldBe basePath
            }
        }

        context("should append path without leading slash when path is given") {
            withTests(FIRST_NAME, "/$FIRST_NAME") { input ->
                // when
                val result = utils.mergePath(input)

                // then
                result shouldBe "$basePath/$FIRST_NAME"
            }
        }

        context("should accept path when path is null or empty") {
            withTests(mapOf("null" to null, "empty" to "")) { input ->
                // when & then
                shouldNotThrowAny { utils.validPath(input, OperationType.READ) }
            }
        }

        test("should accept path when path exists") {
            // given
            File("$basePath/$FIRST_NAME").mkdirs()

            // when & then
            shouldNotThrowAny { utils.validPath(FIRST_NAME, OperationType.READ) }
        }

        test("should throw NotFoundException when path does not exist") {
            // when
            val result = shouldThrow<NotFoundException> { utils.validPath(FIRST_NAME, OperationType.DELETE) }

            // then
            result.message shouldBe "Path: $FIRST_NAME to delete file or directory is not exist"
        }

        test("should return parent segments when path has several segments") {
            // when
            val result = utils.extractMidPath("$FIRST_NAME/$SECOND_NAME/file.txt")

            // then
            result shouldBe listOf(FIRST_NAME, SECOND_NAME)
        }

        test("should place new name in base path when there are no parent segments") {
            // when
            val result = utils.generatePathToRename(emptyList(), SECOND_NAME)

            // then
            result shouldBe "$basePath/$SECOND_NAME"
        }

        test("should place new name under parent segments when they are given") {
            // when
            val result = utils.generatePathToRename(listOf(FIRST_NAME), SECOND_NAME)

            // then
            result shouldBe "$basePath/$FIRST_NAME/$SECOND_NAME"
        }

        context("should throw BadInputException when path leaves base directory") {
            withTests("..", "../outside", "a/../../outside", "/../outside", "a/b/../../../outside") { input ->
                // when & then
                shouldThrow<BadInputException> { utils.resolve(input) }
            }
        }

        test("should throw BadInputException when name leaves base directory") {
            // when & then
            shouldThrow<BadInputException> { utils.resolve(FIRST_NAME, "../../outside") }
        }

        context("should throw BadInputException when name is blank or contains slash") {
            withTests(mapOf("empty" to "", "blank" to " ", "a/b" to "a/b", "/a" to "/a", "a/" to "a/")) { input ->
                // when & then
                assertSoftly {
                    shouldThrow<BadInputException> { utils.resolveName(FIRST_NAME, input) }
                    shouldThrow<BadInputException> { utils.generatePathToRename(emptyList(), input) }
                }
            }
        }

        test("should resolve name under parent when name is valid") {
            // when
            val result = utils.resolveName(FIRST_NAME, SECOND_NAME)

            // then
            result.path shouldBe "$basePath/$FIRST_NAME/$SECOND_NAME"
        }

        test("should not reach another user's directory when path climbs out of own directory") {
            // given
            utils.resolveName(null, FIRST_NAME).mkdirs()
            signIn("user-2")

            // when & then
            shouldThrow<BadInputException> { utils.resolve("../$USER/$FIRST_NAME") }
        }

        context("should throw BadInputException when token subject cannot name a directory") {
            withTests(
                mapOf("empty" to "", "." to ".", ".." to "..", "../evil" to "../evil", "a/b" to "a/b", ".hidden" to ".hidden"),
            ) { sub ->
                // given
                signIn(sub)

                // when & then
                shouldThrow<BadInputException> { utils.generateDefaultPath() }
            }
        }

        test("should throw IllegalStateException when nobody is signed in") {
            // given
            SecurityContextHolder.clearContext()

            // when & then
            shouldThrow<IllegalStateException> { utils.resolve(FIRST_NAME) }
        }

        test("should resolve inside base directory when segments are relative") {
            // when
            val result = utils.resolve("/$FIRST_NAME", "", null, SECOND_NAME)

            // then
            result.path shouldBe "$basePath/$FIRST_NAME/$SECOND_NAME"
        }

        test("should throw BadInputException when entry is the base directory") {
            // when & then
            shouldThrow<BadInputException> { utils.resolveEntry(FIRST_NAME, "..") }
        }

        test("should never resolve outside base directory when segments are arbitrary") {
            // given
            val segment = Arb.element("..", ".", "a", "b", "/", "a/..", "../a", "/a", "")

            // when & then
            checkAll(Arb.list(segment, 0..6)) { input ->
                val result =
                    try {
                        utils.resolve(*input.toTypedArray()).toPath()
                    } catch (e: BadInputException) {
                        null
                    }
                result?.startsWith(Path.of(basePath))?.shouldBe(true)
            }
        }
    })
