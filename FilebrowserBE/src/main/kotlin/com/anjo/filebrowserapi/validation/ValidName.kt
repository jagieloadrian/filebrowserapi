package com.anjo.filebrowserapi.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

const val NAME_RULE = "must not be blank or contain '/'"

fun isValidName(value: String): Boolean = value.isNotBlank() && '/' !in value

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidNameValidator::class])
annotation class ValidName(
    val message: String = NAME_RULE,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class ValidNameValidator : ConstraintValidator<ValidName, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || isValidName(value)
}
