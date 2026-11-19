package ink.chyk.pass

import ink.chyk.pass.features.ecode.createQrMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmCapabilitiesTest {
    @Test
    fun qrMatrixUsesLogicalModulesInsteadOfRenderPixels() {
        val qr = assertNotNull(createQrMatrix("https://pass.neu.edu.cn"))
        assertTrue(qr.width < 100)
        assertEquals(qr.width * qr.height, qr.pixels.size)
        assertTrue(qr.pixels.any { it })
        assertTrue(qr.pixels.any { !it })
    }
}
