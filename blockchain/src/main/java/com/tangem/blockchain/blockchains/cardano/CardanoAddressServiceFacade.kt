package com.tangem.blockchain.blockchains.cardano

import com.tangem.blockchain.blockchains.cardano.utils.CardanoContractAddressRecognizer
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.Address
import com.tangem.blockchain.common.address.AddressService
import com.tangem.blockchain.common.address.ContractAddressValidator
import com.tangem.blockchain.common.address.WalletCoreAddressService
import com.tangem.common.card.EllipticCurve

internal class CardanoAddressServiceFacade : AddressService(), ContractAddressValidator {

    private val legacyService = CardanoAddressService(Blockchain.Cardano)
    private val trustWalletService = WalletCoreAddressService(Blockchain.Cardano)

    override fun makeAddress(walletPublicKey: ByteArray, curve: EllipticCurve?): String {
        return if (CardanoUtils.isExtendedPublicKey(walletPublicKey)) {
            trustWalletService.makeAddress(walletPublicKey, curve)
        } else {
            legacyService.makeAddress(walletPublicKey, curve)
        }
    }

    override fun validate(address: String): Boolean {
        return trustWalletService.validate(address) || legacyService.validate(address)
    }

    override fun makeAddresses(walletPublicKey: ByteArray, curve: EllipticCurve?): Set<Address> {
        return if (CardanoUtils.isExtendedPublicKey(walletPublicKey)) {
            trustWalletService.makeAddresses(walletPublicKey, curve)
        } else {
            legacyService.makeAddresses(walletPublicKey, curve)
        }
    }

    /**
     * Validates the given contract [address] to ensure it adheres to the Cardano Asset Fingerprint standard.
     *
     * The validation process follows these steps:
     * 1. Check if the address is a valid Asset Fingerprint:
     *    - Attempt to validate/decode the address using a library.
     *    - Ensure the format and checksum are correct.
     *    - If invalid, proceed to step 2.
     * 2. Verify the prefix:
     *    - The prefix should be "asset".
     *    - If the only issue is with the prefix, throw an error.
     *    - If the prefix is correct, add the address as is.
     * 3. Validate the hexadecimal format:
     *    - If valid, proceed to the next step.
     *    - If invalid, throw an error.
     * 4. Check the length of the hexadecimal string:
     *    - If the length is greater than 56 characters, it is an Asset ID. Proceed to step 5.
     *    - If the length is exactly 56 characters, it is a Policy ID:
     *      - Convert the token symbol to hexadecimal (ASCII or UTF-8, same result for Latin characters).
     *      - Concatenate the Policy ID with the hexadecimal symbol to form the Asset ID.
     *      - Proceed to step 5.
     *    - If the length is less than 56 characters, throw an error.
     * 5. Convert the Asset ID to an Asset Fingerprint and save it.
     *
     * For more information, refer to:
     * - [Cardano Token Bundles](https://cardano-ledger.readthedocs.io/en/latest/explanations/token-bundles.html)
     * - [Asset Fingerprint CIP-14](https://cips.cardano.org/cip/CIP-14)
     *
     * @param address The contract address to validate.
     * @return True if the address is valid, false otherwise.
     */
    override fun validateContractAddress(address: String): Boolean {
        return CardanoContractAddressRecognizer.recognize(address) != null
    }
}