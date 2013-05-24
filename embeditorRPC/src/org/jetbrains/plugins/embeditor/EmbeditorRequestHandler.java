package org.jetbrains.plugins.embeditor;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;

/**
 * User: zolotov
 * Date: 5/23/13
 */
public class EmbeditorRequestHandler {
  private static final Logger LOG = Logger.getInstance(EmbeditorRequestHandler.class);

  // todo: could be optimized, we should not perform full completion cycle in order to retrieve start offset
  // XML-RPC interface method - keep the signature intact
  @SuppressWarnings("UnusedDeclaration")
  public int getStartCompletionOffset(@NotNull final String path, final String fileContent, final int line, final int column) {
    LOG.debug("getStartCompletionOffset(" + path + ":" + line + ":" + column);

    final int[] result = {0};
    Util.performCompletion(path, fileContent, line, column, new Util.CompletionCallback() {
      @Override
      public void completionFinished(CompletionParameters parameters, LookupElement[] items, Document document) {
        int offset = parameters.getPosition().getTextRange().getStartOffset();
        int lineNumber = document.getLineNumber(offset);
        result[0] = offset - document.getLineStartOffset(lineNumber);
      }
    });
    return result[0];
  }

  // XML-RPC interface method - keep the signature intact
  @NotNull
  @SuppressWarnings("UnusedDeclaration")
  public String[] getCompletionVariants(@NotNull final String path, final String fileContent, final int line, final int column) {
    LOG.debug("getCompletionVariants(" + path + ":" + line + ":" + column);

    final Collection<String> variants = newHashSet();
    Util.performCompletion(path, fileContent, line, column, new Util.CompletionCallback() {
      @Override
      public void completionFinished(CompletionParameters parameters, LookupElement[] items, Document document) {
        variants.addAll(ContainerUtil.map(items, new Function<LookupElement, String>() {
          @NotNull
          @Override
          public String fun(@NotNull LookupElement lookupElement) {
            return lookupElement.getLookupString();
          }
        }));
      }
    });
    return variants.size() > 0 ? variants.toArray(new String[variants.size()]) : ArrayUtil.EMPTY_STRING_ARRAY;
  }

}
