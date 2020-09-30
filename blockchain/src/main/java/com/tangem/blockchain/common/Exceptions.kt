package com.tangem.blockchain.common

class CreateAccountUnderfunded(val minReserve: Amount): Exception()
class SendException(message: String): Exception(message)