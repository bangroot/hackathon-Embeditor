package org.jetbrains.plugins.embeditor

import com.intellij.psi.PsiFile
import com.intellij.codeInsight.daemon.impl.LocalInspectionsPass
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.InspectionManagerEx
import com.intellij.analysis.AnalysisScope
import com.intellij.profile.codeInspection.InspectionProfileManager
import com.intellij.codeInspection.InspectionProfile
import com.intellij.codeInsight.daemon.impl.DefaultHighlightInfoProcessor

/**
 * @author vlan
 */
public class Problem(
    val line: Int,
    val column: Int,
    val description: String)

public fun inspect(file: PsiFile): List<Problem> {
  // TODO: See the internals InspectionApplication.run()
  val manager = InspectionManager.getInstance(file.getProject()) as InspectionManagerEx
  val profileManager = InspectionProfileManager.getInstance()!!
  val profile = profileManager.getProfile(manager.getCurrentProfile()!!, false) as InspectionProfile
  val tools = profile.getInspectionTools(null)
  val context = manager.createNewGlobalContext(true)
  val scope = AnalysisScope(file)
  val document = file.getViewProvider().getDocument()
  val pass = LocalInspectionsPass(file, document, 0, file.getTextLength(), LocalInspectionsPass.EMPTY_PRIORITY_RANGE, true, DefaultHighlightInfoProcessor())
  return listOf(Problem(10, 4, "Undefined name 'foo'"),
                Problem(12, 6, "Undefined name 'bar'"))
}

