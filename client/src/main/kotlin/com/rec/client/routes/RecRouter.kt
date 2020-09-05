package com.rec.client.routes

import com.rec.client.handlers.RecHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class RecRouter {
    @CrossOrigin(origins = ["*"])
    @Bean
    fun router(handler: RecHandler): RouterFunction<ServerResponse> = router {
        ("/me").nest {
            GET("/", handler::whoAmI)
        }
        ("/info/").nest {
            GET("/networkMapSnapshot", handler::networkMapSnapshot)
            GET("/nodeInfo", handler::nodeDiagnostics)
            GET("/networkParameters", handler::networkParameters)
        }
        ("/issue").nest {
            POST("/", handler::IssueTokens)
        }
        ("/states").nest {
            accept(MediaType.APPLICATION_JSON).nest { POST("/", handler::snapshot) }
            accept(MediaType.TEXT_EVENT_STREAM).nest { POST("/updates", handler::updates) }
        }
        ("/linear").nest {
            accept(MediaType.APPLICATION_JSON).nest { POST("/", handler::linearSnapshot) }
            accept(MediaType.TEXT_EVENT_STREAM).nest { POST("/updates", handler::linearUpdates) }
        }
        ("/tokens").nest {
            accept(MediaType.APPLICATION_JSON).nest { POST("/", handler::tokenSnapshot) }
            accept(MediaType.TEXT_EVENT_STREAM).nest { POST("/updates", handler::tokenUpdates) }
        }
    }
}
