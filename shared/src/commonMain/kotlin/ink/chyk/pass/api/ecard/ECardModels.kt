package ink.chyk.pass.api.ecard

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

enum class ECardWallet(val formValue: String) {
  MAIN("0"),    // 主钱包
  SUBSIDY("1"), // 补助钱包
}

/**
 * 一页查询结果。服务端按每页 10 条分页，页码从 1 开始。
 */
@Serializable
data class ECardPage<T>(
  val records: List<T>, // 当前页记录
  val page: Int,        // 当前页码
  val pageCount: Int,   // 总页数
)

/**
 * 一卡通消费记录，对应自助查询平台 ConsumeInfo.aspx 表格中的一行。
 * 金额字段保留页面下发的字符串格式，避免在 API 层做小数转换。
 */
@Serializable
data class ECardConsumeRecord(
  val time: LocalDateTime,      // 操作时间，页面格式为 yyyy/M/d H:mm:ss
  val description: String,      // 科目描述
  val amount: String,           // 钱包交易金额
  val managementFee: String,    // 钱包交易管理费
  val balance: String,          // 交易后钱包余额
  val operator: String,         // 操作员
  val workstation: String,      // 工作站
  val terminal: String,         // 终端名称
)

/**
 * 一卡通圈存记录，对应自助查询平台 BankInfo.aspx 表格中的一行。
 * 金额字段保留页面下发的字符串格式，避免在 API 层做小数转换。
 */
@Serializable
data class ECardBankRecord(
  val machine: String,          // 圈存机
  val time: LocalDateTime,      // 操作时间，页面格式为 yyyy/M/d H:mm:ss
  val amount: String,           // 钱包操作金额
  val description: String,      // 描述
)
