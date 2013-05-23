package org.jetbrains.plugins.embeditor;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

import static com.google.common.collect.Sets.newHashSet;

/**
 * User: zolotov
 * Date: 5/23/13
 */
public class EmbeditorRequestHandler {
  private static final Logger LOG = Logger.getInstance(EmbeditorRequestHandler.class);

  // XML-RPC interface method - keep the signature intact
  @NotNull
  @SuppressWarnings("UnusedDeclaration")
  public String[] getCompletionVariants(@NotNull String path, int offset) {
    LOG.debug("getCompletionVariants(" + path + ", " + offset);

    try {
      final PsiFile targetPsiFile = findTargetFile(path);
      VirtualFile targetVirtualFile = targetPsiFile != null ? targetPsiFile.getVirtualFile() : null;
      if (targetPsiFile != null && targetVirtualFile != null) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        final Project project = targetPsiFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(targetPsiFile);
        if (document != null) {
          final Editor editor = editorFactory.createEditor(document, project, targetVirtualFile, false);
          editor.getCaretModel().moveToOffset(offset);
          final Collection<String> variants = newHashSet();
          UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
              CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                @Override
                public void run() {
                  final CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(CompletionType.BASIC) {

                    @Override
                    protected void completionFinished(int offset1,
                                                      int offset2,
                                                      CompletionProgressIndicator indicator,
                                                      LookupElement[] items,
                                                      boolean hasModifiers) {
                      super.completionFinished(offset1, offset2, indicator, items, hasModifiers);
                      variants.addAll(ContainerUtil.map(items, new Function<LookupElement, String>() {
                        @Override
                        public String fun(LookupElement lookupElement) {
                          return lookupElement.getLookupString();
                        }
                      }));
                    }
                  };

                  Editor completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, targetPsiFile);
                  handler.invokeCompletion(project, completionEditor);
                  int i = 0;
                }
              }, null, null);
            }
          });

          return variants.toArray(new String[variants.size()]);
        }
      }
    } catch (Exception e) {
      LOG.error(e);
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Nullable
  private PsiFile findTargetFile(@NotNull String path) {
    Pair<VirtualFile, Project> data = new File(path).isAbsolute() ? findByAbsolutePath(path) : findByRelativePath(path);
    return data != null ? PsiManager.getInstance(data.second).findFile(data.first) : null;
  }

  @Nullable
  private static Pair<VirtualFile, Project> findByAbsolutePath(@NotNull String path) {
    File file = new File(FileUtil.toSystemDependentName(path));
    if (file.exists()) {
      VirtualFile vFile = findVirtualFile(file);
      if (vFile != null) {
        Project project = ProjectLocator.getInstance().guessProjectForFile(vFile);
        if (project != null) {
          return Pair.create(vFile, project);
        }
      }
    }

    return null;
  }

  @Nullable
  private static Pair<VirtualFile, Project> findByRelativePath(@NotNull String path) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    String localPath = FileUtil.toSystemDependentName(path);

    for (Project project : projects) {
      File file = new File(project.getBasePath(), localPath);
      if (file.exists()) {
        VirtualFile vFile = findVirtualFile(file);
        return vFile != null ? Pair.create(vFile, project) : null;
      }
    }

    for (Project project : projects) {
      for (VcsRoot vcsRoot : ProjectLevelVcsManager.getInstance(project).getAllVcsRoots()) {
        VirtualFile root = vcsRoot.getPath();
        if (root != null) {
          File file = new File(FileUtil.toSystemDependentName(root.getPath()), localPath);
          if (file.exists()) {
            VirtualFile vFile = findVirtualFile(file);
            return vFile != null ? Pair.create(vFile, project) : null;
          }
        }
      }
    }

    return null;
  }

  @Nullable
  private static VirtualFile findVirtualFile(@NotNull final File file) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Nullable
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      }
    });
  }
}
