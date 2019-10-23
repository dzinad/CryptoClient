package by.bsu.ddzina.lab2

import java.security.Key

const val privateKeyFileName = "key.txt"
const val privateKeyMetaFileName = "key_meta.txt"
const val jsonKeyModulus = "modulus"
const val jsonKeyExponent = "exponent"
const val jsonKeyIv = "iv"
const val jsonKeyExtraSymbols = "extra_symbols"
const val jsonKeyError = "error"
const val jsonKeyContent = "content"
const val jsonValueSessionKeyExpired = "session_key_expired"
const val jsonValueNoFile = "no_file"
const val keyUsername = "username"
const val preferencesName = "com.bsu.ddzina.lab2.preferences"

var sessionKey: Key? = null

