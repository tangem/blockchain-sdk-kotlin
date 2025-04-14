package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.blockchains.ethereum.eip712.EthEip712Util
import com.tangem.common.extensions.toHexString
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class EthEip712Test {

    @Test
    fun testSeaportData() {
        val jsonTypedData = getTypedDataFromFile("seaportTypedData")
        assertEquals(
            "54140d99a864932cbc40fd8a2d1d1706c3923a79c183a3b151e929ac468064db",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testAnotherTypedData() {
        val jsonTypedData = getTypedDataFromFile("hashEncode")
        assertEquals(
            "be609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testAnotherTypedData1() {
        val jsonTypedData = getTypedDataFromFile("hashEncode2")
        assertEquals(
            "55eaa6ec02f3224d30873577e9ddd069a288c16d6fb407210eecbc501fa76692",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testV4() {
        val jsonTypedData = getTypedDataFromFile("hashEncodev4")
        assertEquals(
            "f558d08ad4a7651dbc9ec028cfcb4a8e6878a249073ef4fa694f85ee95f61c0f",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testNominex() {
        val jsonTypedData = getTypedDataFromFile("Nominex")
        assertEquals(
            "9bfa080e4705a0beacb2ab710480fb1176f6de9c4117ddf50f5933d3be1ab6a1",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testRarible() {
        val jsonTypedData = getTypedDataFromFile("rarible")
        assertEquals(
            "df0200de55c05eb55af2597012767ea3af653d68000be49580f8e05acd91d366",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testCF() {
        val jsonTypedData = getTypedDataFromFile("cryptofights")
        assertEquals(
            "db12328a6d193965801548e1174936c3aa7adbe1b54b3535a3c905bd4966467c",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testWcExample() {
        val jsonTypedData = getTypedDataFromFile("WCExample")
        assertEquals(
            "abc79f527273b9e7bca1b3f1ac6ad1a8431fa6dc34ece900deabcd6969856b5e",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    @Test
    fun testSnapshonVoting() {
        val jsonTypedData = getTypedDataFromFile("snapshot")
        assertEquals(
            "bf6f1fd8ba3bf541ef77d93f4cb3b4bb43ce0f315f985c4336990dde242608b2",
            jsonTypedData.toHexString().lowercase(),
        )
    }

    private fun getTypedDataFromFile(fileName: String): ByteArray {
        return EthEip712Util.eip712Hash(readJson(fileName))
    }

    private fun readJson(fileName: String): String {
        val workingDir: Path = Paths.get("src/test/resources/eip712", "$fileName.json")
        return String(Files.readAllBytes(workingDir))
    }
}