package ink.chyk.pass.features.courses

import kotlin.test.Test
import kotlin.test.assertEquals

class HitokotoTest {
  @Test
  fun convertsOfficialSentencesWithOptionalAuthors() {
    val sentences = """
      [
        {"id":1,"hitokoto":"测试句子","from":"作品","from_who":"作者","type":"a"},
        {"hitokoto":"另一句","from":"另一部作品","from_who":null},
        {"hitokoto":"无署名","from":"来源"}
      ]
    """.trimIndent()

    assertEquals(
      listOf("测试句子" to "作者, 作品", "另一句" to "另一部作品", "无署名" to "来源"),
      Hitokoto.convertSentences(sentences),
    )
  }
}
