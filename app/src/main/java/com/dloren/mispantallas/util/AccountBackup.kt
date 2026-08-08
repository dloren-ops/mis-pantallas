package com.dloren.mispantallas.util

import com.dloren.mispantallas.data.Account
import org.json.JSONArray
import org.json.JSONObject

/** Respaldo local de cuentas en un archivo JSON (exportar/importar desde el teléfono). */
object AccountBackup {

    fun toJson(accounts: List<Account>): String {
        val array = JSONArray()
        accounts.forEach { account ->
            val obj = JSONObject()
            obj.put("email", account.email)
            obj.put("password", account.password)
            obj.put("profileName", account.profileName)
            obj.put("pin", account.pin)
            obj.put("platform", account.platform)
            obj.put("clientPhone", account.clientPhone)
            obj.put("durationDays", account.durationDays)
            obj.put("startDateMillis", account.startDateMillis)
            obj.put("status", account.status)
            obj.put("soldDateMillis", account.soldDateMillis)
            obj.put("renewEveryDays", account.renewEveryDays)
            obj.put("providerStartMillis", account.providerStartMillis)
            obj.put("price", account.price)
            obj.put("emailChangeNoticePending", account.emailChangeNoticePending)
            array.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("accounts", array)
        return root.toString(2)
    }

    /**
     * Acepta tanto el formato actual (`{"version":1,"accounts":[...]}`) como una lista
     * JSON plana (`[...]`), para poder leer respaldos viejos o de otras instalaciones.
     */
    fun fromJson(json: String): List<Account> {
        val trimmed = json.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            JSONObject(trimmed).getJSONArray("accounts")
        }
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Account(
                email = obj.optString("email"),
                password = obj.optString("password"),
                profileName = obj.optString("profileName"),
                pin = obj.optString("pin"),
                platform = obj.optString("platform"),
                clientPhone = obj.optString("clientPhone"),
                durationDays = obj.optInt("durationDays", 30),
                startDateMillis = obj.optLong("startDateMillis", System.currentTimeMillis()),
                status = obj.optString("status", "SOLD"),
                soldDateMillis = obj.optLong("soldDateMillis", 0L),
                renewEveryDays = obj.optInt("renewEveryDays", 0),
                providerStartMillis = obj.optLong("providerStartMillis", 0L),
                price = obj.optDouble("price", 0.0),
                emailChangeNoticePending = obj.optBoolean("emailChangeNoticePending", false)
            )
        }
    }
}
