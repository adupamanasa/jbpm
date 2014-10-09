/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.bpmn2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.drools.ClockType;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.WorkingMemory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.definition.process.Process;
import org.drools.event.ActivationCancelledEvent;
import org.drools.event.ActivationCreatedEvent;
import org.drools.event.AfterActivationFiredEvent;
import org.drools.event.AgendaGroupPoppedEvent;
import org.drools.event.AgendaGroupPushedEvent;
import org.drools.event.BeforeActivationFiredEvent;
import org.drools.event.RuleFlowGroupActivatedEvent;
import org.drools.event.RuleFlowGroupDeactivatedEvent;
import org.drools.event.process.DefaultProcessEventListener;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.impl.EnvironmentFactory;
import org.drools.impl.KnowledgeBaseFactoryServiceImpl;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.io.ResourceFactory;
import org.drools.process.core.datatype.impl.type.ObjectDataType;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.drools.runtime.process.WorkflowProcessInstance;
import org.drools.runtime.rule.FactHandle;
import org.drools.time.SessionPseudoClock;
import org.jbpm.bpmn2.core.Association;
import org.jbpm.bpmn2.core.DataStore;
import org.jbpm.bpmn2.core.Definitions;
import org.jbpm.bpmn2.handler.ReceiveTaskHandler;
import org.jbpm.bpmn2.handler.SendTaskHandler;
import org.jbpm.bpmn2.handler.ServiceTaskHandler;
import org.jbpm.bpmn2.objects.Person;
import org.jbpm.bpmn2.xml.BPMNDISemanticModule;
import org.jbpm.bpmn2.xml.BPMNExtensionsSemanticModule;
import org.jbpm.bpmn2.xml.BPMNSemanticModule;
import org.jbpm.bpmn2.xml.XmlBPMNProcessDumper;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.process.core.timer.BusinessCalendarImpl;
import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleBPMNProcessTest extends JbpmBpmn2TestCase {

    private Logger logger = LoggerFactory.getLogger(SimpleBPMNProcessTest.class);
    
    protected void setUp() { 
        String testName = getName();
        if( testName.startsWith("testEventBasedSplit") || testName.startsWith("testTimerBoundaryEvent") 
             || testName.startsWith("testIntermediateCatchEventTimer") || testName.startsWith("testTimerStart") ) { 
            persistence = false;
        }
        super.setUp();
    }
	public void testMinimalProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MinimalProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Minimal");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testMinimalProcessImplicit() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MinimalProcessImplicit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Minimal");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testImplicitEndParallel() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ParallelSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testMinimalProcessWithGraphical() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MinimalProcessWithGraphical.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Minimal");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testMinimalProcessWithDIGraphical() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MinimalProcessWithDIGraphical.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Minimal");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testCompositeProcessWithDIGraphical() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-CompositeProcessWithDIGraphical.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Composite");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testScriptTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ScriptTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("ScriptTask");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testImport() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-Import.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Import");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testRuleTask() throws Exception {
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		List<Process> processes = processReader
				.read(SimpleBPMNProcessTest.class
						.getResourceAsStream("/BPMN2-RuleTask.bpmn2"));
		assertNotNull(processes);
		assertEquals(1, processes.size());
		RuleFlowProcess p = (RuleFlowProcess) processes.get(0);
		assertNotNull(p);
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		// logger.debug(XmlBPMNProcessDumper.INSTANCE.dump(p));
		kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
				XmlBPMNProcessDumper.INSTANCE.dump(p))), ResourceType.BPMN2);
		kbuilder.add(
				ResourceFactory.newClassPathResource("BPMN2-RuleTask.drl"),
				ResourceType.DRL);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		List<String> list = new ArrayList<String>();
		ksession.setGlobal("list", list);
		ProcessInstance processInstance = ksession.startProcess("RuleTask");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		restoreSession(ksession, true);
		ksession.fireAllRules();
		assertTrue(list.size() == 1);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testRuleTask2() throws Exception {
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		List<Process> processes = processReader
				.read(SimpleBPMNProcessTest.class
						.getResourceAsStream("/BPMN2-RuleTask2.bpmn2"));
		assertNotNull(processes);
		assertEquals(1, processes.size());
		RuleFlowProcess p = (RuleFlowProcess) processes.get(0);
		assertNotNull(p);
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		// logger.debug(XmlBPMNProcessDumper.INSTANCE.dump(p));
		kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
				XmlBPMNProcessDumper.INSTANCE.dump(p))), ResourceType.BPMN2);
		kbuilder.add(
				ResourceFactory.newClassPathResource("BPMN2-RuleTask2.drl"),
				ResourceType.DRL);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		List<String> list = new ArrayList<String>();
		ksession.setGlobal("list", list);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "SomeString");
		ProcessInstance processInstance = ksession.startProcess("RuleTask", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		restoreSession(ksession, true);
		ksession.fireAllRules();
		assertTrue(list.size() == 0);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testRuleTaskWithFacts() throws Exception {
        KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
                .newKnowledgeBuilderConfiguration();
        ((PackageBuilderConfiguration) conf).initSemanticModules();
        ((PackageBuilderConfiguration) conf)
                .addSemanticModule(new BPMNSemanticModule());
        ((PackageBuilderConfiguration) conf)
                .addSemanticModule(new BPMNDISemanticModule());
        // ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
        XmlProcessReader processReader = new XmlProcessReader(
                ((PackageBuilderConfiguration) conf).getSemanticModules(),
                getClass().getClassLoader());
        List<Process> processes = processReader
                .read(SimpleBPMNProcessTest.class
                        .getResourceAsStream("/BPMN2-RuleTaskWithFact.bpmn2"));
        assertNotNull(processes);
        assertEquals(1, processes.size());
        RuleFlowProcess p = (RuleFlowProcess) processes.get(0);
        assertNotNull(p);
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
                .newKnowledgeBuilder(conf);
        // logger.debug(XmlBPMNProcessDumper.INSTANCE.dump(p));
        kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
                XmlBPMNProcessDumper.INSTANCE.dump(p))), ResourceType.BPMN2);
        kbuilder.add(
                ResourceFactory.newClassPathResource("BPMN2-RuleTask3.drl"),
                ResourceType.DRL);
        if (!kbuilder.getErrors().isEmpty()) {
            for (KnowledgeBuilderError error : kbuilder.getErrors()) {
                logger.error(error.toString());
            }
            throw new IllegalArgumentException(
                    "Errors while parsing knowledge base");
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        final StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        
        final org.drools.event.AgendaEventListener agendaEventListener = new org.drools.event.AgendaEventListener() {
            public void activationCreated(ActivationCreatedEvent event, WorkingMemory workingMemory){
                ksession.fireAllRules();
            }
            public void activationCancelled(ActivationCancelledEvent event, WorkingMemory workingMemory){
            }
            public void beforeActivationFired(BeforeActivationFiredEvent event, WorkingMemory workingMemory) {
            }
            public void afterActivationFired(AfterActivationFiredEvent event, WorkingMemory workingMemory) {
            }
            public void agendaGroupPopped(AgendaGroupPoppedEvent event, WorkingMemory workingMemory) {
            }

            public void agendaGroupPushed(AgendaGroupPushedEvent event, WorkingMemory workingMemory) {
            }
            public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
            }
            public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event, WorkingMemory workingMemory) {
                workingMemory.fireAllRules();
            }
            public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) {
            }
            public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event, WorkingMemory workingMemory) {
            }
        };
        ((StatefulKnowledgeSessionImpl)  ksession).session.addEventListener(agendaEventListener);
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", "SomeString");
        ProcessInstance processInstance = ksession.startProcess("RuleTask", params);
        assertProcessInstanceCompleted(processInstance.getId(), ksession);

        params = new HashMap<String, Object>();

        try {
            processInstance = ksession.startProcess("RuleTask", params);

            fail("Should fail");
        } catch (Exception e) {
            e.printStackTrace();
        }

        params = new HashMap<String, Object>();
        params.put("x", "SomeString");
        processInstance = ksession.startProcess("RuleTask", params);
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
    }
	
	public void testRuleTaskAcrossSessions() throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(
			ResourceFactory.newClassPathResource("BPMN2-RuleTask.bpmn2"), 
			ResourceType.BPMN2);
		kbuilder.add(
			ResourceFactory.newClassPathResource("BPMN2-RuleTask.drl"),
			ResourceType.DRL);
		KnowledgeBase kbase = kbuilder.newKnowledgeBase();
		StatefulKnowledgeSession ksession1 = createKnowledgeSession(kbase);
		StatefulKnowledgeSession ksession2 = createKnowledgeSession(kbase);
		List<String> list1 = new ArrayList<String>();
		ksession1.setGlobal("list", list1);
		List<String> list2 = new ArrayList<String>();
		ksession2.setGlobal("list", list2);
		ProcessInstance processInstance1 = ksession1.startProcess("RuleTask");
		ProcessInstance processInstance2 = ksession2.startProcess("RuleTask");
		ksession1.fireAllRules();
		assertProcessInstanceCompleted(processInstance1.getId(), ksession1);
		assertProcessInstanceActive(processInstance2.getId(), ksession2);
		ksession2.fireAllRules();
		assertProcessInstanceCompleted(processInstance2.getId(), ksession2);
	}
	
	public void testDataObject() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-DataObject.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "UserId-12345");
		ProcessInstance processInstance = ksession.startProcess("Evaluation",
				params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testDataStore() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-DataStore.xml");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Evaluation");
		Definitions def = (Definitions) processInstance.getProcess()
				.getMetaData().get("Definitions");
		assertNotNull(def.getDataStores());
		assertTrue(def.getDataStores().size() == 1);
		DataStore dataStore = def.getDataStores().get(0);
		assertEquals("employee", dataStore.getId());
		assertEquals("employeeStore", dataStore.getName());
		assertEquals(String.class.getCanonicalName(),
				((ObjectDataType) dataStore.getType()).getClassName());
	}

	public void testAssociation() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-Association.xml");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Evaluation");
		Definitions def = (Definitions) processInstance.getProcess()
				.getMetaData().get("Definitions");
		assertNotNull(def.getAssociations());
		assertTrue(def.getAssociations().size() == 1);
		Association assoc = def.getAssociations().get(0);
		assertEquals("_1234", assoc.getId());
		assertEquals("_1", assoc.getSourceRef());
		assertEquals("_2", assoc.getTargetRef());
	}

	public void testUserTaskWithDataStoreScenario() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-UserTaskWithDataStore.xml");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ksession.startProcess("UserProcess");
		// we can't test further as user tasks are asynchronous.
	}

	public void testEvaluationProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EvaluationProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler(
				"RegisterRequest", new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "UserId-12345");
		ProcessInstance processInstance = ksession.startProcess("Evaluation",
				params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testEvaluationProcess2() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EvaluationProcess2.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "UserId-12345");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.evaluation", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testEvaluationProcess3() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EvaluationProcess3.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler(
				"RegisterRequest", new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("employee", "john2");
		ProcessInstance processInstance = ksession.startProcess("Evaluation",
				params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testUserTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-UserTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ProcessInstance processInstance = ksession.startProcess("UserTask");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameter("ActorId"));
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testLane() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-Lane.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ProcessInstance processInstance = ksession.startProcess("UserTask");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("john", workItem.getParameter("ActorId"));
		Map<String, Object> results = new HashMap<String, Object>();
		results.put("ActorId", "mary");
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(),
				results);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		assertEquals("mary", workItem.getParameter("ActorId"));
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testExclusiveSplit() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "First");
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testExclusiveSplitXPathAdvanced() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplitXPath-advanced.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element hi = doc.createElement("hi");
		Element ho = doc.createElement("ho");
		hi.appendChild(ho);
		Attr attr = doc.createAttribute("value");
		ho.setAttributeNode(attr);
		attr.setValue("a");
		params.put("x", hi);
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testExclusiveSplitXPathAdvanced2() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplitXPath-advanced-vars-not-signaled.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element hi = doc.createElement("hi");
		Element ho = doc.createElement("ho");
		hi.appendChild(ho);
		Attr attr = doc.createAttribute("value");
		ho.setAttributeNode(attr);
		attr.setValue("a");
		params.put("x", hi);
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testExclusiveSplitXPathAdvancedWithVars() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplitXPath-advanced-with-vars.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element hi = doc.createElement("hi");
		Element ho = doc.createElement("ho");
		hi.appendChild(ho);
		Attr attr = doc.createAttribute("value");
		ho.setAttributeNode(attr);
		attr.setValue("a");
		params.put("x", hi);
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testExclusiveSplitPriority() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplitPriority.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "First");
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testExclusiveSplitDefault() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ExclusiveSplitDefault.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "NotFirst");
		params.put("y", "Second");
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testInclusiveSplit() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-InclusiveSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", 15);
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testInclusiveSplitDefault() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-InclusiveSplitDefault.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", -5);
		ProcessInstance processInstance = ksession.startProcess(
				"com.sample.test", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	// public void testExclusiveSplitXPath() throws Exception {
	// KnowledgeBase kbase =
	// createKnowledgeBase("BPMN2-ExclusiveSplitXPath.bpmn2");
	// StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
	// ksession.getWorkItemManager().registerWorkItemHandler("Email", new
	// SystemOutWorkItemHandler());
	// Document document =
	// DocumentBuilderFactory.newInstance().newDocumentBuilder()
	// .parse(new ByteArrayInputStream(
	// "<myDocument><chapter1>BlaBla</chapter1><chapter2>MoreBlaBla</chapter2></myDocument>".getBytes()));
	// Map<String, Object> params = new HashMap<String, Object>();
	// params.put("x", document);
	// params.put("y", "SomeString");
	// ProcessInstance processInstance =
	// ksession.startProcess("com.sample.test", params);
	// assertTrue(processInstance.getState() ==
	// ProcessInstance.STATE_COMPLETED);
	// }

	public void testEventBasedSplit() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.signalEvent("Yes", "YesValue", processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		// No
		processInstance = ksession.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.signalEvent("No", "NoValue", processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testEventBasedSplitBefore() throws Exception {
		// signaling before the split is reached should have no effect
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new DoNothingWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new DoNothingWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		ksession.signalEvent("Yes", "YesValue", processInstance.getId());
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		// No
		processInstance = ksession.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new DoNothingWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		ksession.signalEvent("No", "NoValue", processInstance.getId());
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
	}

	public void testEventBasedSplitAfter() throws Exception {
		// signaling the other alternative after one has been selected should
		// have no effect
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		ksession.signalEvent("Yes", "YesValue", processInstance.getId());
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new DoNothingWorkItemHandler());
		// No
		ksession.signalEvent("No", "NoValue", processInstance.getId());
	}

	public void testEventBasedSplit2() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit2.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.signalEvent("Yes", "YesValue", processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		Thread.sleep(800);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.fireAllRules();
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		// Timer
		processInstance = ksession.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		Thread.sleep(800);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.fireAllRules();
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testEventBasedSplit3() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit3.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		Person jack = new Person();
		jack.setName("Jack");
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.signalEvent("Yes", "YesValue", processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		// Condition
		processInstance = ksession.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.insert(jack);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testEventBasedSplit4() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit4.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ksession.signalEvent("Message-YesMessage", "YesValue",
				processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		// No
		processInstance = ksession.startProcess("com.sample.test");
		ksession.signalEvent("Message-NoMessage", "NoValue",
				processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testEventBasedSplit5() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EventBasedSplit5.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		ReceiveTaskHandler receiveTaskHandler = new ReceiveTaskHandler(ksession);
		ksession.getWorkItemManager().registerWorkItemHandler("Receive Task",
				receiveTaskHandler);
		// Yes
		ProcessInstance processInstance = ksession
				.startProcess("com.sample.test");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		receiveTaskHandler.setKnowledgeRuntime(ksession);
		ksession.getWorkItemManager().registerWorkItemHandler("Receive Task",
				receiveTaskHandler);
		receiveTaskHandler.messageReceived("YesMessage", "YesValue");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		receiveTaskHandler.messageReceived("NoMessage", "NoValue");
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Email1",
				new SystemOutWorkItemHandler());
		ksession.getWorkItemManager().registerWorkItemHandler("Email2",
				new SystemOutWorkItemHandler());
		receiveTaskHandler.setKnowledgeRuntime(ksession);
		ksession.getWorkItemManager().registerWorkItemHandler("Receive Task",
				receiveTaskHandler);
		// No
		processInstance = ksession.startProcess("com.sample.test");
		receiveTaskHandler.messageReceived("NoMessage", "NoValue");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		receiveTaskHandler.messageReceived("YesMessage", "YesValue");
	}

	public void testCallActivity() throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder();
		kbuilder.add(ResourceFactory
				.newClassPathResource("BPMN2-CallActivity.bpmn2"),
				ResourceType.BPMN2);
		kbuilder.add(ResourceFactory
				.newClassPathResource("BPMN2-CallActivitySubProcess.bpmn2"),
				ResourceType.BPMN2);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "oldValue");
		ProcessInstance processInstance = ksession.startProcess(
				"ParentProcess", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		assertEquals("new value",
				((WorkflowProcessInstance) processInstance).getVariable("y"));
	}

	public void testSubProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-SubProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.addEventListener(new DefaultProcessEventListener() {
			public void afterProcessStarted(ProcessStartedEvent event) {
				logger.debug(event.toString());
			}

			public void beforeVariableChanged(ProcessVariableChangedEvent event) {
				logger.debug(event.toString());
			}

			public void afterVariableChanged(ProcessVariableChangedEvent event) {
				logger.debug(event.toString());
			}
		});
		ProcessInstance processInstance = ksession.startProcess("SubProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}
	
	public void testSubProcessInvalid() throws Exception {
		try {
			KnowledgeBase kbase = createKnowledgeBase("BPMN2-SubProcessInvalid.bpmn2");
			fail("Shouldn't be able to compile invalid sub process");
		} catch (IllegalArgumentException e) {
			// do nothing
		}
	}
	
	public void testSubProcessWithTerminateEndEvent() throws Exception {
        KnowledgeBase kbase = createKnowledgeBase("BPMN2-SubProcessWithTerminateEndEvent.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        final List<String> list = new ArrayList<String>();
        ksession.addEventListener(new DefaultProcessEventListener() {
     
            public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
                list.add(event.getNodeInstance().getNodeName());
            }
        });
        ProcessInstance processInstance = ksession.startProcess("SubProcessTerminate");
        assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
        assertEquals(7, list.size());
    }
	
	public void testSubProcessWithTerminateEndEventProcessScope() throws Exception {
        KnowledgeBase kbase = createKnowledgeBase("BPMN2-SubProcessWithTerminateEndEventProcessScope.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        final List<String> list = new ArrayList<String>();
        ksession.addEventListener(new DefaultProcessEventListener() {
     
            public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
                list.add(event.getNodeInstance().getNodeName());
            }
        });
        ProcessInstance processInstance = ksession.startProcess("SubProcessTerminate");
        assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
        assertEquals(5, list.size());
    }

	public void testMultiInstanceLoopCharacteristicsProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MultiInstanceLoopCharacteristicsProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		List<String> myList = new ArrayList<String>();
		myList.add("First Item");
		myList.add("Second Item");
		params.put("list", myList);
		ProcessInstance processInstance = ksession.startProcess(
				"MultiInstanceLoopCharacteristicsProcess", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testMultiInstanceLoopCharacteristicsTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-MultiInstanceLoopCharacteristicsTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		List<String> myList = new ArrayList<String>();
		myList.add("First Item");
		myList.add("Second Item");
		params.put("list", myList);
		ProcessInstance processInstance = ksession.startProcess(
				"MultiInstanceLoopCharacteristicsTask", params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testEscalationBoundaryEvent() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EscalationBoundaryEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("EscalationBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testEscalationBoundaryEventInterrupting() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EscalationBoundaryEventInterrupting.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("EscalationBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		// TODO: check for cancellation of task
	}

	public void testErrorBoundaryEvent() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ErrorBoundaryEventInterrupting.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("ErrorBoundaryEvent");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testTimerBoundaryEventDuration() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerBoundaryEventDuration.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("TimerBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		Thread.sleep(1000);
		ksession = restoreSession(ksession, true);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testTimerBoundaryEventCycle1() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerBoundaryEventCycle1.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("TimerBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		Thread.sleep(1000);
		ksession = restoreSession(ksession, true);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testTimerBoundaryEventCycle2() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerBoundaryEventCycle2.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("TimerBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		Thread.sleep(1000);
		assertProcessInstanceActive(processInstance.getId(), ksession);
		Thread.sleep(1000);
		assertProcessInstanceActive(processInstance.getId(), ksession);
		ksession.abortProcessInstance(processInstance.getId());
		Thread.sleep(1000);
	}

	public void testTimerBoundaryEventInterrupting() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerBoundaryEventInterrupting.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("TimerBoundaryEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		Thread.sleep(1000);
		ksession = restoreSession(ksession, true);
		logger.debug("Firing timer");
		ksession.fireAllRules();
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testAdHocSubProcess() throws Exception {
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		List<Process> processes = processReader
				.read(SimpleBPMNProcessTest.class
						.getResourceAsStream("/BPMN2-AdHocSubProcess.bpmn2"));
		assertNotNull(processes);
		assertEquals(1, processes.size());
		RuleFlowProcess p = (RuleFlowProcess) processes.get(0);
		assertNotNull(p);
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		// logger.debug(XmlBPMNProcessDumper.INSTANCE.dump(p));
		kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
				XmlBPMNProcessDumper.INSTANCE.dump(p))), ResourceType.BPMN2);
		kbuilder.add(ResourceFactory
				.newClassPathResource("BPMN2-AdHocSubProcess.drl"),
				ResourceType.DRL);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ProcessInstance processInstance = ksession
				.startProcess("AdHocSubProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNull(workItem);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ksession.fireAllRules();
		logger.debug("Signaling Hello2");
		ksession.signalEvent("Hello2", null, processInstance.getId());
		workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
	}

	public void testAdHocSubProcessAutoComplete() throws Exception {
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		List<Process> processes = processReader
				.read(SimpleBPMNProcessTest.class
						.getResourceAsStream("/BPMN2-AdHocSubProcessAutoComplete.bpmn2"));
		assertNotNull(processes);
		assertEquals(1, processes.size());
		RuleFlowProcess p = (RuleFlowProcess) processes.get(0);
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		// logger.debug(XmlBPMNProcessDumper.INSTANCE.dump(p));
		kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
				XmlBPMNProcessDumper.INSTANCE.dump(p))), ResourceType.BPMN2);
		kbuilder.add(ResourceFactory
				.newClassPathResource("BPMN2-AdHocSubProcess.drl"),
				ResourceType.DRL);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ProcessInstance processInstance = ksession
				.startProcess("AdHocSubProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		WorkItem workItem = workItemHandler.getWorkItem();
		assertNull(workItem);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ksession.fireAllRules();
		workItem = workItemHandler.getWorkItem();
		assertNotNull(workItem);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				workItemHandler);
		ksession.getWorkItemManager().completeWorkItem(workItem.getId(), null);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testAdHocProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-AdHocProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("AdHocProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		logger.debug("Triggering node");
		ksession.signalEvent("Task1", null, processInstance.getId());
		assertProcessInstanceActive(processInstance.getId(), ksession);
		ksession.signalEvent("User1", null, processInstance.getId());
		assertProcessInstanceActive(processInstance.getId(), ksession);
		ksession.insert(new Person());
		ksession.signalEvent("Task3", null, processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testIntermediateCatchEventSignal() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventSignal.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		// now signal process instance
		ksession.signalEvent("MyMessage", "SomeValue", processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testIntermediateCatchEventMessage() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventMessage.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new SystemOutWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		// now signal process instance
		ksession.signalEvent("Message-HelloMessage", "SomeValue",
				processInstance.getId());
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testIntermediateCatchEventTimerDuration() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventTimerDuration.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		// now wait for 1 second for timer to trigger
		Thread.sleep(1000);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ksession.fireAllRules();
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testIntermediateCatchEventTimerCycle1() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventTimerCycle1.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		// now wait for 1 second for timer to trigger
		Thread.sleep(1000);
		ksession = restoreSession(ksession, true);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ksession.fireAllRules();
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testIntermediateCatchEventTimerCycle2() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventTimerCycle2.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		// now wait for 1 second for timer to trigger
		Thread.sleep(1000);
		assertProcessInstanceActive(processInstance.getId(), ksession);
		Thread.sleep(1000);
		assertProcessInstanceActive(processInstance.getId(), ksession);
		ksession.abortProcessInstance(processInstance.getId());
		Thread.sleep(1000);
	}

	public void testIntermediateCatchEventCondition() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventCondition.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		ksession = restoreSession(ksession, true);
		// now activate condition
		Person person = new Person();
		person.setName("Jack");
		ksession.insert(person);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testErrorEndEventProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ErrorEndEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("ErrorEndEvent");
		assertProcessInstanceAborted(processInstance.getId(), ksession);
	}

	public void testEscalationEndEventProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-EscalationEndEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("EscalationEndEvent");
		assertProcessInstanceAborted(processInstance.getId(), ksession);
	}

	public void testEscalationIntermediateThrowEventProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateThrowEventEscalation.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("EscalationIntermediateThrowEvent");
		assertProcessInstanceAborted(processInstance.getId(), ksession);
	}

	public void testCompensateIntermediateThrowEventProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateThrowEventCompensate.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("CompensateIntermediateThrowEvent");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testCompensateEndEventProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-CompensateEndEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("CompensateEndEvent");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		assertNodeTriggered(processInstance.getId(), "StartProcess", "Task", "CompensateEvent", "CompensateEvent2", "Compensate", "EndEvent");
	}

	public void testServiceTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ServiceProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Service Task",
				new ServiceTaskHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("s", "john");
		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) ksession
				.startProcess("ServiceProcess", params);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		assertEquals("Hello john!", processInstance.getVariable("s"));
	}

	public void testSendTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-SendTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Send Task",
				new SendTaskHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("s", "john");
		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) ksession
				.startProcess("SendTask", params);
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testReceiveTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ReceiveTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ReceiveTaskHandler receiveTaskHandler = new ReceiveTaskHandler(ksession);
		ksession.getWorkItemManager().registerWorkItemHandler("Receive Task",
				receiveTaskHandler);
		WorkflowProcessInstance processInstance = (WorkflowProcessInstance) ksession
				.startProcess("ReceiveTask");
		assertEquals(ProcessInstance.STATE_ACTIVE, processInstance.getState());
		ksession = restoreSession(ksession, true);
		receiveTaskHandler.messageReceived("HelloMessage", "Hello john!");
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
	}

	public void testConditionalStart() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-ConditionalStart.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Person person = new Person();
		person.setName("jack");
		ksession.insert(person);
		ksession.fireAllRules();
		person = new Person();
		person.setName("john");
		ksession.insert(person);
		ksession.fireAllRules();
	}

	public void testTimerStart() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerStart.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		final List<Long> list = new ArrayList<Long>();
		ksession.addEventListener(new DefaultProcessEventListener() {
			public void afterProcessStarted(ProcessStartedEvent event) {
				list.add(event.getProcessInstance().getId());
			}
		});
		Thread.sleep(250);
		assertEquals(0, list.size());
		for (int i = 0; i < 5; i++) {
			ksession.fireAllRules();
			Thread.sleep(500);
		}
		assertEquals(5, list.size());
	}

	public void testTimerStartCron() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-TimerStartCron.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		final List<Long> list = new ArrayList<Long>();
		ksession.addEventListener(new DefaultProcessEventListener() {
			public void afterProcessStarted(ProcessStartedEvent event) {
				list.add(event.getProcessInstance().getId());
			}
		});
		Thread.sleep(500);
		for (int i = 0; i < 5; i++) {
			ksession.fireAllRules();
			Thread.sleep(1000);
		}
		assertEquals(6, list.size());
	}

	public void testSignalStart() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-SignalStart.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		final List<Long> list = new ArrayList<Long>();
		ksession.addEventListener(new DefaultProcessEventListener() {
			public void afterProcessStarted(ProcessStartedEvent event) {
				list.add(event.getProcessInstance().getId());
			}
		});
		ksession.signalEvent("MySignal", "NewValue");
		assertEquals(1, list.size());
	}

	public void testSignalStartDynamic() throws Exception {
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		KnowledgeBase kbase2 = createKnowledgeBase("BPMN2-SignalStart.bpmn2");
		kbase.addKnowledgePackages(kbase2.getKnowledgePackages());
		final List<Long> list = new ArrayList<Long>();
		ksession.addEventListener(new DefaultProcessEventListener() {
			public void afterProcessStarted(ProcessStartedEvent event) {
				list.add(event.getProcessInstance().getId());
			}
		});
		ksession.signalEvent("MySignal", "NewValue");
		assertEquals(1, list.size());
	}

	public void testSignalEnd() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-SignalEndEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "MyValue");
		ksession.startProcess("SignalEndEvent", params);
	}

	public void testMessageStart() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MessageStart.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.signalEvent("Message-HelloMessage", "NewValue");
	}

	public void testMessageEnd() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-MessageEndEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Send Task",
				new SendTaskHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "MyValue");
		ProcessInstance processInstance = ksession.startProcess(
				"MessageEndEvent", params);
		assertEquals(ProcessInstance.STATE_COMPLETED,
				processInstance.getState());
	}

	public void testMessageIntermediateThrow() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateThrowEventMessage.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Send Task",
				new SendTaskHandler());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "MyValue");
		ProcessInstance processInstance = ksession.startProcess(
				"MessageIntermediateEvent", params);
		assertEquals(ProcessInstance.STATE_COMPLETED,
				processInstance.getState());
	}

	public void testSignalIntermediateThrow() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateThrowEventSignal.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("x", "MyValue");
		ProcessInstance processInstance = ksession.startProcess(
				"SignalIntermediateEvent", params);
		assertEquals(ProcessInstance.STATE_COMPLETED,
				processInstance.getState());
	}

	public void testNoneIntermediateThrow() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateThrowEventNone.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess(
				"NoneIntermediateEvent", null);
		assertEquals(ProcessInstance.STATE_COMPLETED,
				processInstance.getState());
	}

	public void testXpathExpression() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-XpathExpression.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream(
						"<instanceMetadata><user approved=\"false\" /></instanceMetadata>"
								.getBytes()));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", document);
		ProcessInstance processInstance = ksession.startProcess("XPathProcess",
				params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testOnEntryExitScript() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-OnEntryExitScriptProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new SystemOutWorkItemHandler());
		List<String> myList = new ArrayList<String>();
		ksession.setGlobal("list", myList);
		ProcessInstance processInstance = ksession
				.startProcess("OnEntryExitScriptProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		assertEquals(4, myList.size());
	}
	
	public void testOnEntryExitNamespacedScript() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-OnEntryExitNamespacedScriptProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new SystemOutWorkItemHandler());
		List<String> myList = new ArrayList<String>();
		ksession.setGlobal("list", myList);
		ProcessInstance processInstance = ksession
				.startProcess("OnEntryExitScriptProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		assertEquals(4, myList.size());
	}
	
	public void testOnEntryExitMixedNamespacedScript() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-OnEntryExitMixedNamespacedScriptProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new SystemOutWorkItemHandler());
		List<String> myList = new ArrayList<String>();
		ksession.setGlobal("list", myList);
		ProcessInstance processInstance = ksession
				.startProcess("OnEntryExitScriptProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		assertEquals(4, myList.size());
	}
	
	public void testOnEntryExitScriptDesigner() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-OnEntryExitDesignerScriptProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("MyTask",
				new SystemOutWorkItemHandler());
		List<String> myList = new ArrayList<String>();
		ksession.setGlobal("list", myList);
		ProcessInstance processInstance = ksession
				.startProcess("OnEntryExitScriptProcess");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
		assertEquals(4, myList.size());
	}
	
	public void testXORGateway() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-gatewayTest.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream(
						"<instanceMetadata><user approved=\"false\" /></instanceMetadata>"
								.getBytes()));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", document);
		params.put(
				"startMessage",
				DocumentBuilderFactory
						.newInstance()
						.newDocumentBuilder()
						.parse(new ByteArrayInputStream(
								"<task subject='foobar2'/>".getBytes()))
						.getFirstChild());
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

	public void testDataInputAssociations() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {
					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("hello world",
								workItem.getParameter("coId"));
					}
				});
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream("<user hello='hello world' />"
						.getBytes()));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", document.getFirstChild());
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
	}

	public void testDataInputAssociationsWithStringObject() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-string-object.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("hello", workItem.getParameter("coId"));
					}

				});
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", "hello");
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
	}

	public void FIXMEtestDataInputAssociationsWithLazyLoading()
			throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-lazy-creating.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("mydoc", ((Element) workItem
								.getParameter("coId")).getNodeName());
						assertEquals("mynode", ((Element) workItem
								.getParameter("coId")).getFirstChild()
								.getNodeName());
						assertEquals("user",
								((Element) workItem.getParameter("coId"))
										.getFirstChild().getFirstChild()
										.getNodeName());
						assertEquals("hello world",
								((Element) workItem.getParameter("coId"))
										.getFirstChild().getFirstChild()
										.getAttributes().getNamedItem("hello")
										.getNodeValue());
					}

				});
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream("<user hello='hello world' />"
						.getBytes()));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", document.getFirstChild());
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
	}

	public void FIXMEtestDataInputAssociationsWithString() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-string.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("hello", workItem.getParameter("coId"));
					}

				});
		ProcessInstance processInstance = ksession
				.startProcess("process", null);
	}

	public void testDataInputAssociationsWithStringWithoutQuotes()
			throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-string-no-quotes.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("hello", workItem.getParameter("coId"));
					}

				});
		ProcessInstance processInstance = ksession
				.startProcess("process", null);
	}

	public void testDataInputAssociationsWithXMLLiteral() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-xml-literal.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("id", ((org.w3c.dom.Node) workItem
								.getParameter("coId")).getNodeName());
						assertEquals("some text", ((org.w3c.dom.Node) workItem
								.getParameter("coId")).getFirstChild()
								.getTextContent());
					}

				});
		ProcessInstance processInstance = ksession
				.startProcess("process", null);
	}

	public void FIXMEtestDataInputAssociationsWithTwoAssigns() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataInputAssociations-two-assigns.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						assertEquals("foo", ((Element) workItem
								.getParameter("Comment")).getNodeName());
						// assertEquals("mynode", ((Element)
						// workItem.getParameter("Comment")).getFirstChild().getNodeName());
						// assertEquals("user", ((Element)
						// workItem.getParameter("Comment")).getFirstChild().getFirstChild().getNodeName());
						// assertEquals("hello world", ((Element)
						// workItem.getParameter("coId")).getFirstChild().getFirstChild().getAttributes().getNamedItem("hello").getNodeValue());
					}

				});
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.parse(new ByteArrayInputStream("<user hello='hello world' />"
						.getBytes()));
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("instanceMetadata", document.getFirstChild());
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
	}

	public void testDataOutputAssociationsforHumanTask() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataOutputAssociations-HumanTask.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						DocumentBuilderFactory factory = DocumentBuilderFactory
								.newInstance();
						DocumentBuilder builder;
						try {
							builder = factory.newDocumentBuilder();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							throw new RuntimeException(e);
						}
						final Map<String, Object> results = new HashMap<String, Object>();

						// process metadata
						org.w3c.dom.Document processMetadaDoc = builder
								.newDocument();
						org.w3c.dom.Element processMetadata = processMetadaDoc
								.createElement("previoustasksowner");
						processMetadaDoc.appendChild(processMetadata);
						// org.w3c.dom.Element procElement =
						// processMetadaDoc.createElement("previoustasksowner");
						processMetadata
								.setAttribute("primaryname", "my_result");
						// processMetadata.appendChild(procElement);
						results.put("output", processMetadata);

						mgr.completeWorkItem(workItem.getId(), results);
					}

				});
		Map<String, Object> params = new HashMap<String, Object>();
		ProcessInstance processInstance = ksession.startProcess("process",
				params);
	}

	public void testDataOutputAssociations() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataOutputAssociations.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						try {
							Document document = DocumentBuilderFactory
									.newInstance()
									.newDocumentBuilder()
									.parse(new ByteArrayInputStream(
											"<user hello='hello world' />"
													.getBytes()));
							Map<String, Object> params = new HashMap<String, Object>();
							params.put("output", document.getFirstChild());
							mgr.completeWorkItem(workItem.getId(), params);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}

					}

				});
		ProcessInstance processInstance = ksession
				.startProcess("process", null);
	}

	public void testDataOutputAssociationsXmlNode() throws Exception {
		KnowledgeBase kbase = createKnowledgeBaseWithoutDumper("BPMN2-DataOutputAssociations-xml-node.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new WorkItemHandler() {

					public void abortWorkItem(WorkItem manager,
							WorkItemManager mgr) {

					}

					public void executeWorkItem(WorkItem workItem,
							WorkItemManager mgr) {
						try {
							Document document = DocumentBuilderFactory
									.newInstance()
									.newDocumentBuilder()
									.parse(new ByteArrayInputStream(
											"<user hello='hello world' />"
													.getBytes()));
							Map<String, Object> params = new HashMap<String, Object>();
							params.put("output", document.getFirstChild());
							mgr.completeWorkItem(workItem.getId(), params);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}

					}

				});
		ProcessInstance processInstance = ksession
				.startProcess("process", null);
	}

	public void testLinkIntermediateEvent() throws Exception {

		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateLinkEvent.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession
				.startProcess("linkEventProcessExample");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);

	}

	public void testLinkEventCompositeProcess() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-LinkEventCompositeProcess.bpmn2");
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		ProcessInstance processInstance = ksession.startProcess("Composite");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_COMPLETED);
	}

    public void testIntermediateCatchEventConditionFilterByProcessInstance()throws Exception {
        KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventConditionFilterByProcessInstance.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        Map<String,Object> params1 = new HashMap<String,Object>();
        params1.put("personId", Long.valueOf(1L));
        Person person1 = new Person();
        person1.setId(1L);
        WorkflowProcessInstance pi1 = (WorkflowProcessInstance) ksession.createProcessInstance("IntermediateCatchEventConditionFilterByProcessInstance", params1);
        long pi1id = pi1.getId();
        
        ksession.insert(pi1);
        FactHandle personHandle1 = ksession.insert(person1);
        
        ksession.startProcessInstance(pi1.getId());
        
        Map<String,Object> params2 = new HashMap<String,Object>();
        params2.put("personId", Long.valueOf(2L));
        Person person2 = new Person();
        person2.setId(2L);
        
        WorkflowProcessInstance pi2 = (WorkflowProcessInstance) ksession.createProcessInstance("IntermediateCatchEventConditionFilterByProcessInstance", params2);
        long pi2id = pi2.getId();
        
        ksession.insert(pi2);
        FactHandle personHandle2 = ksession.insert(person2);
        
        ksession.startProcessInstance(pi2.getId());
        
        person1.setName("John");
        ksession.update(personHandle1, person1);
        
        
        
        assertNull("First process should be completed", ksession.getProcessInstance(pi1id));
        assertNotNull("Second process should NOT be completed", ksession.getProcessInstance(pi2id));

    }

    
    public void testIntermediateCatchEventTimerCycleWithError() throws Exception {
        KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventTimerCycleWithError.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
                new DoNothingWorkItemHandler());
        ProcessInstance processInstance = ksession
                .startProcess("IntermediateCatchEvent");
        assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
        // now wait for 1 second for timer to trigger
        Thread.sleep(1000);
        assertProcessInstanceActive(processInstance.getId(), ksession);
        ((WorkflowProcessInstance)ksession.getProcessInstance(processInstance.getId())).setVariable("x", 0);
        Thread.sleep(1000);
        assertProcessInstanceActive(processInstance.getId(), ksession);
        Thread.sleep(1000);
        assertProcessInstanceActive(processInstance.getId(), ksession);
        
        Integer xValue = (Integer) ((WorkflowProcessInstance)processInstance).getVariable("x");
        assertEquals(new Integer(2), xValue);
        
        ksession.abortProcessInstance(processInstance.getId());
        assertProcessInstanceAborted(processInstance.getId(), ksession);
    }
    
    public void testUserTaskWithBooleanOutput() throws Exception {
        KnowledgeBase kbase = createKnowledgeBase("BPMN2-UserTaskWithBooleanOutput.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        TestWorkItemHandler workItemHandler = new TestWorkItemHandler();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
                workItemHandler);
        ProcessInstance processInstance = ksession.startProcess("com.sample.boolean");
        assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
        ksession = restoreSession(ksession, true);
        WorkItem workItem = workItemHandler.getWorkItem();
        assertNotNull(workItem);
        assertEquals("john", workItem.getParameter("ActorId"));
        HashMap<String, Object> output = new HashMap<String, Object>();
        output.put("isCheckedCheckbox", "true");
        ksession.getWorkItemManager().completeWorkItem(workItem.getId(), output);
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
    }
    
	public void testIntermediateCatchEventTimerDurationWithBusinessCalendar() throws Exception {
		KnowledgeBase kbase = createKnowledgeBase("BPMN2-IntermediateCatchEventTimerDurationDays.bpmn2");
		
		KnowledgeSessionConfiguration conf = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
		conf.setOption(ClockTypeOption.get( ClockType.PSEUDO_CLOCK.getId() ));
		
		Environment env = EnvironmentFactory.newEnvironment();
		
		StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession(conf, env);
		
		SessionPseudoClock clock = ( SessionPseudoClock) ksession.getSessionClock();
		// use a fixed date to be always Wednesday as timer is set to 3 days
		Date fixedDate = parseToDateWithTime("2012-02-01 12:00");
        clock.advanceTime( fixedDate.getTime() - clock.getCurrentTime(), TimeUnit.MILLISECONDS ); 
        
        System.out.println(new Date(clock.getCurrentTime()));
        // use empty config to make use of defaults
 		Properties config = new Properties();
 		env.set("jbpm.business.calendar", new BusinessCalendarImpl(config, clock));
        
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task",
				new DoNothingWorkItemHandler());
		ProcessInstance processInstance = ksession
				.startProcess("IntermediateCatchEvent");
		assertTrue(processInstance.getState() == ProcessInstance.STATE_ACTIVE);
		// advance time for 3 days (scheduled time on timer)
		clock.advanceTime(3, TimeUnit.DAYS);
		ksession.fireAllRules();
		// timer should not fire as it's based on business calendar
		assertProcessInstanceActive(processInstance.getId(), ksession);
		// advance for 2 more days to move to Monday when timer should fire
		clock.advanceTime(2, TimeUnit.DAYS);
		ksession.fireAllRules();
		assertProcessInstanceCompleted(processInstance.getId(), ksession);
		ksession.dispose();
	}
	
    
    public void testTerminateWithinSubprocessEnd() throws Exception {
    	KnowledgeBase kbase = createKnowledgeBase("subprocess/BPMN2-SubprocessWithParallelSpitTerminate.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        ProcessInstance processInstance = ksession.startProcess("BPMN2-SubprocessWithParallelSpitTerminate");
        
        ksession.signalEvent("signal1", null, processInstance.getId());
        
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        
    }
        
    public void testTerminateEnd() throws Exception {
    	KnowledgeBase kbase = createKnowledgeBase("BPMN2-ParallelSpitTerminate.bpmn2");
        StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
        ProcessInstance processInstance = ksession.startProcess("BPMN2-ParallelSpitTerminate");
        
        ksession.signalEvent("Signal_1", null, processInstance.getId());
        
        assertProcessInstanceCompleted(processInstance.getId(), ksession);
        
    }

	
	private Date parseToDateWithTime(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        Date testTime;
        try {
            testTime = sdf.parse(dateString);
            
            return testTime;
        } catch (ParseException e) {
            return null;
        }        
    }

	private KnowledgeBase createKnowledgeBase(String process) throws Exception {
		KnowledgeBaseFactory
				.setKnowledgeBaseServiceFactory(new KnowledgeBaseFactoryServiceImpl());
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNExtensionsSemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		List<Process> processes = processReader
				.read(SimpleBPMNProcessTest.class.getResourceAsStream("/"
						+ process));
		for (Process p : processes) {
			RuleFlowProcess ruleFlowProcess = (RuleFlowProcess) p;
			logger.debug(XmlBPMNProcessDumper.INSTANCE
					.dump(ruleFlowProcess));
			kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
					XmlBPMNProcessDumper.INSTANCE.dump(ruleFlowProcess))),
					ResourceType.BPMN2);
		}
		kbuilder.add(ResourceFactory
				.newReaderResource(new InputStreamReader(
						SimpleBPMNProcessTest.class.getResourceAsStream("/"
								+ process))), ResourceType.BPMN2);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		return kbase;
	}

	private KnowledgeBase createKnowledgeBaseWithoutDumper(String process)
			throws Exception {
		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
				.newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNDISemanticModule());
		((PackageBuilderConfiguration) conf)
				.addSemanticModule(new BPMNExtensionsSemanticModule());
		// ProcessDialectRegistry.setDialect("XPath", new XPathDialect());
		XmlProcessReader processReader = new XmlProcessReader(
				((PackageBuilderConfiguration) conf).getSemanticModules(),
				getClass().getClassLoader());
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder(conf);
		kbuilder.add(ResourceFactory
				.newReaderResource(new InputStreamReader(
						SimpleBPMNProcessTest.class.getResourceAsStream("/"
								+ process))), ResourceType.BPMN2);
		if (!kbuilder.getErrors().isEmpty()) {
			for (KnowledgeBuilderError error : kbuilder.getErrors()) {
				logger.error(error.toString());
			}
			throw new IllegalArgumentException(
					"Errors while parsing knowledge base");
		}
		KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
		kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
		return kbase;
	}

}
