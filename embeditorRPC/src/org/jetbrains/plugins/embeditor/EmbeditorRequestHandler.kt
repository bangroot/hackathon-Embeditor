package org.jetbrains.plugins.embeditor

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import java.util.Hashtable
import java.util.HashMap
import com.intellij.util.ui.UIUtil

val LOG = Logger.getInstance(javaClass<EmbeditorRequestHandler>())

public class EmbeditorRequestHandler {
  public fun getCompletionPrefixLength(path: String, fileContent: String, line: Int, column: Int): Int {
    LOG?.debug("getCompletionStartOffsetInLine(${path}:${line}:${column}")
    var result = 0
    EmbeditorUtil.performCompletion(path, fileContent, line, column, object: EmbeditorUtil.CompletionCallback {
      override fun completionFinished(parameters: CompletionParameters,
                                      items: Array<out LookupElement>,
                                      document: Document) {
        result = EmbeditorUtil.getOffsetFromLineStart(parameters, document)
      }
    })
    return result
  }

  public fun getCompletionVariants(path: String, fileContent: String, line: Int, column: Int): Array<String> {
    LOG?.debug("getCompletionVariants(${path}:${line}:${column}")
    var variants: Set<String> = setOf()
    EmbeditorUtil.performCompletion(path, fileContent, line, column, object: EmbeditorUtil.CompletionCallback {
      override fun completionFinished(parameters: CompletionParameters,
                                      items: Array<out LookupElement>,
                                      document: Document) {
        variants = items.map { it.getLookupString() }.toSet()
      }
    })
    return variants.toArray(array<String>())
  }

  public fun inspectCode(path: String, fileContent: String): Hashtable<String, String> {
    var result = Hashtable<String, String>()
    UIUtil.invokeAndWaitIfNeeded(runnable {
      val file = EmbeditorUtil.findTargetFile(path)
      if (file != null) {
        val problems = inspect(file)
        result = Hashtable(problems.map { "${it.line}:${it.column}" to it.description }.toMap())
      }
    })
    return result
  }
}

fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> {
  val result = HashMap<K, V>()
  for (x in this) {
    val (k, v) = x
    result.put(k, v)
  }
  return result
}
