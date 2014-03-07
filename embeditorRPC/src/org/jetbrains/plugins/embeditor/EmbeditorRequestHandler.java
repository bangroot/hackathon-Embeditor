package org.jetbrains.plugins.embeditor;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
public class EmbeditorRequestHandler {
  private final static Logger LOG = Logger.getInstance(EmbeditorRequestHandler.class);

  public Hashtable[] resolve(String path, String fileContent, int line, int column) {
    LOG.debug(String.format("resolve(%s:%d:%d)", path, line, column));
    List<ResolveOutcome> resolveOutcomes = EmbeditorUtil.getResolveOutcomes(path, fileContent, line, column);
    Hashtable[] results = new Hashtable[resolveOutcomes.size()];
    for (int i = 0; i < resolveOutcomes.size(); i++) {
      ResolveOutcome resolveOutcome = resolveOutcomes.get(i);
      Hashtable<String, Object> result = new Hashtable<String, Object>();
      result.put("path", resolveOutcome.getFilePath());
      result.put("line", resolveOutcome.getRow());
      result.put("column", resolveOutcome.getColumn());
      result.put("text", resolveOutcome.getText());
      results[i] = result;
    }
    return results;
  }

  public int getCompletionStartOffsetInLine(String path, String fileContent, int line, int column) {
    LOG.debug(String.format("getCompletionStartOffsetInLine(%s:%d:%d)", path, line, column));
    final Ref<Integer> integerRef = Ref.create(0);
    EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
      @Override
      public void completionFinished(@NotNull CompletionParameters parameters,
                                     @NotNull LookupElement[] items,
                                     @NotNull Document document) {
        integerRef.set(EmbeditorUtil.getOffsetFromLineStart(parameters, document));
      }
    });
    return integerRef.get();
  }

  public String[] getCompletionVariants(String path, String fileContent, int line, int column) {
    LOG.debug(String.format("getCompletionVariants(%s:%d:%d)", path, line, column));
    final Collection<String> completionVariants = ContainerUtil.newLinkedList();
    EmbeditorUtil.performCompletion(path, fileContent, line, column, new EmbeditorUtil.CompletionCallback() {
      @Override
      public void completionFinished(@NotNull CompletionParameters parameters,
                                     @NotNull LookupElement[] items,
                                     @NotNull Document document) {
        for (LookupElement item : items) {
          completionVariants.add(item.getLookupString());
        }
      }
    });
    return completionVariants.toArray(new String[completionVariants.size()]);
  }

  public Hashtable[] inspectCode(final String path, String fileContent) {
    final Hashtable[][] resultsWrapper = new Hashtable[1][];
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        PsiFile file = EmbeditorUtil.findTargetFile(path);
        if (file != null) {
          List<Problem> problems = CodeInspector.inspect(file);
          Hashtable[] results = new Hashtable[problems.size()];
          for (int i = 0; i < problems.size(); i++) {
            Problem problem = problems.get(i);
            Hashtable<String, Object> result = new Hashtable<String, Object>();
            result.put("line", problem.getLine());
            result.put("column", problem.getColumn());
            result.put("text", problem.getText());
            results[i] = result;
          }
          resultsWrapper[0] = results;
        }
      }
    });
    return resultsWrapper[0];
  }
}
