package de.upb.docgen;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import crypto.rules.CrySLRule;
import de.upb.docgen.crysl.CrySLReader;
import de.upb.docgen.utils.Constant;

/**
 * @author Ritika Singh
 */

public class DocumentGeneratorMain {

	public static void main(String[] args) throws IOException {

		CrySLReader reader = new CrySLReader();
		Map<File, CrySLRule> rules = reader.readRulesFromSourceFiles(Constant.rulePath);

		ClassEventForb cef = new ClassEventForb();
		ConstraintsVc valueconstraint = new ConstraintsVc();
		ConstraintsPred predicateconstraint = new ConstraintsPred();
		ConstraintsComparison comp = new ConstraintsComparison();
		ConstraintCrySLVC cryslvc = new ConstraintCrySLVC();
		ConstraintCryslnocallto nocall = new ConstraintCryslnocallto();
		ConstraintCrySLInstanceof instance = new ConstraintCrySLInstanceof();
		ConstraintCrySLandencmode enc = new ConstraintCrySLandencmode();
		Order or = new Order();
		Ensures en = new Ensures();
		EnsuresCaseTwo entwo = new EnsuresCaseTwo();
		Negates neg = new Negates();

		for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
			CrySLRule rule = ruleEntry.getValue();
			cef.getClassName(rule);
			cef.getEventNumbers(rule);
			or.runOrder(rule, ruleEntry.getKey());
			valueconstraint.getConstraintsVc(rule);
			predicateconstraint.getConstraintsPred(rule);
			comp.getConstriantsComp(rule);
			cryslvc.getConCryslVC(rule);
			nocall.getnoCalltoConstraint(rule);
			instance.getInstanceof(rule);
			enc.getConCryslandenc(rule);
			cef.getForbiddenMethods(rule); 
			en.getEnsuresThis(rule);
			entwo.getEnsures(rule);
			neg.getNegates(rule);

		}
	}
}
