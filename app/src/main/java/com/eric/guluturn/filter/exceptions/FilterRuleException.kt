package com.eric.guluturn.filter.exceptions

class FilterRuleException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
