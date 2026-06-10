package com.dloren.mispantallas.data.backup

import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializa/deserializa la lista de cuentas a JSON para el respaldo.
 * Formato simple y estable: un objeto con "version" y un arreglo "accounts".
 */
object BackupCodec {

    private const val VERSION = 1

    fun toJson(accounts: List<Account>): String {
        val arr = JSONArray()
        accounts.forEach { a ->
            val o = JSONObject()
            o.put("email", a.email)
            o.put("password", a.password)
            o.put("profileName", a.profileName)
            o.put("pin", a.pin)
            o.put("platform", a.platform)
            o.put("clientPhone", a.clientPhone)
            o.put("durationDays", a.durationDays)
            o.put("startDateMillis", a.startDateMillis)
            o.put("status", a.status.name)
            o.put("soldDateMillis", a.soldDateMillis)
            o.put("renewEveryDays", a.renewEveryDays)
            o.put("providerStartMillis", a.providerStartMillis)
            arr.put(o)
        }
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("accounts", arr)
        return root.toString(2)
    }

    /** Devuelve las cuentas del JSON (con id = 0 para insertarlas como nuevas). */
    fun fromJson(json: String): List<Account> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("accounts") ?: JSONArray()
        val result = mutableListOf<Account>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                Account(
                    id = 0L,
                    email = o.optString("email"),
                    password = o.optString("password"),
                    profileName = o.optString("profileName"),
                    pin = o.optString("pin"),
                    platform = o.optString("platform"),
                    clientPhone = o.optString("clientPhone"),
                    durationDays = o.optInt("durationDays", 30),
                    startDateMillis = o.optLong("startDateMillis", System.currentTimeMillis()),
                    status = runCatching {
                        AccountStatus.valueOf(o.optString("status", "NOT_SOLD"))
                    }.getOrDefault(AccountStatus.NOT_SOLD),
                    soldDateMillis = o.optLong("soldDateMillis", 0L),
                    renewEveryDays = o.optInt("renewEveryDays", 0),
                    providerStartMillis = o.optLong(
                        "providerStartMillis",
                        System.currentTimeMillis()
                    )
                )
            )
        }
        return result
    }
}
