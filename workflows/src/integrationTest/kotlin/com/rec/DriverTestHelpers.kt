package com.rec

import com.rec.flows.FlowTestHelpers
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.NotarySpec
import net.corda.testing.node.User
import java.util.concurrent.Future

class DriverTestHelpers {
    companion object {

        infix fun TestIdentity.withUsers(users: List<User>): Pair<TestIdentity, List<User>> = this to users

        // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
        fun withDriver(test: DriverDSL.() -> Unit) = driver(
                DriverParameters(
                        isDebug = true,
                        startNodesInProcess = true,
                        networkParameters = FlowTestHelpers.prepareMockNetworkParameters.networkParameters,
                        cordappsForAllNodes = FlowTestHelpers.prepareMockNetworkParameters.cordappsForAllNodes,
                        notarySpecs = FlowTestHelpers.prepareMockNetworkParameters.notarySpecs.map { NotarySpec(it.name) }
                )
        ) { test() }

        fun DriverDSL.startNodes(vararg identities: Pair<TestIdentity, List<User>>): List<NodeHandle> =
                identities
                .map { startNode(providedName = it.first.name, rpcUsers = it.second) }
                .waitForAll()

        private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

        fun NodeHandle.getProxy(username: String, password: String):  CordaRPCOps =
                CordaRPCClient(this.rpcAddress).start(username, password).proxy

    }
}