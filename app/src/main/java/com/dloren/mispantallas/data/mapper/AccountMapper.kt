package com.dloren.mispantallas.data.mapper

import com.dloren.mispantallas.data.local.AccountEntity
import com.dloren.mispantallas.domain.model.Account
import com.dloren.mispantallas.domain.model.AccountStatus

/** Conversión entre el modelo de persistencia (Room) y el modelo de dominio. */

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    email = email,
    password = password,
    profileName = profileName,
    pin = pin,
    platform = platform,
    clientPhone = clientPhone,
    durationDays = durationDays,
    startDateMillis = startDateMillis,
    status = runCatching { AccountStatus.valueOf(status) }.getOrDefault(AccountStatus.NOT_SOLD),
    soldDateMillis = soldDateMillis,
    renewEveryDays = renewEveryDays,
    providerStartMillis = providerStartMillis
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    email = email,
    password = password,
    profileName = profileName,
    pin = pin,
    platform = platform,
    clientPhone = clientPhone,
    durationDays = durationDays,
    startDateMillis = startDateMillis,
    status = status.name,
    soldDateMillis = soldDateMillis,
    renewEveryDays = renewEveryDays,
    providerStartMillis = providerStartMillis
)
