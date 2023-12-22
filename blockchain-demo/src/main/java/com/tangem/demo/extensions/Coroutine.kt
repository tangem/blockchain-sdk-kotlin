package com.tangem.demo.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by Anton Zhilenkov on 12/08/2022.
 */
suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
