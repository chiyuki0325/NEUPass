package ink.chyk.pass.api

// 各平台服务的统一认证回调地址
data class CampusApp internal constructor(
  val name: String,
  val callbackUrl: String,
) {
  companion object {
    // 一码通
    val ECODE = CampusApp("ECODE", "https://ecode.neu.edu.cn/ecode/api/sso/login")

    // 一卡通自助查询平台
    val ECARD = CampusApp("ECARD", "http://ecard.neu.edu.cn/selflogin/login.aspx")

    // 门户
    val PORTAL = CampusApp(
      "PORTAL",
      "https://personal.neu.edu.cn/portal/manage/common/cas_login/1?redirect=https%3A%2F%2Fpersonal.neu.edu.cn%2Fportal",
    )

    // 缴费服务大厅
    val PAY_HALL = CampusApp("PAY_HALL", "https://pay.neu.edu.cn/drCasLogin")

    // 校园乐跑
    val CAMPUS_RUN = CampusApp(
      "CAMPUS_RUN",
      "https://tybzhtypt.neu.edu.cn/bdlp_h5_fitness_test/public/index.php/index/login/neuLogin",
    )

    // 教务系统
    val JWAPP = CampusApp("JWAPP", "https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do")

    // 更多应用
    val XG = CampusApp(
      "XG",
      "https://xg.neu.edu.cn/xsfw/sys/xggzptapp/*default/index.do?min=1#/gzzm",
    )
    val PAYMENT_QUERY = CampusApp(
      "PAYMENT_QUERY",
      "http://hub.17wanxiao.com/cas-dongbei/cas/cjgy/light.action?flag=dongbei_dongbeidaxue&amp;ecardFunc=index",
    )
    val BLACKBOARD = CampusApp("BLACKBOARD", "https://bb.neu.edu.cn/webapps/bb-sso-BBLEARN/index.jsp")
    val SEARCH = CampusApp("SEARCH", "https://so.neu.edu.cn/#/accessLogin?searchWord=")
    // WebVPN 只认自己的 CAS 登录入口，ST 绑定到带 cas_login 的回调地址
    val WEBVPN = CampusApp("WEBVPN", "https://webvpn.neu.edu.cn/login?cas_login=true")
    val LIBRARY = CampusApp("LIBRARY", "https://portal.neu.edu.cn/tp_up/up/subgroup/gotoLibirarySystem")
    val BUS_BOOKING = CampusApp("BUS_BOOKING", "http://book.neu.edu.cn/booking/page/selectList/stu_bus")
    val PLACE_BOOKING = CampusApp("PLACE_BOOKING", "http://book.neu.edu.cn/booking/page/selectPeList")

    // IP 网关的回调地址带动态 ac_id
    fun ipGateway(acId: String) = CampusApp(
      name = "IP_GATEWAY",
      callbackUrl = "http://ipgw.neu.edu.cn/srun_portal_sso?ac_id=$acId",
    )
  }
}

/** 稳定的小写标识，后续可用于按应用缓存等场景。 */
fun CampusApp.id(): String = name.lowercase()
