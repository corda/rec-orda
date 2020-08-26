package com.rec.contracts

import com.r3.corda.lib.tokens.contracts.FungibleTokenContract.Companion.contractId
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.amount
import com.rec.states.EnergySource
import com.rec.states.RECToken
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Test

//class REContractTest {
//
//    private val ledgerServices = MockServices(listOf("com.r3.corda.lib.tokens.contracts"))
//    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB")).party
//    private val bob = TestIdentity(CordaX500Name("Bob", "New York", "US")).party
//    private val carly = TestIdentity(CordaX500Name("Carly", "New York", "US")).party

//    @Test
//    fun transactionMustIncludeTheAttachment() {
//        ledgerServices.transaction {
//            output(contractId, createFrom(alice, bob, 10L, EnergySource.values()[0]))
//        }


//        transaction(ledgerServices, { tx: TransactionDSL<TransactionDSLInterpreter?> ->
//            tx.output(contractId, create(aliceMile, bob, 10L))
//            tx.command(alice.owningKey, IssueTokenCommand(aliceMile, listOf(0)))
//            tx.failsWith("Contract verification failed: Expected to find type jar")
//            tx.attachment("com.template.contracts", AirMileType.getContractAttachment())
//            tx.verifies()
//            null
//        })
//    }
//}

//fun createFrom(issuer: Party, holder: Party, quantity: Long, source: EnergySource) = FungibleToken(
//        amount = amount(quantity, IssuedTokenType(issuer, RECToken(source))),
//        holder = holder
//)