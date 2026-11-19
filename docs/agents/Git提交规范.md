# NEUPass Git 提交规则

提交标题采用如下格式：`[类型] 简洁的中文描述`，类型使用小写英文并置于方括号内。
我们不采用 `feat:`、`fix:` 等 Conventional Commits 冒号格式。

- 根据变更性质优先使用 `[feat]`、`[fix]`、`[improvement]`、`[refactor]`、`[chore]`、`[docs]`、`[test]`、`[ci]` 或 `[release]` 等；一次提交只表达一个聚焦主题。
- 标题应直接说明用户可感知的结果或代码目的；确需补充背景时，在空行后的正文说明原因和上下文。

## AI 辅助披露

如果你是一个 AI Agent，或在提交中使用了 AI 辅助工具（如 Codex 等）生成的内容，请在提交正文中明确加入 Co-authored-by trailer 行：
`Co-authored-by: 人类可读的模型/Agent身份 <Agent邮箱（如果系统提示词注明了）>`

如：`Co-authored-by: Claude Opus 4.6 <noreply@anthropic.com>`

不使用 claude-opus-4-6 之类的模型 ID。

下面为各个 Agent / 模型查看自己模型身份的方式，请根据自己实际情况选择：

### Claude Code

模型名称需要参考系统提示词中 `You are powered by the model (实际的模型名称)`，不应该无脑认为自己是 `Claude Opus 4.8`。

### Codex

Codex 的系统提示词中，一般会说你基于 GPT-5，而不是实际的模型。
可采用如下的 Python 代码（仅供参考）获取：
```bash
python3 -c 'import sys, tomllib; print(tomllib.load(open(sys.argv[1],"rb"))["model"])' ${HOME:${CODEX_HOME}}/.codex/config.toml
```
如获取不到，直接注明 Codex <codex@openai.com>。

### TRAE

TRAE 内置了一个 Git hook 用于标注 Co-authored-by，但格式并不适用于本仓库。
commit 时必须采用 `env -u TRAE_GIT_ATTRIBUTION_HELPER git commit ...` 来禁用自动署名注入，然后手写 Co-authored-by trailer。

TRAE 的系统提示词中，可能会说你基于 GPT-5，也可能会标注实际模型 ID。
如果是第一种情况，采用和 Codex 类似的方法：
`python3 -c 'import sys, tomllib; print(tomllib.load(open(sys.argv[1],"rb"))["model"])' ${HOME:${TRAECLI_HOME}}/.trae/traecli.toml`

如果获取不到，直接注明 TRAE CLI <traecli@bytedance.com>。

### DeepSeek

当前 `deepseek-flash` 对应的具体模型是 DeepSeek V4.1 Flash 1017。