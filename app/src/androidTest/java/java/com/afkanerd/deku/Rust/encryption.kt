package java.com.afkanerd.deku.Rust

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afkanerd.deku.rust.RustyRatchetBridge
import org.junit.Test
import org.junit.runner.RunWith
import org.spongycastle.asn1.x500.style.RFC4519Style.l


@RunWith(AndroidJUnit4::class)
class EncryptionTest {

    @Test
    fun simpleRatchet() {
        val statePnt = RustyRatchetBridge.newState()
        val sk = ByteArray(32)
        val bobPublicKey = ByteArray(32)

        val newStatePnt = RustyRatchetBridge.initAlice(
            handle = statePnt,
            sk = sk,
            bob_dh_public_key = bobPublicKey
        )
    }
}