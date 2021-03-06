package com.vito.work.weather.admin.controllers

import com.vito.work.weather.domain.config.SpiderStatus
import com.vito.work.weather.domain.services.HourWeatherService
import com.vito.work.weather.domain.services.LocationService
import com.vito.work.weather.domain.util.http.BusinessError
import com.vito.work.weather.domain.util.http.BusinessException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import us.codecraft.webmagic.scheduler.QueueScheduler

/**
 * Created by lingzhiyuan.
 * Date : 16/4/19.
 * Time : 下午2:57.
 * Description:
 *
 */

@Controller
@RequestMapping("/weather/today")
open class TodayWeatherController @Autowired constructor(val hourWeatherService: HourWeatherService, val locationService: LocationService)
{

    @RequestMapping("/spider/update")
    @ResponseBody
    open fun updateFromWeb()
    {

        if (SpiderStatus.TODAY_WEATHER_STATUS == true)
        {
            throw BusinessException(BusinessError.ERROR_TODAY_WEATHER_UPDATING)
        }
        // 重置 Scheduler （清空Scheduler内已爬取的 url）
        HourWeatherService.spider.scheduler = QueueScheduler()

        val cities = locationService.findCities() ?: listOf()
        val distrcts = locationService.findDistricts() ?: listOf()

        distrcts.forEach { district ->
            try
            {
                hourWeatherService.updateFromWeb(cities.first { it.id == district.city }, district)
            }
            catch(ex: Exception)
            {
                ex.printStackTrace()
                // 忽略错误
            }
        }
    }

}