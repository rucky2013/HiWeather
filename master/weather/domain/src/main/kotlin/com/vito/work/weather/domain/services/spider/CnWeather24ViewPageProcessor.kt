package com.vito.work.weather.domain.services.spider

import org.slf4j.LoggerFactory
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.processor.PageProcessor

/**
 * Created by lingzhiyuan.
 * Date : 16/4/16.
 * Time : 下午12:54.
 * Description:
 *
 */

open class CnWeather24ViewPageProcessor : PageProcessor
{
    companion object{
        val logger = LoggerFactory.getLogger(CnWeather24ViewPageProcessor::class.java)
    }

    private var site: Site = Site.me().setSleepTime(5).setRetryTimes(5).setCycleRetryTimes(3)

    override fun getSite(): Site?
    {
        return site
    }

    override fun process(page: Page?)
    {
        page?.putField("ptime", page.html?.xpath("//sktq/@ptime"))

        var hours = page?.html?.xpath("//sktq/qw/@h")?.all()
        var temps = page?.html?.xpath("//sktq/qw/@wd")?.all()
        var wds = page?.html?.xpath("//sktq/qw/@fx")?.all()
        var wfs = page?.html?.xpath("//sktq/qw/@fl")?.all()
        var preds = page?.html?.xpath("//sktq/qw/@js")?.all()
        var humis = page?.html?.xpath("//sktq/qw/@sd")?.all()

        page?.putField("hours", hours)
        page?.putField("temps", temps)
        page?.putField("wds", wds)
        page?.putField("wfs", wfs)
        page?.putField("preds", preds)
        page?.putField("humis", humis)
    }
}