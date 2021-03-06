package com.vito.work.weather.admin.controllers

import com.vito.work.weather.domain.config.SpiderStatus
import com.vito.work.weather.domain.entities.City
import com.vito.work.weather.domain.entities.Province
import com.vito.work.weather.domain.services.LocationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import us.codecraft.webmagic.scheduler.QueueScheduler

/**
 * Created by lingzhiyuan.
 * Date : 16/4/11.
 * Time : 下午1:05.
 * Description:
 *
 */

@Controller
@RequestMapping("/location")
open class LocationController @Autowired constructor(val locationService: LocationService)
{
    @RequestMapping("/")
    open fun toManage(): String
    {
        return "location/index"
    }

    /**
     * 获取所有省信息
     * */
    @RequestMapping("/provinces")
    @ResponseBody
    fun getProvinces(): List<Province>?
    {
        return locationService.findProvinces()
    }

    /**
     * 获取所有市信息
     * */
    @RequestMapping("/cities")
    @ResponseBody
    fun getCitiesByProvinceId(@RequestParam(required = false, defaultValue = "0")provinceId: Long): List<City?>?
    {
        return locationService.findCities(provinceId)
    }

    /**
     * 获取所有区县信息
     * */
    @RequestMapping("/districts")
    @ResponseBody
    fun getDistrictByCityId(@RequestParam(required = false, defaultValue = "0") cityId: Long): List<Any?>?
            = locationService.findDistricts(cityId)

    /**
     * 从网上更新所有区域信息
     * */
    @RequestMapping("/spider/update")
    @ResponseBody
    fun updateLocations(): Any
    {
        if(SpiderStatus.LOCATION_UPDATE_STATUS == true)
        {
            return false
        }

        LocationService.spider.scheduler = QueueScheduler()

        locationService.updateAllLocationsFromWeb()

        return true
    }

}