package com.anjo.filebrowserapi.service

import com.anjo.filebrowserapi.TestObjects.BASE_FOLDER
import com.anjo.filebrowserapi.TestObjects.FIRST_NAME
import com.anjo.filebrowserapi.TestObjects.SECOND_NAME
import com.anjo.filebrowserapi.configuration.AppConfiguration
import com.anjo.filebrowserapi.exception.CannotRealizeException
import com.anjo.filebrowserapi.model.OperationType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Path
import kotlin.test.Test

class PathModifierUtilsTest {
    @TempDir
    private lateinit var workDir: Path

    private val utils by lazy { PathModifierUtils(AppConfiguration(workDir.toString())) }
    private val basePath: String get() = "$workDir/$BASE_FOLDER"

    @Test
    fun `should create base folder when initialized`() {
        // when
        val result = utils.generateDefaultPath()

        // then
        File(result).isDirectory shouldBe true
    }

    @Test
    fun `should keep base folder when it already exists`() {
        // given
        File(basePath).mkdirs()

        // when
        val result = utils.generateDefaultPath()

        // then
        result shouldBe basePath
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `should return base path when path is null or blank`(input: String?) {
        // when
        val result = utils.mergePath(input)

        // then
        result shouldBe basePath
    }

    @ParameterizedTest
    @ValueSource(strings = [FIRST_NAME, "/$FIRST_NAME"])
    fun `should append path without leading slash when path is given`(input: String) {
        // when
        val result = utils.mergePath(input)

        // then
        result shouldBe "$basePath/$FIRST_NAME"
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `should accept path when path is null or empty`(input: String?) {
        // when & then
        shouldNotThrowAny { utils.validPath(input, OperationType.READ) }
    }

    @Test
    fun `should accept path when path exists`() {
        // given
        File("$basePath/$FIRST_NAME").mkdirs()

        // when & then
        shouldNotThrowAny { utils.validPath(FIRST_NAME, OperationType.READ) }
    }

    @Test
    fun `should throw CannotRealizeException when path does not exist`() {
        // when
        val result = shouldThrow<CannotRealizeException> { utils.validPath(FIRST_NAME, OperationType.DELETE) }

        // then
        result.message shouldBe "Path: $FIRST_NAME to delete file or directory is not exist"
    }

    @Test
    fun `should return parent segments when path has several segments`() {
        // when
        val result = utils.extractMidPath("$FIRST_NAME/$SECOND_NAME/file.txt")

        // then
        result shouldBe listOf(FIRST_NAME, SECOND_NAME)
    }

    @Test
    fun `should place new name in base path when there are no parent segments`() {
        // when
        val result = utils.generatePathToRename(emptyList(), SECOND_NAME)

        // then
        result shouldBe "$basePath/$SECOND_NAME"
    }

    @Test
    fun `should place new name under parent segments when they are given`() {
        // when
        val result = utils.generatePathToRename(listOf(FIRST_NAME), SECOND_NAME)

        // then
        result shouldBe "$basePath/$FIRST_NAME/$SECOND_NAME"
    }
}
