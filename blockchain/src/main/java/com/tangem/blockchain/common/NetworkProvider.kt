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
fun <T : NetworkProvider> createWithPostfixIfContained(
    baseUrl: String,
    postfixUrl: String,
    create: (baseUrl: String, postfixUrl: String) -> T,
): T {
    return if (baseUrl.endsWith(suffix = "/$postfixUrl/")) {
        create(baseUrl.substringBeforeLast(delimiter = "$postfixUrl/"), postfixUrl)
    } else {
        create(baseUrl, "")
    }
}