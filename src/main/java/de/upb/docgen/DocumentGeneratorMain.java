package de.upb.docgen;

import java.io.*;
import java.util.*;

import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import de.upb.docgen.crysl.CrySLReader;
import de.upb.docgen.graphviz.StateMachineToGraphviz;
import de.upb.docgen.utils.PredicateTreeGenerator;
import de.upb.docgen.utils.TemplateAbsolutePathLoader;
import de.upb.docgen.utils.TreeNode;
import de.upb.docgen.utils.Utils;
import de.upb.docgen.writer.FreeMarkerWriter;
import freemarker.template.*;
import org.apache.commons.io.FileUtils;

/**
 * @author Ritika Singh
 */

public class DocumentGeneratorMain {


	public static void main(String[] args) throws IOException, TemplateException {
		//create singleton to access parsed flags from other classes
		DocSettings docSettings = DocSettings.getInstance();
		docSettings.parseSettingsFromCLI(args);
		//generate Graphviz dot and pngs set --booleanC to turnOff generation
		if(docSettings.isBooleanC()) StateMachineToGraphviz.generateGraphvizStateMachines(docSettings.getRulesetPathDir(),docSettings.getReportDirectory());
		//read CryslRules from absolutePath provided by the user
		Map<File, CrySLRule> rules = CrySLReader.readRulesFromSourceFiles(docSettings.getRulesetPathDir());

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
		List<ComposedRule> composedRuleList = new ArrayList<>();
		Map<String, List<CrySLPredicate>> mapEnsures = new HashMap<>();
		Map<String, List<CrySLPredicate>> mapRequires = new HashMap<>();

		//generate 2 Maps with Ensures, Requires predicates
		for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
			CrySLRule rule = ruleEntry.getValue();
			mapEnsures.put(rule.getClassName(),rule.getPredicates());
			mapRequires.put(rule.getClassName(), rule.getRequiredPredicates());
		}

		//iterate over every Crysl rule, create composedRule for every Rule
		for (Map.Entry<File, CrySLRule> ruleEntry : rules.entrySet()) {
			ComposedRule composedRule = new ComposedRule();
			CrySLRule rule = ruleEntry.getValue();

			//Overview section
			String classname = rule.getClassName();
			//fully qualified name
			composedRule.setComposedClassName(classname);
			//Only rule name necessary for ftl Template
			composedRule.setOnlyRuleName(classname.substring(classname.lastIndexOf(".") + 1));
			//Set classname sentence
			composedRule.setComposedFullClass(cef.getFullClassName(rule));
			//Link to corresponding JavaDoc
			composedRule.setComposedLink(cef.getLink(rule));
			composedRule.setOnlyLink(cef.getLinkOnly(rule));
			composedRule.setNumberOfMethods(cef.getEventNumbers(rule));


			//Order section
			composedRule.setOrder(or.runOrder(rule, ruleEntry.getKey()));

			//
			composedRule.setValueConstraints(valueconstraint.getConstraintsVc(rule));
			//create necessary Data structure to link required predicates of current crysl rule
			Map<String, List<Map<String, List<String>>>> singleRuleEnsuresMap = Utils.mapPredicates(mapEnsures, mapRequires);
			//Pairing Dependency only by class name
			Map<String, Set<String>> singleReqToEns = Utils.toOnlyClassNames(singleRuleEnsuresMap);
			Set<String> ensuresForThisRule = singleReqToEns.get(composedRule.getComposedClassName());
			composedRule.setConstrainedPredicates(predicateconstraint.getConstraintsPred(rule, ensuresForThisRule, singleRuleEnsuresMap));
			//ConstraintsSection
			composedRule.setComparsionConstraints(comp.getConstriantsComp(rule));
			composedRule.setConstrainedValueConstraints(cryslvc.getConCryslVC(rule));
			composedRule.setNoCallToConstraints(nocall.getnoCalltoConstraint(rule));
			composedRule.setInstanceOfConstraints(instance.getInstanceof(rule));
			composedRule.setConstraintAndEncConstraints(enc.getConCryslandenc(rule));
			composedRule.setForbiddenMethods(cef.getForbiddenMethods(rule));
			//
			List<String> allConstraints = new ArrayList<>(composedRule.getComparsionConstraints());
			allConstraints.addAll(composedRule.getValueConstraints());
			allConstraints.addAll(composedRule.getConstrainedPredicates());
			allConstraints.addAll(composedRule.getConstrainedValueConstraints());
			allConstraints.addAll(composedRule.getNoCallToConstraints());
			allConstraints.addAll(composedRule.getInstanceOfConstraints());
			allConstraints.addAll(composedRule.getConstraintAndEncConstraints());
			allConstraints.addAll(composedRule.getForbiddenMethods());
			composedRule.setAllConstraints(allConstraints);

			//Predicates Section
			composedRule.setEnsuresThisPredicates(en.getEnsuresThis(rule, Utils.mapPredicates(mapRequires, mapEnsures)));
			composedRule.setEnsuresPredicates(entwo.getEnsures(rule, Utils.mapPredicates(mapRequires, mapEnsures)));
			composedRule.setNegatesPredicates(neg.getNegates(rule));
			composedRuleList.add(composedRule);
		}

		//Necessary DataStructure to generate Requires and Ensures Tree
		Map<String, List<Map<String, List<String>>>> ensuresToRequiresMap = Utils.mapPredicates(mapRequires, mapEnsures);
		Map<String, List<Map<String, List<String>>>> requiresToEnsuresMap = Utils.mapPredicates(mapEnsures, mapRequires);

		Map<String, Set<String>> onlyClassnamesReqToEns = Utils.toOnlyClassNames(ensuresToRequiresMap);
		Map<String, Set<String>> onlyClassnamesEnsToReq = Utils.toOnlyClassNames(requiresToEnsuresMap);

		Map<String, TreeNode<String>> reqToEns = PredicateTreeGenerator.buildDependencyTreeMap(onlyClassnamesReqToEns);
		Map<String, TreeNode<String>> ensToReq = PredicateTreeGenerator.buildDependencyTreeMap(onlyClassnamesEnsToReq);


		//Freemarker Setup and create cognicryptdoc html pages
		Configuration cfg = new Configuration(new Version(2, 3, 20));
		FreeMarkerWriter.setupFreeMarker(cfg);
		FreeMarkerWriter.createCogniCryptLayout(cfg);
		FreeMarkerWriter.createSidebar(composedRuleList, cfg);
		FreeMarkerWriter.createSinglePage(composedRuleList, cfg, ensToReq, reqToEns, docSettings.isBooleanA(), docSettings.isBooleanB(), docSettings.isBooleanC() , docSettings.isBooleanD() , docSettings.isBooleanE() , docSettings.isBooleanF());
		//copy CryslRulesFolder into generated Cognicrypt folder
		//specifify this flag to distribute the documentation
		if (!docSettings.isBooleanF()) {
			File source = new File(docSettings.getRulesetPathDir());
			File dest = new File(docSettings.getReportDirectory() + File.separator + "rules");
			try {
				FileUtils.copyDirectory(source, dest);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}





	}


}
