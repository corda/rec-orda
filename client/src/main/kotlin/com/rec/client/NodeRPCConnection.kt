package com.rec.client

import io.bluebank.braid.core.utils.toJarsClassLoader
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.GracefulReconnect
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

@Component
class NodeRPCConnection(
        @Value("\${config.rpc.host}") private val host: String,
        @Value("\${config.rpc.port}") private val rpcPort: Int,
        @Value("\${config.rpc.username}") private val username: String,
        @Value("\${config.rpc.password}") private val password: String,
        @Value("\${config.rpc.cordapps}") private val cordapps: String
) : AutoCloseable {

    private val rpcConnection: CordaRPCConnection
    // final because of the kotlin spring plugin making everything open by default
    final val proxy: CordaRPCOps

    final val cordappClassloader: ClassLoader

    init {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val classloader = listOf(cordapps).toJarsClassLoader()
        val rpcClient = CordaRPCClient(hostAndPort = rpcAddress, classLoader = classloader)
        val gracefulReconnect = GracefulReconnect(
                { /* On Disconnect */  },
                { /* On Reconnect */ },
                3
        )
        rpcConnection = rpcClient.start(username, password, gracefulReconnect)
        proxy = rpcConnection.proxy
        cordappClassloader = classloader
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}