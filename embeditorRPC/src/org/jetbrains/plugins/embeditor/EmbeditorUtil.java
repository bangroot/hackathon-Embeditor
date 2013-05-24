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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.reference.SoftReference;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * User: zolotov
 * Date: 5/24/13
 */
public final class EmbeditorUtil {
  private EmbeditorUtil() {
  }

  private static final Key<SoftReference<Pair<PsiFile, Document>>> SYNC_FILE_COPY_KEY = Key.create("CompletionFileCopy");

  public static void performCompletion(@NotNull final String path, @NotNull final String fileContent, final int line, final int column, @NotNull final CompletionCallback completionCallback) {
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        final PsiFile targetPsiFile = findTargetFile(path);
        final VirtualFile targetVirtualFile = targetPsiFile != null ? targetPsiFile.getVirtualFile() : null;
        if (targetPsiFile != null && targetVirtualFile != null) {
          final EditorFactory editorFactory = EditorFactory.getInstance();
          final Project project = targetPsiFile.getProject();
          final Document originalDocument = PsiDocumentManager.getInstance(project).getDocument(targetPsiFile);
          if (originalDocument != null) {
            PsiFile fileCopy = createFileCopy(targetPsiFile, fileContent);
            final Document document = fileCopy.getViewProvider().getDocument();
            if (document != null) {
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
                                                      @NotNull CompletionProgressIndicator indicator,
                                                      @NotNull LookupElement[] items,
                                                      boolean hasModifiers) {
                      CompletionServiceImpl.setCompletionPhase(new CompletionPhase.ItemsCalculated(indicator));
                      completionCallback.completionFinished(indicator.getParameters(), items, document);
                    }
                  };

                  handler.invokeCompletion(project, editor);
                }
              }, null, null);
            }
          }
        }
      }
    });
  }

  @NotNull
  public static PsiFile createFileCopy(@NotNull final PsiFile originalFile, @NotNull final CharSequence newFileContent) {
    final PsiFile[] fileCopy = {null};
    CommandProcessor.getInstance().runUndoTransparentAction(new Runnable() {
      @Override
      public void run() {
        fileCopy[0] = ApplicationManager.getApplication().runWriteAction(new Computable<PsiFile>() {
          @Override
          public PsiFile compute() {
            final SoftReference<Pair<PsiFile, Document>> reference = originalFile.getUserData(SYNC_FILE_COPY_KEY);
            if (reference != null) {
              final Pair<PsiFile, Document> pair = reference.get();
              if (pair != null && pair.first.getClass().equals(originalFile.getClass()) && isCopyUpToDate(pair.first, pair.second)) {
                final PsiFile copy = pair.first;
                if (copy.getViewProvider().getModificationStamp() > originalFile.getViewProvider().getModificationStamp()) {
                  ((PsiModificationTrackerImpl) originalFile.getManager().getModificationTracker()).incCounter();
                }
                final Document document = pair.second;
                document.setText(newFileContent);
                return copy;
              }
            }

            final PsiFile copy = (PsiFile) originalFile.copy();
            final Document documentCopy = copy.getViewProvider().getDocument();
            if (documentCopy == null) {
              throw new IllegalStateException("Document copy can't be null");
            }
            originalFile.putUserData(SYNC_FILE_COPY_KEY, new SoftReference<Pair<PsiFile, Document>>(Pair.create(copy, documentCopy)));
            PsiDocumentManager.getInstance(originalFile.getProject()).commitDocument(documentCopy);
            return copy;
          }
        });
      }
    });
    return fileCopy[0];
  }

  private static boolean isCopyUpToDate(@NotNull PsiFile file, @NotNull Document document) {
    if (!file.isValid()) {
      return false;
    }
    PsiFile current = PsiDocumentManager.getInstance(file.getProject()).getPsiFile(document);
    return current != null && current.getViewProvider().getPsi(file.getLanguage()) == file;
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
    void completionFinished(@NotNull CompletionParameters parameters,
                            @NotNull LookupElement[] items,
                            @NotNull Document document);
  }
}
