package com.dloren.mispantallas.data.mapper

import com.dloren.mispantallas.data.local.AccountEntity
import com.dloren.mispantallas.domain.model.Account

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
    startDateMillis = startDateMillis
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
    startDateMillis = startDateMillis
)
