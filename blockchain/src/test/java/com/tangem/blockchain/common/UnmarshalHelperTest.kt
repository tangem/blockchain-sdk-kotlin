package com.tangem.blockchain.common

import com.google.common.truth.Truth
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

class UnmarshalHelperTest {

    @Test
    fun unmarshalSignatureExtended() {
        val signature =
            "15259D8689ECFF88B8F96BBFE722F06DAADD5C5E3D49E949D542532A34D2B64631AD6ECBDA05E1CDD59F07BA92E72A033BFE1A595265F2031828F05002963257".hexToBytes()
        val hash = "00A6B4160A26E386B648C82F6CC9966B86606ED553BA18BC89D885B77D3924E2".hexToBytes()
        val publicKey =
            "04EC892B365507F003B81C8B385C1E17E8B9AD86CCCD56A8646BF869363C66E6424F6143BC59A3D2632293AC269A9A94ABCCC78904A76D494F42C1157561E1F98F".hexToBytes()

        val result = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = publicKey,
        )

        Truth.assertThat(result.asRSV())
            .isEqualTo(
                "15259D8689ECFF88B8F96BBFE722F06DAADD5C5E3D49E949D542532A34D2B64631AD6ECBDA05E1CDD59F07BA92E72A033BFE1A595265F2031828F0500296325701".hexToBytes(),
            )

        Truth.assertThat(result.asRSVLegacyEVM())
            .isEqualTo(
                "15259D8689ECFF88B8F96BBFE722F06DAADD5C5E3D49E949D542532A34D2B64631AD6ECBDA05E1CDD59F07BA92E72A033BFE1A595265F2031828F050029632571C".hexToBytes(),
            )
    }
}