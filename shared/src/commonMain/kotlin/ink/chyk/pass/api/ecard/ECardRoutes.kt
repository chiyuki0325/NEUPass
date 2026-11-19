package ink.chyk.pass.api.ecard

import ink.chyk.pass.*
import ink.chyk.pass.api.CampusApp
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

private val LOGIN_URL = CampusApp.ECARD.callbackUrl
private const val SSO_LOGIN_URL = "http://ecard.neu.edu.cn/selfsearch/SSOLogin.aspx"
internal const val ECARD_INDEX_PATH = "/selfsearch/Index.aspx"
internal const val ECARD_INDEX_URL = "http://ecard.neu.edu.cn$ECARD_INDEX_PATH"

private const val CONSUME_INFO_URL = "http://ecard.neu.edu.cn/selfsearch/User/ConsumeInfo.aspx" // 消费记录页面
private const val BANK_INFO_URL = "http://ecard.neu.edu.cn/selfsearch/User/BankInfo.aspx" // 圈存记录页面

// 以下字段名都来自页面表单，是服务端（ASP.NET WebForms）生成的 name，提交时不能改写。
// 表头「操作时间」链接的 id，class 用于判断当前是 ASC 还是 DESC。
private const val SORT_BUTTON_ID = "ContentPlaceHolder1_gridView_SortBt"
// 点击表头时回发的 __EVENTTARGET；和表头 id 不同，不能直接由 SORT_BUTTON_ID 拼出。
private const val SORT_TARGET = "ctl00\$ContentPlaceHolder1\$gridView\$ctl01\$SortBt"
private const val WALLET_FIELD = "ctl00\$ContentPlaceHolder1\$rbtnType" // 钱包类型：0 主钱包，1 补助钱包
private const val START_DATE_FIELD = "ctl00\$ContentPlaceHolder1\$txtStartDate" // 开始日期，yyyy-MM-dd
private const val END_DATE_FIELD = "ctl00\$ContentPlaceHolder1\$txtEndDate" // 结束日期，yyyy-MM-dd
private const val SEARCH_FIELD = "ctl00\$ContentPlaceHolder1\$btnSearch" // 查询按钮；排序 postback 不提交它
private const val SEARCH_VALUE = "查  询"

// 一卡通自助查询平台（ecard）API
abstract class ECardRoutes internal constructor(
  private val loginHttp: HttpClient,
  private val http: HttpClient,
) {
  abstract suspend fun login(appTicket: String)

  // 用统一认证换出的 ST 建立一卡通平台会话。
  // selflogin/login.aspx 消费 ST 后返回一张自动提交表单，
  // 其中隐藏字段由服务端生成，回传给 SSOLogin.aspx 才真正下发会话 Cookie。
  protected suspend fun exchangeTicket(appTicket: String): String? {
    // 消费 ST，拿到服务端生成的换票表单
    val html = loginHttp.get(LOGIN_URL) {
      parameter("ticket", appTicket)
    }.let {
      // 服务端错误不当作票据失效，避免无意义地重换 ST
      if (it.status.value >= 500) throw RequestFailedException(LOGIN_URL)
      it.bodyAsText()
    }
    val fields = parseBridgeForm(html) ?: throw TicketExpiredException()

    // 换取一卡通平台会话，成功时 302 到首页并下发 .ASPXAUTSSM
    val response = loginHttp.post(SSO_LOGIN_URL) {
      setBody(FormDataContent(Parameters.build {
        fields.forEach { (name, value) -> append(name, value) }
      }))
    }
    if (response.status.value >= 500) throw RequestFailedException(SSO_LOGIN_URL)
    if (response.status.value !in 300..399) throw TicketExpiredException()
    return response.headers[HttpHeaders.Location]
  }

  // 消费记录按操作时间倒序返回，每页 10 条。
  suspend fun getConsumeRecords(
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet = ECardWallet.MAIN,
    page: Int = 1,
  ): ECardPage<ECardConsumeRecord> =
    queryDescendingPage(CONSUME_INFO_URL, ECardGridPage.CONSUME, startDate, endDate, wallet, page)
      .let { ECardPage(parseConsumeRecords(it.html), it.page, it.pageCount) }

  // 圈存记录按操作时间倒序返回，每页 10 条。
  suspend fun getBankRecords(
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet = ECardWallet.MAIN,
    page: Int = 1,
  ): ECardPage<ECardBankRecord> =
    queryDescendingPage(BANK_INFO_URL, ECardGridPage.BANK, startDate, endDate, wallet, page)
      .let { ECardPage(parseBankRecords(it.html), it.page, it.pageCount) }

  // 一卡通平台是 ASP.NET WebForms：排序方向不在请求参数里，只存在 __VIEWSTATE 中，
  // 点「操作时间」表头会翻转当前方向。页面刚打开时是 ASC，
  // 因此带上日期字段提交一次 SortBt 就能直接得到 DESC 结果。
  // 倒序状态可以跨会话复用，本地有缓存时直接 POST，省掉 GET 和翻转排序；
  // 服务端 machineKey 轮换会让缓存失效，此时清掉缓存重新走一遍。
  private suspend fun queryDescendingGrid(
    url: String,
    page: ECardGridPage,
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet,
  ): String {
    val cached = ECardViewStateStorage.load(page)
    if (cached != null) {
      try {
        val html = postGrid(url, cached, startDate, endDate, wallet)
        if (rememberIfDescending(page, html, url)) return html
      } catch (_: ECardViewStateExpiredException) {
        ECardViewStateStorage.clear(page)
      }
    }

    val state = parseFormState(http.get(url).bodyAsText(), url)
    val html = postGrid(url, state, startDate, endDate, wallet)
    rememberIfDescending(page, html, url)
    return html
  }

  // 分页状态同样只存在 __VIEWSTATE 里。先取第 1 页拿到状态和总页数，
  // 再用 AspNetPager 的 __EVENTARGUMENT 直接跳到目标页。
  private suspend fun queryDescendingPage(
    url: String,
    gridPage: ECardGridPage,
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet,
    page: Int,
  ): DescendingPage {
    val first = queryDescendingGrid(url, gridPage, startDate, endDate, wallet)
    val pager = parsePager(first) ?: return DescendingPage(first, 1, 1)
    val target = page.coerceIn(1, pager.pageCount)
    if (target == 1) return DescendingPage(first, 1, pager.pageCount)

    val html = postPager(url, parseFormState(first, url), pager, target, startDate, endDate, wallet)
    val resultPager = parsePager(html)
    return DescendingPage(
      html = html,
      page = resultPager?.current ?: target,
      pageCount = resultPager?.pageCount ?: pager.pageCount,
    )
  }

  private suspend fun postGrid(
    url: String,
    state: ECardFormState,
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet,
  ): String {
    val fields = mutableMapOf(
      "__EVENTTARGET" to if (state.descending) "" else SORT_TARGET,
      "__EVENTARGUMENT" to "",
      "__VIEWSTATE" to state.viewState,
      "__EVENTVALIDATION" to state.eventValidation,
      WALLET_FIELD to wallet.formValue,
      START_DATE_FIELD to startDate.toString(),
      END_DATE_FIELD to endDate.toString(),
    )
    // 当前已经是 DESC 时不再翻转，直接按日期查询，保持排序方向。
    if (state.descending) fields[SEARCH_FIELD] = SEARCH_VALUE

    return postForm(url, fields)
  }

  // 翻页只回发 AspNetPager 的 __EVENTTARGET/__EVENTARGUMENT，不提交查询按钮。
  private suspend fun postPager(
    url: String,
    state: ECardFormState,
    pager: ECardPager,
    targetPage: Int,
    startDate: LocalDate,
    endDate: LocalDate,
    wallet: ECardWallet,
  ): String = postForm(url, mapOf(
    "__EVENTTARGET" to pager.target,
    "__EVENTARGUMENT" to targetPage.toString(),
    "__VIEWSTATE" to state.viewState,
    "__EVENTVALIDATION" to state.eventValidation,
    WALLET_FIELD to wallet.formValue,
    START_DATE_FIELD to startDate.toString(),
    END_DATE_FIELD to endDate.toString(),
  ))

  private suspend fun postForm(url: String, fields: Map<String, String>): String =
    http.post(url) {
      setBody(FormDataContent(Parameters.build {
        fields.forEach { (name, value) -> append(name, value) }
      }))
    }.bodyAsText()

  // 只缓存倒序状态；方向不对时说明状态已不可信，顺带清掉。
  private fun rememberIfDescending(page: ECardGridPage, html: String, url: String): Boolean {
    val state = parseFormState(html, url)
    if (state.descending) ECardViewStateStorage.save(page, state) else ECardViewStateStorage.clear(page)
    return state.descending
  }

  fun close() {
    loginHttp.close()
    http.close()
  }
}

// selflogin/login.aspx 消费 ST 后返回的换票表单，action 指向 SSOLogin.aspx。
// 直连时属性是单引号，经 WebVPN 代理时网关会把 form 起始标签重写成双引号，
// 所以按 action 识别表单，并同时接受两种引号。
private val bridgeForm = Regex(
  """<form\b[^>]*\baction\s*=\s*["']?[^"'\s>]*SSOLogin\.aspx["']?[^>]*>.*?</form>""",
  setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)

// 隐藏字段当前为 username/timestamp/auid，客户端原样回传，不解析生成规则。
// 票据失效时页面里没有这张表单，返回 null 交由调用方按 ST 过期处理。
private fun parseBridgeForm(html: String): Map<String, String>? {
  val form = bridgeForm.find(html)?.value ?: return null
  return inputTag.findAll(form).mapNotNull { tag ->
    val attributes = parseAttributes(tag.value)
    val name = attributes["name"] ?: return@mapNotNull null
    val value = attributes["value"] ?: return@mapNotNull null
    name to value
  }.toMap().ifEmpty { null }
}

private fun parseFormState(html: String, url: String): ECardFormState {
  val viewState = inputValue(html, "__VIEWSTATE") ?: throw RequestFailedException(url)
  val eventValidation = inputValue(html, "__EVENTVALIDATION") ?: throw RequestFailedException(url)
  return ECardFormState(
    viewState = viewState,
    eventValidation = eventValidation,
    descending = sortButtonClass(html)?.contains("Desc", ignoreCase = true) == true,
  )
}

private data class DescendingPage(
  val html: String,
  val page: Int,
  val pageCount: Int,
)

private data class ECardPager(
  val current: Int,   // 当前页码，从 1 开始
  val pageCount: Int, // 总页数
  val target: String, // AspNetPager 的 __EVENTTARGET
)

// AspNetPager 的翻页链接形如：
// <a href="javascript:__doPostBack('ctl00$ContentPlaceHolder1$AspNetPager1','2')">2</a>
// 末页时「>>」会禁用，所以总页数取当前页码和所有翻页参数的最大值。
private val pagerBlock = Regex("""<div[^>]*id="[^"]*AspNetPager[^"]*"[^>]*>.*?</div>""", RegexOption.DOT_MATCHES_ALL)
private val pagerCurrent = Regex("""<span class="currentPager">(\d+)</span>""")
private val pagerPostBack = Regex("""__doPostBack\(&#39;([^&]+?)&#39;,&#39;(\d+)&#39;\)""")

private fun parsePager(html: String): ECardPager? {
  val block = pagerBlock.find(html)?.value ?: return null
  val current = pagerCurrent.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 1
  val links = pagerPostBack.findAll(block).mapNotNull { match ->
    match.groupValues[2].toIntOrNull()?.let { match.groupValues[1] to it }
  }.toList()
  if (links.isEmpty()) return null
  return ECardPager(
    current = current,
    pageCount = maxOf(current, links.maxOf { it.second }),
    target = links.first().first,
  )
}

private val gridTable = Regex("""<table[^>]*class="GridViewStyle"[^>]*>.*?</table>""", RegexOption.DOT_MATCHES_ALL)
private val dataRow = Regex("""<tr class="(?:RowStyle|AltRowStyle)">(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
private val tableCell = Regex("""<td[^>]*>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL)

private fun parseConsumeRecords(html: String): List<ECardConsumeRecord> {
  val table = gridTable.find(html)?.value ?: return emptyList()
  return dataRow.findAll(table).mapNotNull { row ->
    val cells = tableCell.findAll(row.groupValues[1]).map { cellText(it.groupValues[1]) }.toList()
    if (cells.size < 8) return@mapNotNull null
    val time = parseTime(cells[0]) ?: return@mapNotNull null
    ECardConsumeRecord(
      time = time,
      description = cells[1],
      amount = cells[2],
      managementFee = cells[3],
      balance = cells[4],
      operator = cells[5],
      workstation = cells[6],
      terminal = cells[7],
    )
  }.toList()
}

private fun parseBankRecords(html: String): List<ECardBankRecord> {
  val table = gridTable.find(html)?.value ?: return emptyList()
  return dataRow.findAll(table).mapNotNull { row ->
    val cells = tableCell.findAll(row.groupValues[1]).map { cellText(it.groupValues[1]) }.toList()
    if (cells.size < 4) return@mapNotNull null
    val time = parseTime(cells[1]) ?: return@mapNotNull null
    ECardBankRecord(
      machine = cells[0],
      time = time,
      amount = cells[2],
      description = cells[3],
    )
  }.toList()
}

private val consumeTime = Regex("""(\d{4})/(\d{1,2})/(\d{1,2}) (\d{1,2}):(\d{2}):(\d{2})""")

// 页面时间形如 2026/11/4 11:24:47，补零后交给 kotlinx-datetime 解析。
private fun parseTime(text: String): LocalDateTime? {
  val match = consumeTime.matchEntire(text) ?: return null
  val (year, month, day, hour, minute, second) = match.destructured
  return LocalDateTime.parse(
    "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}" +
      "T${hour.padStart(2, '0')}:$minute:$second"
  )
}

private val inputTag = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)
private val anchorTag = Regex("""<a\b[^>]*>""", RegexOption.IGNORE_CASE)
// 属性可能带单引号、双引号或不带引号（如 type=hidden），值原样取出。
private val htmlAttribute = Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*(["']?)(.*?)\2(?=[\s/>])""")

private fun inputValue(html: String, name: String): String? =
  inputTag.findAll(html).firstNotNullOfOrNull { tag ->
    val attributes = parseAttributes(tag.value)
    if (attributes["name"] == name) attributes["value"] else null
  }

private fun sortButtonClass(html: String): String? =
  anchorTag.findAll(html).firstNotNullOfOrNull { tag ->
    val attributes = parseAttributes(tag.value)
    if (attributes["id"] == SORT_BUTTON_ID) attributes["class"] else null
  }

private fun parseAttributes(tag: String): Map<String, String> =
  htmlAttribute.findAll(tag).associate { it.groupValues[1].lowercase() to it.groupValues[3] }

private val htmlTag = Regex("""<[^>]*>""")
private val htmlEntity = Regex("""&(#x?[0-9a-fA-F]+|[a-zA-Z]+);""")

private fun cellText(html: String): String =
  decodeHtmlEntities(html.replace(htmlTag, "")).trim()

private fun decodeHtmlEntities(text: String): String =
  htmlEntity.replace(text) { match ->
    val name = match.groupValues[1]
    when {
      name == "nbsp" -> " "
      name == "amp" -> "&"
      name == "lt" -> "<"
      name == "gt" -> ">"
      name == "quot" -> "\""
      name == "apos" -> "'"
      name.startsWith("#x", ignoreCase = true) ->
        name.drop(2).toIntOrNull(16)?.toChar()?.toString() ?: match.value
      name.startsWith("#") ->
        name.drop(1).toIntOrNull()?.toChar()?.toString() ?: match.value
      else -> match.value
    }
  }
