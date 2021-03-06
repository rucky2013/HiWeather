package com.vito.work.weather.domain.util.cnweather

import com.vito.work.weather.domain.util.http.sendGetRequestViaHttpClient
import org.slf4j.LoggerFactory
import java.nio.charset.Charset

/**
 * Created by lingzhiyuan.
 * Date : 16/4/21.
 * Time : 下午11:10.
 * Description:
 *
 */

class HackedAPI{
    companion object{
        val logger = LoggerFactory.getLogger(HackedAPI::class.java)
    }
}

val CNWEATHER_API_BASE_URL_NOW = "http://d1.weather.com.cn/sk_2d/districtId.html"
val CNWEATHER_API_REFERER_BASE_URL = "http://www.weather.com.cn/weather1d/districtId.shtml"

private fun UrlBuilder(baseUrl: String, districtId: String): String
{

    var urlBuffer: StringBuffer = StringBuffer()

    with(urlBuffer){
        if (! baseUrl.startsWith("http://") && ! baseUrl.startsWith("https://"))
        {
            append("http://")
        }
        append(baseUrl.replace("districtId", districtId))
    }

    return urlBuffer.toString()
}

fun invokeAPI(districtId: String): String
{
    var targetUrl = UrlBuilder(CNWEATHER_API_BASE_URL_NOW,districtId)
    var refererUrl = UrlBuilder(CNWEATHER_API_REFERER_BASE_URL,districtId)
    HackedAPI.logger.info("Invoking API: $targetUrl")
    var result = sendGetRequestViaHttpClient(targetUrl, hashMapOf(), headers = hashMapOf("Referer" to refererUrl), charset = Charset.forName("utf-8"))
    HackedAPI.logger.info("Received Data: $result")
    return result ?: ""
}