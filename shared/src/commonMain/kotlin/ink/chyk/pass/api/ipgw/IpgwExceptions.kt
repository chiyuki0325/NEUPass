package ink.chyk.pass.api.ipgw

open class IpgwException(message: String) : Exception(message)

class IpgwArrearageUserException : IpgwException("用户已欠费")

class IpgwTooLongNoLoginException : IpgwException("太久没有登录无线网")
