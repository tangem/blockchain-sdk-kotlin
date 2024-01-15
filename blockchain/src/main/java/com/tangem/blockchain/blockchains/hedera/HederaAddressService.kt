package com.tangem.blockchain.blockchains.hedera

import android.content.Context
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.Client
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.ContextAddressProvider
import com.tangem.blockchain.common.preferences.BlockchainSharedPrefs
import com.tangem.blockchain.common.preferences.DefaultBlockchainSharedPrefs
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.toCompressedPublicKey
import com.tangem.common.extensions.toHexString

class HederaAddressService(isTestnet: Boolean, context: Context? = null) : AddressService(), ContextAddressProvider {

    private val client = if (isTestnet) Client.forTestnet() else Client.forMainnet()
    private var sharedPrefs: BlockchainSharedPrefs? = null

    init {
        tryInitSharedPrefs(context)
    }

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return ""
    }

    override fun validate(address: String): Boolean {
        return try {
            AccountId.fromString(address).validateChecksum(client) // won't fail if there is no checksum
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun makeContextAddresses(
        walletPublicKey: ByteArray,
        curve: EllipticCurve?,
        context: Context
    ): Set<Address> {
        tryInitSharedPrefs(context)
        val address = sharedPrefs
            ?.getString(getAccountIdPrefKey(walletPublicKey), "")
            ?: ""
        return setOf(Address(address))
    }

    fun saveAddress(address: String, walletPublicKey: ByteArray) {
        sharedPrefs?.let { it.putString(getAccountIdPrefKey(walletPublicKey), address) }
    }

    private fun tryInitSharedPrefs(context: Context?) {
        context?.let { sharedPrefs = DefaultBlockchainSharedPrefs(it) }
    }

    private fun getAccountIdPrefKey(walletPublicKey: ByteArray) =
        HEDERA_ACCOUNT_ID_PREF_KEY_PREFIX + walletPublicKey.toCompressedPublicKey().toHexString()

    companion object {
        const val HEDERA_ACCOUNT_ID_PREF_KEY_PREFIX = "hedera_account_id_for_public_key_"
    }
}