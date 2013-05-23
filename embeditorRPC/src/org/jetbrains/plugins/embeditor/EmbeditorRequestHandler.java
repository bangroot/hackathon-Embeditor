package org.jetbrains.plugins.embeditor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
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
    PsiFile targetFile = findTargetFile(path);
    if (targetFile != null) {
      final PsiReference referenceAt = targetFile.findReferenceAt(offset);
      if (referenceAt != null) {
        final Collection<String> variants = newHashSet();
        final Object[] referenceAtVariants = referenceAt.getVariants();
        for (Object referenceAtVariant : referenceAtVariants) {
          variants.add(referenceAt.toString());
        }
        return variants.toArray(new String[variants.size()]);
      }
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
