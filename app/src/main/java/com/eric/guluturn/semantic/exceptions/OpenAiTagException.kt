package com.eric.guluturn.semantic.exceptions

/**
 * Base exception for tag generation errors.
 *
 * This exception class is designed to handle errors that occur during the
 * generation of semantic tags from user input.
 *
 * @param message The error message describing the cause of the exception.
 * @param cause The underlying exception that caused this error, if any.
 */
open class TagGenerationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when an error occurs during OpenAI-based tag generation.
 *
 * This exception is specifically used when the OpenAiTagGenerator encounters
 * API errors or issues with response parsing.
 *
 * @param message The error message detailing the problem.
 * @param cause The underlying exception that triggered this error, if any.
 */
class OpenAiTagException(
    message: String,
    cause: Throwable? = null
) : TagGenerationException(message, cause)
