package by.bsu.ddzina.lab2

import java.security.Key
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

const val privateKeyFileName = "key.txt"
const val tokenFileName = "token.txt"
const val privateKeyMetaFileName = "key_meta.txt"
const val jsonKeyModulus = "modulus"
const val jsonKeyExponent = "exponent"
const val jsonKeyIv = "iv"
const val jsonKeyExtraSymbols = "extra_symbols"
const val jsonKeyError = "error"
const val jsonKeyContent = "content"
const val jsonValueSessionKeyExpired = "session_key_expired"
const val jsonValueNoFile = "no_file"
const val jsonValueAuthorizationError = "authorization_error"
const val keyUsername = "username"
const val preferencesName = "com.bsu.ddzina.lab2.preferences"
const val jsonKeyFileName = "filename"
const val jsonKeyFileContent = "file_content"
const val tokenParam = "token"
const val jsonKeyRequestCode = "request_code"
const val jsonKeyVerificationCode = "ver_code"
const val keyResult = "result"
const val keySessionId = "session_id"

@Volatile var privateKey: RSAPrivateKey? = null
var publicKey: RSAPublicKey? = null
var sessionKey: Key? = null
var token: String? = null
var secretRequestCode: String? = null
var sessionId: String? = null

