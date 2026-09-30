package com.billforce.owner.data.model

data class DbConfig(
    val dbFilePath: String = "",
    val shopName: String = "",
    val ownerPin: String = "",        // 4-digit PIN set by owner
    val isBiometricEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val lastConnectedAt: Long = 0L
)
