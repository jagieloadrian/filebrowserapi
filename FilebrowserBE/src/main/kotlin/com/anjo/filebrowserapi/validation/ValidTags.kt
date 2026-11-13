package com.anjo.filebrowserapi.validation

import com.anjo.filebrowserapi.exception.BadInputException
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

const val MAX_TAGS = 20
const val MAX_DESCRIPTION = 1000
const val TAG_RULE = "tags must be 1-50 letters, digits, spaces, '.', '_' or '-' and start with a letter or digit"

private val TAG = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N} ._-]{0,49}")

fun isValidTag(value: String): Boolean = TAG.matches(value)

fun normalizeTags(tags: List<String>): List<String> {
    if (tags.size > MAX_TAGS || !tags.all(::isValidTag)) throw BadInputException("At most $MAX_TAGS $TAG_RULE")
    return tags.map { it.lowercase() }.distinct().sorted()
}

fun normalizeDescription(description: String?): String? {
    val text = description?.trim()?.ifEmpty { null }
    if (text != null && text.length > MAX_DESCRIPTION) throw BadInputException("Description is longer than $MAX_DESCRIPTION")
    return text
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidTagsValidator::class])
annotation class ValidTags(
    val message: String = TAG_RULE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ValidTagsValidator : ConstraintValidator<ValidTags, List<String>> {
    override fun isValid(
        value: List<String>?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || value.all(::isValidTag)
}
