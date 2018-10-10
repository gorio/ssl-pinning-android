package com.wultra.android.sslpinning.plugins.powerauth

import android.content.Context
import com.wultra.android.sslpinning.CertStore
import com.wultra.android.sslpinning.CertStoreConfiguration
import com.wultra.android.sslpinning.interfaces.ECPublicKey
import io.getlime.security.powerauth.networking.ssl.PA2ClientValidationStrategy
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * @author Tomas Kypta, tomas.kypta@wultra.com
 */
fun CertStore.Companion.powerAuthCertStore(configuration: CertStoreConfiguration,
                                           context: Context,
                                           keychainIdentifier: String? = null): CertStore {
    val secureDataStore = if (keychainIdentifier == null) {
        PowerAuthSecureDataStore(context)
    } else {
        PowerAuthSecureDataStore(context, keychainIdentifier)
    }
    return CertStore(configuration = configuration,
            cryptoProvider = PowerAuthCryptoProvider(),
            secureDataStore = secureDataStore)
}

class PowerAuthSslPinningValidationStrategy(private val certStore: CertStore) : PA2ClientValidationStrategy {

    private val sslPinningTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            certStore.validateCertificate(chain[0])
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    override fun getHostnameVerifier(): HostnameVerifier? {
        return null
    }

    override fun getSSLSocketFactory(): SSLSocketFactory? {
        val trustSslPinningCerts = arrayOf(sslPinningTrustManager)
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustSslPinningCerts, null)
            return sc.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: KeyManagementException) {
            throw RuntimeException(e)
        }

    }
}