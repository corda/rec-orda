package com.rec.client.handlers

import com.rec.client.NodeRPCConnection
import com.rec.client.dto.*
import com.rec.flows.*
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStatesOrZero
import com.rec.flows.IssueFlows
import net.corda.core.contracts.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.*
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import rx.RxReactiveStreams.toPublisher
import rx.Single
import org.json.simple.JSONObject
import org.springframework.web.bind.annotation.CrossOrigin

@Component
class RecHandler(rpc: NodeRPCConnection) {

    private val proxy = rpc.proxy
    private val classloader = rpc.cordappClassloader

    @CrossOrigin(origins = ["*"])
    fun whoAmI(request: ServerRequest): Mono<ServerResponse> {
        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(proxy.nodeInfo().legalIdentities.first().name.toString())), ParameterizedTypeReference.forType(String::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun networkMapSnapshot(request: ServerRequest): Mono<ServerResponse> {
        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(
                        proxy.networkMapSnapshot().map { it ->
                            it.legalIdentities[0]
                        }
                )), ParameterizedTypeReference.forType(JSONObject::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun nodeDiagnostics(request: ServerRequest): Mono<ServerResponse> {
        val diagnosticInfo = proxy.nodeDiagnosticInfo()
        val info = JSONObject()
        info.put("version", diagnosticInfo.version)
        info.put("vendor", diagnosticInfo.vendor)
        info.put("cordapps", diagnosticInfo.cordapps)
        info.put("platformVersion", diagnosticInfo.platformVersion)
        info.put("revision", diagnosticInfo.revision)

        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(
                        info)), ParameterizedTypeReference.forType(JSONObject::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun networkParameters(request: ServerRequest): Mono<ServerResponse> {
        val info = JSONObject()
        info.put("notaries", proxy.networkParameters.notaries)
        info.put("minimumPlatformVersion", proxy.networkParameters.minimumPlatformVersion)
        info.put("maxTransactionSize", proxy.networkParameters.maxTransactionSize)
        info.put("maxMessageSize", proxy.networkParameters.maxMessageSize)
        return ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(
                        info)), ParameterizedTypeReference.forType(JSONObject::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun snapshot(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it, true, classloader).asSubclass(ContractState::class.java)
        var pageNumber = DEFAULT_PAGE_NUM
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val states = mutableListOf<StateAndRef<ContractState>>()

        do {
            val pageSpec = PageSpecification(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
            val results = proxy.vaultQueryByWithPagingSpec(
                    criteria = criteria, paging = pageSpec, contractStateType = clazz)
            states.addAll(results.states)
            pageNumber++
        } while ((pageSpec.pageSize * (pageNumber - 1)) <= results.totalStatesAvailable)

        ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(states.groupStates())), ParameterizedTypeReference.forType(Map::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun updates(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it, true, classloader).asSubclass(ContractState::class.java)
        ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(toPublisher(
                        proxy.vaultTrackByWithPagingSpec(clazz, QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL),PageSpecification(pageNumber = DEFAULT_PAGE_NUM, pageSize = DEFAULT_PAGE_SIZE)).updates.map { update ->
                            ContractStateUpdate(
                                    consumed = update.consumed.groupStates(),
                                    produced = update.produced.groupStates()
                            )
                        }),
                        ParameterizedTypeReference.forType(ContractStateUpdate::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun linearSnapshot(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it, true, classloader).asSubclass(LinearState::class.java)
        var pageNumber = DEFAULT_PAGE_NUM
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val states = mutableListOf<StateAndRef<LinearState>>()

        do {
            val pageSpec = PageSpecification(pageNumber = pageNumber, pageSize = DEFAULT_PAGE_SIZE)
            val results = proxy.vaultQueryByWithPagingSpec(
                    criteria = criteria, paging = pageSpec, contractStateType = clazz)
            states.addAll(results.states)
            pageNumber++
        } while ((pageSpec.pageSize * (pageNumber - 1)) <= results.totalStatesAvailable)
        ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(states.groupStates())),
                        ParameterizedTypeReference.forType(Map::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun linearUpdates(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it, true, classloader).asSubclass(LinearState::class.java)
        ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(toPublisher(
                        proxy.vaultTrackByWithPagingSpec(clazz, QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL),PageSpecification(pageNumber = DEFAULT_PAGE_NUM, pageSize = DEFAULT_PAGE_SIZE)).updates.map { update ->
                            LinearStateUpdate(
                                    consumed = update.consumed.groupLinearStates(),
                                    produced = update.produced.groupLinearStates()
                            )
                        }),
                        ParameterizedTypeReference.forType(LinearStateUpdate::class.java))
    }

    @CrossOrigin(origins = ["*"])
    fun tokenSnapshot(request: ServerRequest): Mono<ServerResponse> = ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(toPublisher(Single.just(proxy.vaultQuery(FungibleToken::class.java).states.sumTokens())),
                    ParameterizedTypeReference.forType(Map::class.java))

    @CrossOrigin(origins = ["*"])
    fun tokenUpdates(request: ServerRequest): Mono<ServerResponse> = ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(
                    toPublisher(proxy.vaultTrackByCriteria(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL)).updates.map {
                        TokenUpdate(
                                consumed = it.consumed.sumTokens(),
                                produced = it.produced.sumTokens()
                        )
                    }), ParameterizedTypeReference.forType(TokenUpdate::class.java)
            )

    // NS:
    // This POST method to Issue Tokens fails on the response: "net.corda.core.transactions.SignedTransaction cannot be cast to java.lang.CharSequence"
    // You need to change the return type to something else, like string of the transaction ID for example (this will break your flow tests probably).
    // This should be an easy fix
    @CrossOrigin(origins = ["*"])
    fun IssueTokens(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(IssueTokensDto::class.java).flatMap {
        val future = proxy.startFlowDynamic(IssueFlows.Initiator::class.java, it.holder, it.quantity, it.source).returnValue.toCompletableFuture()
        ok()
                .contentType(MediaType.APPLICATION_JSON).body(Mono.fromFuture(future), ParameterizedTypeReference.forType(String::class.java))
    }


    fun Collection<StateAndRef<FungibleToken>>.sumTokens() = map { it.state.data }
            .groupBy { it.issuedTokenType }.mapValues { it.value.sumTokenStatesOrZero(it.key) }

    fun Collection<StateAndRef<LinearState>>.groupLinearStates() = map { it.state.data.linearId to it.state.data }.toMap()

    fun Collection<StateAndRef<ContractState>>.groupStates() = map { it.ref to it.state.data }.toMap()
}

class TokenUpdate(val consumed: Map<IssuedTokenType, Amount<IssuedTokenType>>, val produced: Map<IssuedTokenType, Amount<IssuedTokenType>>)

class LinearStateUpdate(val consumed: Map<UniqueIdentifier, ContractState>, val produced: Map<UniqueIdentifier, ContractState>)

class ContractStateUpdate(val consumed: Map<StateRef, ContractState>, val produced: Map<StateRef, ContractState>)
