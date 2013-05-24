package com.jetbrains.plugins.embeditor.completion;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.embeditor.EmbeditorUtil;

import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @author traff
 */
public class EmbeditorCompletionUtil {
  private static final Logger LOG = Logger.getInstance(EmbeditorCompletionUtil.class);

  @NotNull
  @SuppressWarnings("UnusedDeclaration")
  public static LookupElement[] getCompletionVariants(@NotNull final String path, final String fileContent, final int line, final int column) {
    LOG.debug("getCompletionVariants(" + path + ":" + line + ":" + column);

    final Collection<LookupElement> variants = newHashSet();
    EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
      @Override
      public void completionFinished(CompletionParameters parameters, LookupElement[] items, Document document) {
        variants.addAll(Lists.newArrayList(items));
      }
    });
    return variants.toArray(new LookupElement[variants.size()]);
  }
}
