package com.tangem.blockchain.common

interface NetworkProvider {
    val baseUrl: String
}

/**
 * Create instance [NetworkProvider].

 * and [postfixUrl] will be put in each request.
 *
 * This mechanism helps to support urls without '/' in the end.
 * Example: https://base-url/postfix-url        – isn't working
 *          https://base-url/   +   postfix-url – is working
 */
internal fun <T : NetworkProvider> createWithPostfixIfContained(
    baseUrl: String,
    postfixUrl: String,
    create: (baseUrl: String, postfixUrl: String) -> T,
): T {
    return createWithPostfixIfContainedInternal(
        isUrlWithPostfix = baseUrl.endsWith(suffix = "/$postfixUrl/"),
        baseUrl = baseUrl,
        postfixUrl = postfixUrl,
        create = create,
    )
}

/**
 * Create instance [NetworkProvider].

 * and [postfixUrl] will be put in each request.
 *
 * This mechanism helps to support urls without '/' in the end.
 * Example: https://base-url/postfix-url        – isn't working
 *          https://base-url/   +   postfix-url – is working
 */
internal fun <T : NetworkProvider> createWithPostfixIfContained(
    baseUrl: String,
    vararg postfixUrl: String,
    create: (baseUrl: String, postfixUrl: String) -> T,
): T {
    val postfix = postfixUrl
        .filter(String::isNotBlank)
        .firstOrNull { baseUrl.endsWith(suffix = "/$it/") }

    return createWithPostfixIfContainedInternal(
        isUrlWithPostfix = postfix != null,
        baseUrl = baseUrl,
        postfixUrl = postfix.orEmpty(),
        create = create,
    )
}

private fun <T> createWithPostfixIfContainedInternal(
    isUrlWithPostfix: Boolean,
    baseUrl: String,
    postfixUrl: String,
    create: (baseUrl: String, postfixUrl: String) -> T,
): T {
    return if (isUrlWithPostfix) {
        create(baseUrl.substringBeforeLast(delimiter = "$postfixUrl/"), postfixUrl)
    } else {
        create(baseUrl, "")
    }
}