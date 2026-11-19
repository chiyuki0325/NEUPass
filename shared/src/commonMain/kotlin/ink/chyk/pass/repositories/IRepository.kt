package ink.chyk.pass.repositories


// 2026.10.28 建立 IRepository 接口
interface IRepository {

  // 所有涉及往磁盘存东西的 repository 都必须实现
  // 用户退出登录时，清理数据
  // 只清除 repo 内手写的 mmkv key，缓存由全局 clearAllCache 处理
  fun clearAllData()
}

val repositories: List<IRepository> = listOf(
  ECodeRepository,
  ECardRepository,
  PortalRepository,
  JwappRepository,
  WebVpnRepository,
  CampusRunRepository,
  CourseRepository,
  LoginRepository,
)
