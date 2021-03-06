package com.vito.work.weather.domain.services

import com.vito.work.weather.domain.config.Constant
import com.vito.work.weather.domain.config.getAQITypeCodeByName
import com.vito.work.weather.domain.daos.*
import com.vito.work.weather.domain.entities.AQI
import com.vito.work.weather.domain.entities.District
import com.vito.work.weather.domain.entities.Station
import com.vito.work.weather.domain.entities.StationAQI
import com.vito.work.weather.domain.services.spider.AQIViewPageProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import us.codecraft.webmagic.ResultItems
import us.codecraft.webmagic.Spider
import us.codecraft.webmagic.selector.PlainText
import java.lang.Integer.parseInt
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.annotation.PreDestroy
import javax.transaction.Transactional

/**
 * Created by lingzhiyuan.
 * Date : 16/4/17.
 * Time : 下午4:15.
 * Description:
 *
 */

@Service
@Transactional
open class AQIService @Autowired constructor(val aqiDao: AQIDao, val locationDao: LocationDao, val urlDao: UrlDao, val stationDao: StationDao, val stationAQIDao: StationAQIDao)
{

    @PreDestroy
    open fun destroy()
    {
        spider.close()
    }

    companion object
    {
        var spider: Spider = Spider.create(AQIViewPageProcessor())
                .thread(Constant.SPIDER_THREAD_COUNT)

        val logger = LoggerFactory.getLogger(AQIService::class.java)
    }

    open fun findLatestAQI(districtId: Long): AQI?
    {
        var result = aqiDao.findLatestByDistrict(districtId)
        return result
    }


    open fun updateAQI(district: District): WebData
    {
        val targetUrl = AQIViewUrlBuilder(Constant.AQI_BASE_URL, district.pinyin_aqi)
        try
        {
            var webData = fetchDataViaSpider(targetUrl, district)
            saveWebdata(webData)
            return webData
        }
        catch(ex: Exception)
        {
            throw ex
        }
    }

    /**
     * 保存天气预报的 list
     * @param weathers      待保存的所有天气
     * @param district      待保存的天气所属的区县
     *
     * 保存前需要先查出数据库中是否有对应的天气, 根据区县和日期判断是否存在旧项
     *  若有, 则更新旧项
     *  没有, 则保存为新项
     * */
    open fun saveWebdata(webData: WebData)
    {

        val aqi = webData.aqi
        val stationAQIs = webData.stationAQIs
        val stations = webData.stations

        val stationNames = mutableListOf<String>()
        stations.forEach { stationNames.add(it.name_zh) }

        var savedStations = stationDao.findByNames(stationNames) ?: mutableListOf()

        // 不存在则保存
        stations.forEach { iw ->
            var station = savedStations.firstOrNull { iw.name_zh == it.name_zh }
            if (station == null)
            {
                savedStations.add(iw)
            }
            else
            {
                station.district = iw.district
                station.name_en = iw.name_en
                station.name_zh = iw.name_zh
                savedStations.add(station)
            }
        }

        aqiDao.saveOrUpdate(aqi)

        savedStations.forEach { stationDao.saveOrUpdate(it) }

        stationAQIs.forEach { iw -> iw.station = savedStations.firstOrNull { iw.station_name == it.name_zh }?.id ?: 0L }

        stationAQIs.forEach { stationAQIDao.saveOrUpdate(it) }

    }

    open fun findStationAQI(districtId: Long): List<StationAQI>?
    {
        var stations = stationDao.findByDistrict(districtId) ?: mutableListOf()
        var ids = mutableListOf<Long>()
        stations.forEach { ids.add(it.id) }
        var result = stationAQIDao.findLatestByStations(ids)
        result?.forEach { id -> stations.forEach { if (it.id == id.station) id.station_name = it.name_zh } }
        return result
    }

}

/**
 * URL Builder
 * */
private fun AQIViewUrlBuilder(baseUrl: String, districtPinyin: String): String
{

    val urlSeparator = "/"
    val urlSuffix = ".html"

    var urlBuffer: StringBuffer = StringBuffer()
    if (! baseUrl.startsWith("http://") && ! baseUrl.startsWith("https://"))
    {
        urlBuffer.append("http://")
    }

    urlBuffer.append(baseUrl)
    urlBuffer.append(districtPinyin)
    urlBuffer.append(urlSuffix)

    return urlBuffer.toString()
}

private fun fetchDataViaSpider(targetUrl: String, district: District): WebData
{

    var resultItems: ResultItems = ResultItems()
    var stationAqis = mutableListOf<StationAQI>()
    var aqi = AQI()
    var stations = mutableListOf<Station>()

    try
    {
        resultItems = AQIService.spider.get(targetUrl)
    }
    catch(ex: Exception)
    {
        throw ex
    }

    if (resultItems.request == null)
    {
        throw Exception("Request Can't Be Null")
    }

    with(resultItems) {

        var value: PlainText = get("aqi_value")

        aqi.district = district.id
        aqi.value = parseIntegerData(value.toString())
        aqi.date = Date.valueOf(LocalDate.now())
        aqi.update_time = Timestamp.valueOf(LocalDateTime.now())

        var stationNames: List<String> = get("stations")
        var station_urls: List<String> = get("station_urls")
        var station_values: List<String> = get("station_values")
        var station_pm25: List<String> = get("station_pm25")
        var station_o3: List<String> = get("station_o3")
        var station_primary: List<String> = get("station_primary")

        for ((index, stationName) in stationNames.withIndex())
        {
            val stationAQI = StationAQI()
            val station = Station()
            with(station) {
                name_zh = stationName.toString()
                name_en = station_urls[index].toString()
                this.district = district.id
            }
            stations.add(station)

            with(stationAQI) {
                station_name = stationName.toString()
                this.value = parseIntegerData(station_values[index])
                PM25 = parseIntegerData(station_pm25[index])
                O3 = parseIntegerData(station_o3[index])
                major = getAQITypeCodeByName(station_primary[index].toString()).code
                datetime = Timestamp.valueOf(LocalDateTime.now())
            }
            stationAqis.add(stationAQI)
        }

    }

    val result = WebData(stationAqis, aqi, stations)

    return result
}

private fun parseIntegerData(data: String): Int
{
    try
    {
        return parseInt(data.split("：").last().replace("μg/m3", "").trim())
    }
    catch(ex: Exception)
    {
        return - 1
    }
}

data class WebData(
        val stationAQIs: List<StationAQI>,
        val aqi: AQI,
        val stations: List<Station>
                  )