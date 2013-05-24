package com.jetbrains.plugins.embeditor.completion;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
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
  public static Pair<LookupElement[], Integer> getCompletionVariants(@NotNull final String path,
                                                                     final String fileContent,
                                                                     final int line,
                                                                     final int column) {
    LOG.debug("getCompletionVariants(" + path + ":" + line + ":" + column);

    final Collection<LookupElement> variants = newHashSet();
    final Ref<Integer> result = Ref.create(null);
    EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
      @Override
      public void completionFinished(@NotNull CompletionParameters parameters, @NotNull LookupElement[] items, @NotNull Document document) {
        variants.addAll(Lists.newArrayList(items));
        result.set(EmbeditorUtil.getCompletionPrefixLength(parameters));
      }
    });
    return Pair.create(variants.toArray(new LookupElement[variants.size()]), result.get());
  }
}
