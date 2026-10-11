package com.afkanerd.deku.Forwarder.data

import android.net.InetAddresses
import android.os.Build
import android.util.Log
import android.util.Patterns
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL

object Network {
    data class NetworkResponseResults(val response: Response,
                                      val result: Result<String, Exception>)
    fun requestGet(url: String, headers: Headers? = null) : NetworkResponseResults{
        val (_, response, result) = if(headers.isNullOrEmpty())
            url.httpGet()
                .responseString()
        else
            url.httpGet()
                .header(headers)
                .responseString()

        return when(result) {
            is Result.Failure -> {
                NetworkResponseResults(response, Result.Failure(result.error))
            }

            is Result.Success -> {
                NetworkResponseResults(response, Result.Success(result.get()))
            }
        }
    }

    fun jsonRequestDelete(url: String, payload: String, headers: Headers? = null) :
            NetworkResponseResults {
        println("url: $url")
        val (_, response, result) = if(headers.isNullOrEmpty())
            Fuel.delete(url)
                .jsonBody(payload)
                .responseString()
        else
            Fuel.delete(url)
                .jsonBody(payload)
                .header(headers)
                .responseString()

        return when(result) {
            is Result.Failure -> {
                Log.w(javaClass.name, "Response text - ${String(response.data)}")
                NetworkResponseResults(response, Result.Failure(result.error))
            }

            is Result.Success -> {
                NetworkResponseResults(response, Result.Success(result.get()))
            }
        }
    }
    fun jsonRequestPut(url: String, payload: String, headers: Headers? = null) :
            NetworkResponseResults {
        println("url: $url")
        val (_, response, result) = if(headers.isNullOrEmpty())
            Fuel.put(url)
                .jsonBody(payload)
                .responseString()
        else
            Fuel.put(url)
                .jsonBody(payload)
                .header(headers)
                .responseString()

        return when(result) {
            is Result.Failure -> {
                Log.w(javaClass.name, "Response text - ${String(response.data)}")
                NetworkResponseResults(response, Result.Failure(result.error))
            }

            is Result.Success -> {
                NetworkResponseResults(response, Result.Success(result.get()))
            }
        }
    }

    fun jsonRequestPost(url: String, payload: String, headers: Headers? = null) : NetworkResponseResults {
        val (_, response, result) = if(headers.isNullOrEmpty())
            Fuel.post(url)
                .jsonBody(payload)
                .responseString()
        else
            Fuel.post(url)
                .jsonBody(payload)
                .header(headers)
                .responseString()


        return when(result) {
            is Result.Failure -> {
                NetworkResponseResults(response, Result.Failure(result.error))
            }

            is Result.Success -> {
                NetworkResponseResults(response, Result.Success(result.get()))
            }
        }
    }

    fun isUrlAnIpAddress(urlString: String): Boolean {
        val protocolRegex = "^[a-zA-Z][a-zA-Z0-9+.-]*://".toRegex()
        return !urlString.contains(protocolRegex) && try {
            val parsingUrl = if (!urlString.contains("://")) "http://$urlString" else urlString
            val url = URL(parsingUrl)
            val host = url.host

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                InetAddresses.isNumericAddress(host)
            } else {
                Patterns.IP_ADDRESS.matcher(host).matches()
            }
        } catch (e: MalformedURLException) {
            false
        }
    }
}