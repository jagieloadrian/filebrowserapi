package com.anjo.filebrowser.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.anjo.filebrowser.ui.files.tokenStyle
import com.anjo.filebrowser.ui.model.TokenKind
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual

private const val TEXT_CONTRAST = 4.5

private data class SchemeCase(
    val palette: Palette,
    val dark: Boolean,
)

private fun contrast(
    first: Color,
    second: Color,
): Double {
    val (lighter, darker) = listOf(first.luminance(), second.luminance()).sortedDescending()
    return (lighter + 0.05) / (darker + 0.05)
}

class ColorSchemesTest :
    FunSpec({
        context("should give text at least 4.5:1 contrast on its background in every theme and mode") {
            withTests(
                nameFn = { "${it.palette.label} ${if (it.dark) "dark" else "light"}" },
                Palette.entries.flatMap { listOf(SchemeCase(it, dark = false), SchemeCase(it, dark = true)) },
            ) { case ->
                // given
                val scheme = case.palette.colorScheme(case.dark)

                // when
                val result =
                    with(scheme) {
                        listOf(
                            primary to onPrimary,
                            primaryContainer to onPrimaryContainer,
                            secondary to onSecondary,
                            secondaryContainer to onSecondaryContainer,
                            tertiary to onTertiary,
                            tertiaryContainer to onTertiaryContainer,
                            error to onError,
                            errorContainer to onErrorContainer,
                            background to onBackground,
                            surface to onSurface,
                            surface to onSurfaceVariant,
                            surfaceContainer to onSurfaceVariant,
                            surfaceContainerHighest to onSurfaceVariant,
                            inverseSurface to inverseOnSurface,
                        ).map { (background, text) -> contrast(background, text) }
                    }

                // then
                result.forEach { it shouldBeGreaterThanOrEqual TEXT_CONTRAST }
            }
        }

        context("should give every highlighted code token at least 4.5:1 contrast on the preview surface") {
            withTests(
                nameFn = { "${it.palette.label} ${if (it.dark) "dark" else "light"}" },
                Palette.entries.flatMap { listOf(SchemeCase(it, dark = false), SchemeCase(it, dark = true)) },
            ) { case ->
                // given
                val scheme = case.palette.colorScheme(case.dark)

                // when
                val result = TokenKind.entries.map { contrast(scheme.surface, scheme.tokenStyle(it).color) }

                // then
                result.forEach { it shouldBeGreaterThanOrEqual TEXT_CONTRAST }
            }
        }
    })
