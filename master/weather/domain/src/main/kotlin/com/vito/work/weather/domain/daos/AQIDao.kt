package com.vito.work.weather.domain.daos

import com.vito.work.weather.domain.entities.AQI
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.springframework.stereotype.Repository

/**
 * Created by lingzhiyuan.
 * Date : 16/4/17.
 * Time : 下午4:11.
 * Description:
 *
 */

@Repository
open class AQIDao : BaseDao()
{
    open fun findLatestByDistrict(districtId: Long): AQI?
    {
        var criteria = sessionFactory.currentSession.createCriteria(AQI::class.java)
        criteria.add(Restrictions.eq("district", districtId))
        criteria.addOrder(Order.desc("update_time"))
        criteria.setMaxResults(1)
        var list = criteria.list()
        if(list != null && list.size != 0){
            return list[0] as AQI
        }
        else
        {
            return AQI()
        }
    }


}