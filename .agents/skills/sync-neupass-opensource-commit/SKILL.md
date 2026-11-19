---
name: sync-neupass-opensource-commit
description: 将NEUPass闭源分支上的一个或多个提交通过选择性cherry-pick脱敏后迁移到开源分支。用于用户要求同步、移植、pick或补发闭源分支改动到公开仓库，同时排除Crypto实现、固定密钥、API文档、真实凭据、旧网络层和NoOSS裁剪机制的场景。
---

# 同步NEUPass开源提交

把闭源提交作为改动来源，在开源分支上使用`git cherry-pick --no-commit`应用，再脱敏、审查并重新提交。允许公开API端点、请求/响应模型和一般协议流程；禁止公开Crypto实现和秘密材料。

## 开始前

1. 读取仓库根目录`AGENTS.md`和`docs/agents/Git提交规范.md`。
2. 确认源提交、目标开源分支和目标远端。不得根据名称猜测分支身份。
3. 运行`git status --short`，保留用户已有改动。当前工作树不干净或目标分支已在其他位置检出时，使用独立worktree操作，不得stash、reset或覆盖用户工作。
4. 用以下命令检查源提交，不输出已知凭据的具体值：

   ```bash
   git show --format=fuller --stat --name-status <source-commit>
   git diff <source-commit>^ <source-commit> --name-only
   ```

5. 多个提交必须按原顺序逐个迁移并逐个提交，不得默认压成一个提交。合并提交先确认正确的mainline。

## 选择性pick

在目标开源分支中一次处理一个提交：

```bash
git cherry-pick --no-commit <source-commit>
git restore --staged -- .
```

`--no-commit`是硬性要求。禁止直接`git cherry-pick <source-commit>`，避免未脱敏内容先进入公开分支历史。

发生冲突时，只保留适合公开分支的语义。解决冲突并清除未合并项后，退出cherry-pick序列并保持工作树改动，再取消全部暂存：

```bash
git cherry-pick --quit
git restore --staged -- .
```

不得为追求机械一致而恢复被公开分支有意删除的代码。

## 脱敏规则

逐文件检查本次改动，遵守以下边界。

### 一律排除

- `docs/api/`下的API文档。
- 文件名或职责属于Crypto、加解密、签名或密钥装载的实现，包括`*Crypto*`、`RsaCodec*`及其测试和平台实现。
- PEM私钥、客户端私钥、固定AES密钥、固定签名salt、可用于还原受保护协议的密钥或常量。
- 真实访问令牌、密码、Cookie、ticket、私有凭据文件和部署秘密。公开示例只能使用明显占位符，如`<GITEA_TOKEN>`。
- 用户未授权公开的内部基础设施地址、用户个人数据和无再分发授权的素材。

### 可以保留

- API端点、HTTP方法、普通请求字段、响应模型和错误码。
- CAS/CASTGC/ST、Cookie持久化、重登和请求重放等一般认证流程。
- 教务字段解析、DOM选择器和业务规则。
- 不携带固定秘密材料的普通Ktor客户端、仓库、ViewModel和UI改动。

如果一个允许公开的文件依赖被排除的Crypto实现，优先只去掉依赖该实现的hunk，或接到开源分支已有的公开抽象。不要擅自增加伪实现。开源分支因此暂时不能构建不构成本skill的阻断项，除非用户明确要求构建可用。

## 暂存与审查

脱敏完成后，只暂存逐一确认过的路径。不要使用无范围的`git add -A`：

```bash
git add -- <reviewed-path>...
git diff --cached --check
git diff --cached --stat
git diff --cached --name-status
git diff --cached
```

提交前必须同时检查：

```bash
git ls-files | rg '(^docs/api/|Crypto|RsaCodec|NetworkLegacy|api_legacy|NoOSS)'
git grep -IlE 'BEGIN ([A-Z]+ )?PRIVATE KEY|CLIENT_PRIVATE_KEY|giteaAuthorization[[:space:]]*=[[:space:]]*"[^<]' -- .
```

这些检查输出文件名即可；不要把匹配到的秘密值打印到会话或提交说明。对每个命中逐项判断，预期公开树不包含上述禁止项。

再对比源提交，确认公开提交保留了用户可见功能中允许公开的部分，并明确记录因脱敏省略的行为。不要把源分支名、私有提交哈希、内部工单或敏感背景写进公开提交消息。

## 提交与交付

沿用源提交的主题和作者意图，但按公开内容改写不再准确或含敏感信息的标题、正文。提交格式和AI辅助披露以`docs/agents/Git提交规范.md`为准。

提交后报告：

- 新公开提交哈希和标题；
- 对应迁移的功能；
- 排除或改写了哪些敏感部分；
- 执行过的差异检查和秘密扫描；
- 未执行的构建、测试或运行验证。

除非用户明确要求，不得push、强推、发布或删除源分支。若用户要求push，先检查将要推送的完整提交范围和最终公开树，不得把闭源分支或闭源标签一并推送。
