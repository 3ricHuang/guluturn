package com.eric.guluturn.common.errors

import com.eric.guluturn.common.constants.MAX_PROFILES_PER_API_KEY

class MaxProfilesExceededException : IllegalStateException(
    "You can create up to $MAX_PROFILES_PER_API_KEY profiles per API key."
)
