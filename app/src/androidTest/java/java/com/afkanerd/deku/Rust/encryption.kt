package java.com.afkanerd.deku.Rust

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afkanerd.deku.DefaultSMS.data.Cryptography
import com.afkanerd.deku.rust.RustyRatchetBridge
import com.afkanerd.smswithoutborders_libsmsmms.extensions.context.generateSecureRandom
import org.junit.Test
import org.junit.runner.RunWith
import org.spongycastle.asn1.x500.style.RFC4519Style.l
import kotlin.io.encoding.Base64


@RunWith(AndroidJUnit4::class)
class EncryptionTest {

    @Test
    fun simpleRatchet() {
        var aliceState = RustyRatchetBridge.newState()
        var bobState = RustyRatchetBridge.newState()

        val sk = Cryptography.generateSecureRandom(32)

        val alicePrivate = "f8d7ef06f5b7c38d00d5d43b1d528af4621b1bcce3cddcf5c03ed36e3d8eb943".hexToByteArray()
        val alicePublic = "bae06b517b1de9675df982630aade00e9572ed3cce6f2a4806c736f24ea09c5d".hexToByteArray()

        val bobPrivate = "b02c292ba617a92893531d7fc6b59d99cd8f1f7957113430c6fe66c39f26b55c".hexToByteArray()
        val bobPublic = "5322d992ca856162ced81a364614191b090159563b74f317978da4acc96d1800".hexToByteArray()

        aliceState = RustyRatchetBridge.initAlice(
            state = aliceState,
            sk = sk,
            bob_dh_public_key = bobPublic
        )
//
//        bobState = RustyRatchetBridge.initBob(
//            state = bobState,
//            sk = sk,
//            bob_keypair = bobKeypair
//        )
//
//        val ad = "Hello Bob".toByteArray()
//        val (newAliceState, ciphertext, header) = RustyRatchetBridge.encrypt(
//            state = aliceState,
//            plaintextText = "Hello Bob".toByteArray(),
//            ad = ad
//        )
//        aliceState = newAliceState
//
//        val (newBobState, payload) = RustyRatchetBridge.decrypt(
//            state = bobState,
//            header = header,
//            ciphertext = ciphertext,
//            ad = ad
//        )
//        bobState = newBobState
    }
}