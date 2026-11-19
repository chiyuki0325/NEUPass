#!/usr/bin/env python3

# 使用字典树的方式存储课程编号与课程类别的对应关系

from typing import List, Dict, Optional as Opt
import csv
import json  # 对照组
import zlib

COURSE_GROUP_MAP: Dict[str, int] = {
    "数学与自然科学": 0,
    "数学与自然科学类": 0,
    "化 学 模 块": 1,
    "人文社会科学类": 1,
    "四史教育类": 1,
    "通识类": 2,
    "通识选修类": 2,
    "通识选修课": 2,
    "科学素养类": 2,
    "数字素养类": 2,
    "专业基础课": 3,
    "学科基础类": 3,
    "专业核心课": 3,
    "专业选修课": 3,
    "专业方向类": 3,
    "微专业": 3,
    "专业平台": 3,
    "实践课": 4,
    "实践类": 4,
    "其他": 5,
    "交 叉 领 域 模 块": 5,
    "暑期国际课": 5,
    "课程类别": 5,
}


class TrieNode:
    def __init__(self):
        self.childs: Dict[str, TrieNode] = {}
        self.value: Opt[int] = None


class Trie:
    def __init__(self):
        self.root = TrieNode()

    def get(self, key: str) -> Opt[int]:
        cursor = self.root
        for char in key:
            cursor = cursor.childs[char]
        return cursor.value

    def insert(self, key: str, value: int):
        cursor = self.root
        for char in key:
            if char not in cursor.childs:
                cursor.childs[char] = TrieNode()
            cursor = cursor.childs[char]
        cursor.value = value

    def serialize(self) -> str:
        # 存储为字符串
        result = ""

        def dfs(node: TrieNode):
            nonlocal result
            for key_part, child in node.childs.items():
                result += key_part
                dfs(child)
            if node.value is not None:
                result += ">"
                result += str(node.value)
            result += "<"

        dfs(self.root)
        return result[:-1]

    def deserialize(self, s: str):
        key = ["0"] * 24
        end = 0
        pos = 0
        n = len(s)

        while pos < n:
            if s[pos] == ">":
                # 是叶子节点，阅读 value
                pos += 1
                value = ""
                while s[pos].isdigit():
                    value += s[pos]
                    pos += 1
                pos -= 1
                self.insert("".join(key[:end]), int(value))
            elif s[pos] == "<":
                end -= 1
            else:
                key[end] = s[pos]
                end += 1
            pos += 1

        assert end == 0


course_lists = ["lessons_list_dedup.csv", "lessons_list_dedup2.csv"]

t = Trie()
d = {}

for course_list in course_lists:
    with open(course_list) as f:
        reader = csv.reader(f)
        for parts in reader:
            course_code: str = parts[2]
            if course_code[0] != "A":
                # 研✌️不关我事
                continue

            course_group: int = COURSE_GROUP_MAP[parts[4]]

            t.insert(course_code, course_group)
            d[course_code] = course_group

result = t.serialize()
result_json = json.dumps(d, separators=(",", ":"))

print("JSON 存储 (对照组)", len(result_json), "Bytes")
print("JSON 存储 (gzip 压缩)", len(zlib.compress(result_json.encode("ascii"))), "Bytes")
print("字典树存储", len(result), "Bytes")
print("字典树存储 (gzip 压缩)", len(zlib.compress(result.encode("ascii"))), "Bytes")


with open("course_code_icon_map.txt", "w") as f:
    f.write(result)
