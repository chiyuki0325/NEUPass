package ink.chyk.pass.models

import kotlin.text.iterator

class Trie {
  val root = TrieNode()
  private var pos = 0 // 使用全局指针追踪字符串位置，用于 deserialize

  operator fun get(key: String): Int? {
    var cursor = root
    for (char in key) {
      cursor = cursor.childs[char] ?: return null
    }
    return cursor.value
  }

  operator fun set(key: String, value: Int) {
    var cursor = root
    for (char in key) {
      cursor = cursor.childs.getOrPut(char) { TrieNode() }
    }
    cursor.value = value
  }

  fun fill(current: TrieNode, s: String) {
    while (pos < s.length) {
      when (val char = s[pos++]) {
        '>' -> {
          // 读取数字逻辑
          val start = pos
          while (pos < s.length && s[pos].isDigit()) pos++
          current.value = s.substring(start, pos).toInt()
          // 注意：这里不需要返回，因为后面可能还有子节点或回溯
        }
        '<' -> {
          // 遇到回溯符，直接结束当前递归层
          return
        }
        else -> {
          // 遇到字符，创建/获取子节点并“潜”入下一层
          val child = current.childs.getOrPut(char) { TrieNode() }
          fill(child, s)
        }
      }
    }
  }

  fun deserialize(s: String) {
    pos = 0
    // 从根节点开始递归填充
    fill(root, s)
  }
}

class TrieNode {
  val childs = mutableMapOf<Char, TrieNode>()
  var value: Int? = null
}