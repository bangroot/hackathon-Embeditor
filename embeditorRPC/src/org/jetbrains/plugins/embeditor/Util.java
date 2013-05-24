package org.jetbrains.plugins.embeditor;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
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
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * User: zolotov
 * Date: 5/24/13
 */
public class Util {
  public static void performCompletion(final String path, final String fileContent, final int line, final int column, final CompletionCallback completionCallback) {
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        final PsiFile targetPsiFile = findTargetFile(path);
        final VirtualFile targetVirtualFile = targetPsiFile != null ? targetPsiFile.getVirtualFile() : null;
        if (targetPsiFile != null && targetVirtualFile != null) {
          final EditorFactory editorFactory = EditorFactory.getInstance();
          final Project project = targetPsiFile.getProject();
          final Document document = PsiDocumentManager.getInstance(project).getDocument(targetPsiFile);
          if (document != null) {
            final CharSequence originalText = document.getCharsSequence();
            setDocumentText(document, fileContent);
            final Editor editor = editorFactory.createEditor(document, project, targetVirtualFile, false);
            int offset = document.getLineStartOffset(line) + column;
            editor.getCaretModel().moveToOffset(offset);
            CommandProcessor.getInstance().executeCommand(project, new Runnable() {
              @Override
              public void run() {
                final CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(CompletionType.BASIC) {

                  @Override
                  protected void completionFinished(int offset1,
                                                    int offset2,
                                                    CompletionProgressIndicator indicator,
                                                    @NotNull LookupElement[] items,
                                                    boolean hasModifiers) {
                    CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(indicator));
                    completionCallback.completionFinished(indicator.getParameters(), items, document);
                  }
                };

                Editor completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, targetPsiFile);
                handler.invokeCompletion(project, completionEditor);
                setDocumentText(document, originalText);
              }
            }, null, null);
          }
        }
      }

      private void setDocumentText(final Document document, final CharSequence content) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            document.setText(content);

          }
        });
      }
    });
  }

  @Nullable
  public static PsiFile findTargetFile(@NotNull String path) {
    Pair<VirtualFile, Project> data = new File(path).isAbsolute() ? findByAbsolutePath(path) : findByRelativePath(path);
    return data != null ? PsiManager.getInstance(data.second).findFile(data.first) : null;
  }

  @Nullable
  public static Pair<VirtualFile, Project> findByAbsolutePath(@NotNull String path) {
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
  public static Pair<VirtualFile, Project> findByRelativePath(@NotNull String path) {
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
  public static VirtualFile findVirtualFile(@NotNull final File file) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
      @Nullable
      @Override
      public VirtualFile compute() {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      }
    });
  }

  public static interface CompletionCallback {
    void completionFinished(CompletionParameters parameters, LookupElement[] items, Document document);
  }
}
