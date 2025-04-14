package com.tangem.blockchain.common

/**
 * Interface representing an entity that can check the initialization status of an account.
 *
 * Implementing classes should use this interface to indicate and track
 * whether an account has been properly initialized before performing operations on it.
 */
interface InitializableAccount {

    /**
     * Represents the current initialization state of the account.
     *
     * This property returns one of the predefined states:
     * - [State.INITIALIZED] if the account is fully set up.
     * - [State.NOT_INITIALIZED] if the setup is incomplete.
     * - [State.UNDEFINED] if the initialization state is unknown.
     */
    val accountInitializationState: State

    /**
     * Enumeration defining possible account initialization states.
     */
    enum class State {
        /** The account is fully initialized and ready for use. */
        INITIALIZED,

        /** The account setup is incomplete. */
        NOT_INITIALIZED,

        /** The initialization status of the account is unknown. */
        UNDEFINED,
    }
}