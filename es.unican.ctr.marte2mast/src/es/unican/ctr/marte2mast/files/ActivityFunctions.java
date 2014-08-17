package es.unican.ctr.marte2mast.files;

/*---------------------------------------------------------------------
 --                           Marte2Mast                              --
 --     Converter of Schedulability analysis models made with UML2    --
 --   and The UML Profile for MARTE to MAST, the Analysis Suite for   --
 --                      Real-Time Applications                       --
 --                                                                   --
 --                     Copyright (C) 2010-2011                       --
 --                 Universidad de Cantabria, SPAIN                   --
 --                                                                   --
 --                                                                   --
 --           URL: http://mast.unican.es/umlmast/marte2mast           --
 --                                                                   --
 --  Authors: Alvaro Garcia Cuesta   alvaro@binarynonsense.com        --
 --           Julio Medina           julio.medina@unican.es           --
 --                                                                   --
 -- This program is free software; you can redistribute it and/or     --
 -- modify it under the terms of the GNU General Public               --
 -- License as published by the Free Software Foundation; either      --
 -- version 2 of the License, or (at your option) any later version.  --
 --                                                                   --
 -- This program is distributed in the hope that it will be useful,   --
 -- but WITHOUT ANY WARRANTY; without even the implied warranty of    --
 -- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU --
 -- General Public License for more details.                          --
 --                                                                   --
 -- You should have received a copy of the GNU General Public         --
 -- License along with this program; if not, write to the             --
 -- Free Software Foundation, Inc., 59 Temple Place - Suite 330,      --
 -- Boston, MA 02111-1307, USA.                                       --
 --                                                                   --
 ---------------------------------------------------------------------*/

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Operation;

/**
 * This class contains a series of methods used to extract the relevant data
 * from the model's activity diagrams and generate the MAST's text for
 * operations and transactions.
 * 
 * <p>
 * Two of the methods are called from Acceleo's marte2mast.mtl template:
 * extractActivityData and getOperationsTransactionsText
 */
public class ActivityFunctions {

	// /data///////////////////////
	static String mSimpleOperationsText = "";
	static String mEnclosingOperationsText = "";
	static String mCompositeOperationsText = "";
	static String mTransactionsText = "";
	/** Used to avoid repeating an operation in the MAST file */
	static Vector<String> mSimpleOperationsNames = new Vector<String>();

	static public class eventHandler {
		String mInputEvent;
		String mOutputEvent;
		String mOperation;
		String mServer;

		eventHandler() {
			mInputEvent = "";
			mOutputEvent = "";
			mOperation = "";
			mServer = "";
		}
	}// end eventHandler

	static Vector<eventHandler> mEventHandlerList = new Vector<eventHandler>();
	static eventHandler mEventHandler = new eventHandler();
	static Vector<String> mEventHandlerOperationList = new Vector<String>();
	static String mNextInputEvent = "";
	static String mActualThread = "";
	static int mInternalEventCounter = 0;
	static boolean mDummyOperationCreated = false;
	static int mCompositeOperationsCounter = 0;
	static String mTransactionName = "";

	static Vector<String> mInternalEventList = new Vector<String>();

	/**
	 * Used to store a map of internal events to last event operation (step). we
	 * need this for Mast.java so we can update the model with the data results
	 * from MAST
	 */
	static HashMap<String, String> mIntEvent2OperationMap = new HashMap<String, String>();
	/**
	 * Used to store a list of lists of root-last children-last children...
	 * elements we need to update the UML model
	 */
	// static Vector<Element> mTransactionLastChildrenList = new
	// Vector<Element>();
	static Vector<Vector<fatherChildrenStruct>> mTransactionFatherChildrensLists = new Vector<Vector<fatherChildrenStruct>>();

	static public class fatherChildrenStruct {
		Element mElement;
		String mInternalEventName;

		fatherChildrenStruct() {
			mElement = null;
			mInternalEventName = "";
		}
	}// end eventHandler

	/**
	 * Used to store a list of root-last children-last children... elements we
	 * need to update the UML model
	 */
	static Vector<fatherChildrenStruct> mFatherChildrenList = new Vector<fatherChildrenStruct>();

	// ////////////////////////////
	/**
	 * Extracts the context data from the "SaAnalysisContext" stereotyped
	 * element. If the invoke option, for the mast tool, is set to false,
	 * Mast.mOpenGmast will be set to false, in any other case it will be set to
	 * true.
	 * 
	 * <p>
	 * we also check for a mode text here.
	 * 
	 * @param theContext
	 *            "SaAnalysisContext" stereotyped element
	 */
	public static void extractContextInfo(Element theContext) {

		Date todaysDate = new java.util.Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String formattedDate = formatter.format(todaysDate);
		Mast.mDate = todaysDate;

		if (HelperFunctions.getStereotypeProperty(theContext, "SaAnalysisContext", "context") != null) {
			// example:
			// (tool=mast,options=(invoke=true,recoverResults=true,overwriteResults=false,overwriteOutputModel=true,mode='The_mode:text._-'))
			String inputText = HelperFunctions.getStereotypeProperty(theContext, "SaAnalysisContext", "context").toString();
			// mast invoke true/false (default=true)
			if (HelperFunctions.parseContext(inputText, "mast", "invoke").equals("false")) {
				Mast.mOpenGmast = false;
			} else {
				Mast.mOpenGmast = true;
			}
			// mast recover results true/false (default=false)
			if (HelperFunctions.parseContext(inputText, "mast", "recoverResults").equals("true")) {
				Mast.mRecoverResults = true;
			} else {
				Mast.mRecoverResults = false;//
			}
			// mast modeID ->String
			// 'alphanumerics_and_numbers_points_underscore_and_score', no
			// commas o spaces?
			if (HelperFunctions.parseContext(inputText, "mast", "modeID") != null && !HelperFunctions.parseContext(inputText, "mast", "modeID").equals("MALFORMED INPUT") && !HelperFunctions.parseContext(inputText, "mast", "modeID").equals("") && !HelperFunctions.parseContext(inputText, "mast", "modeID").equals("''")) {
				Mast.mModeID = HelperFunctions.parseContext(inputText, "mast", "modeID");
				Mast.mModeID = Mast.mModeID.replace("'", "");
			} else {
				// auto generate mode text
				// ref: printHeaderData
				Mast.mModeID = formattedDate;
			}
			// mast overwriteResults true/false (default=false)
			if (HelperFunctions.parseContext(inputText, "mast", "overwriteResults").equals("true")) {
				Mast.mOverwriteResults = true;
			} else {
				Mast.mOverwriteResults = false;
			}
			// mast overwriteOutputModel???? true/false (default=true)
			if (HelperFunctions.parseContext(inputText, "mast", "overwriteOutputModel").equals("false")) {
				Mast.mOverwriteOutputModel = false;
			} else {
				Mast.mOverwriteOutputModel = true;
			}
		} else {// no context info
				// set default values
			Mast.mOpenGmast = true;
			Mast.mRecoverResults = false;
			// auto generate mode text
			// ref: printHeaderData
			// Log.println("generating mode text: "+formattedDate);
			Mast.mModeID = formattedDate;
			Mast.mOverwriteResults = false;
			Mast.mOverwriteOutputModel = true;
		}// end SaAnalysisContext

		// Future work: check other tools

	}// end extractContextInfo

	// ///////////////////////////

	/**
	 * Creates a new name for the internal event from the old one. Right now it
	 * outputs the input name, so maybe this isn't needed.
	 * 
	 * @param name
	 *            the original name
	 */
	public static String createInternalEventText(String name) {
		// [TODO] check if I'll need to expand this in the future or I can get
		// rid of it
		String theText = "" + name;
		return theText;
	}// getInternalEvent

	/**
	 * Creates a MAST simple operation with execution times set to 0.0 called
	 * DummyOperation.
	 * 
	 * @return the String "DummyOperation"
	 */
	public static String createDummyOperation() {
		if (!mDummyOperationCreated) {
			mSimpleOperationsText += "\nOperation (\n";
			mSimpleOperationsText += "    Type                        => Simple,\n";
			mSimpleOperationsText += "    Name                        => DummyOperation";
			mSimpleOperationsText += ",\n    Worst_Case_Execution_Time   => 0.0";
			mSimpleOperationsText += ",\n    Avg_Case_Execution_Time     => 0.0";
			mSimpleOperationsText += ",\n    Best_Case_Execution_Time    => 0.0";
			mSimpleOperationsText += "\n);\n";
			mDummyOperationCreated = true;
		}
		return "DummyOperation";
	}// createDummyOperation

	/**
	 * Recursive method that checks if a given element ("SaStep" stereotyped),
	 * or any of its 'children' (in the subUsage property or an associated
	 * Activity) has a value set for the concuRes property of the stereotype.
	 * 
	 * @param root
	 *            the initial element
	 * 
	 * @param checkThisOne
	 *            true if you want to check the root element or only its
	 *            'children'
	 * 
	 * @return <code>true</code> if an element with concurRes was found,
	 *         <code>false</code> if not
	 */
	// used in extractStepData:
	public static boolean treeHasConcurRes(NamedElement root, boolean checkThisOne) {
		if (HelperFunctions.hasStereotype(root, "SaStep")) {
			// Log.println("checking  "+root.getName());
			if (checkThisOne) {
				if (HelperFunctions.getStereotypeProperty(root, "SaStep", "concurRes") != null) {
					Log.debugPrintln("treeHasConcurRes: stereotype " + root.getName() + " has concurRes");
					return true;
				}// if concurRes
			}// if check
				// check children
				// subUsages--------------------------
			List<Operation> simpleOperations = HelperFunctions.getSubUsage(root, "SaStep", "subUsage");
			if (simpleOperations.size() != 0) {// subUsage has data
				for (Operation anOperation : simpleOperations) {
					if (treeHasConcurRes(anOperation, true))
						return true;
				}
			}// if subUsage data
				// activities--------------------------
			if (root instanceof Operation) {// code taken, modified, from
											// getActivityFromOperation
				Operation theOperation = (Operation) root;
				Element initialNode = getActivityFromOperation(theOperation);
				if (initialNode != null) {
					Vector<NamedElement> pathElements = new Vector<NamedElement>();
					pathElements = extractActivityElements(initialNode);
					for (NamedElement anElement : pathElements) {
						if (treeHasConcurRes(anElement, true))
							return true;
					}// end for
				}
			}// end if operation

		}// if saStep
			// Log.println("checking tree: activity operation - nothing found");
		return false;
	}// treeHasConcurRes

	/**
	 * Recursive method that checks if a given element ("SaStep" stereotyped),
	 * or any of its 'children' (in the subUsage property or an associated
	 * Activity) has a value set for the execTime property of the stereotype.
	 * 
	 * @param root
	 *            the initial element
	 * 
	 * @param checkThisOne
	 *            true if you want to check the root element or only its
	 *            'children'
	 * 
	 * @return <code>true</code> if an element with execTime was found,
	 *         <code>false</code> if not
	 */
	public static boolean treeHasExecTime(NamedElement root, boolean checkThisOne) {
		if (HelperFunctions.hasStereotype(root, "SaStep")) {

			if (checkThisOne) {
				if (HelperFunctions.getStereotypeProperty(root, "SaStep", "execTime") != null) {
					return true;
				}// if concurRes
			}// if check
				// check children
				// subUsages--------------------------
			List<Operation> simpleOperations = HelperFunctions.getSubUsage(root, "SaStep", "subUsage");
			if (simpleOperations.size() != 0) {// subUsage has data
				for (Operation anOperation : simpleOperations) {
					if (treeHasExecTime(anOperation, true))
						return true;
				}
			}// if subUsage data
				// activities--------------------------
			if (root instanceof Operation) {// code taken, modified, from
											// getActivityFromOperation
				Operation theOperation = (Operation) root;
				Element initialNode = getActivityFromOperation(theOperation);
				if (initialNode != null) {
					Vector<NamedElement> pathElements = new Vector<NamedElement>();
					pathElements = extractActivityElements(initialNode);
					for (NamedElement anElement : pathElements) {
						if (treeHasExecTime(anElement, true))
							return true;
					}// end for
				}
			}// end if operation

		}// if saStep
		return false;
	}// treeHasExecTime

	/**
	 * Adds all the data left to complete the current event handler (MAST)
	 * stored in 'mEventHandler' and adds it to the 'mEventHandlerList' list of
	 * event handlers. A new event handler structure is then created (in
	 * 'mEventHandler').
	 * 
	 * <p>
	 * When a handler is closed, a new composite operation (MAST) is created,
	 * containing all the operations stored in the list
	 * 'mEventHandlerOperationList' (the list is then cleared for later use).
	 * This list is filled by 'extractStepData'.
	 * 
	 * <p>
	 * Every time the current thread changes, or we reach the end of the
	 * transaction, we have to call this method to complete and store the opened
	 * handler.
	 */
	public static void closeEventHandler() {
		// add in event to list
		// String tempEventName = mTransactionName+"__Internal_Event_" +
		// (++mInternalEventCounter);
		String tempEventName = mEventHandler.mOutputEvent;
		Log.debugPrintln("adding internal event to list: " + tempEventName);
		mInternalEventList.add(createInternalEventText(tempEventName));
		// mEventHandler.mOutputEvent= tempEventName;
		mNextInputEvent = tempEventName;// next one will start from the output
										// of this one
		// create common operation if needed and add it
		if (mEventHandlerOperationList.size() > 0) {

			if (mEventHandlerOperationList.size() == 1) {// just one -> no need
															// to create one
				mEventHandler.mOperation = mEventHandlerOperationList.get(0);
			} else {// more than one
					// create composite of listed ops... name?
				String tempText = "";
				String tempName = "";
				String tempList = "";
				for (int i = 0; i < mEventHandlerOperationList.size(); i++) {
					if (i > 0)
						tempName += "__";
					tempName += mEventHandlerOperationList.get(i);
					tempList += mEventHandlerOperationList.get(i);
					if (i < mEventHandlerOperationList.size() - 1)
						tempList += ", ";
				}

				tempName = "Composite_Operation_" + (++mCompositeOperationsCounter);

				tempText += "\nOperation (\n";
				tempText += "    Type                        => Composite,\n";
				tempText += "    Name                        => " + tempName;
				tempText += ",\n    Composite_Operation_List    => ( ";
				tempText += tempList;
				tempText += " )";
				tempText += "\n);\n";
				// add to composite list
				mCompositeOperationsText += tempText;
				tempText = "";
				Log.debugPrintln("EXTRACT: adding composite operation: " + tempName);
				mEventHandler.mOperation = tempName;
			}

			// save last op to in event
			// lower case 'cause mast results names are in lower case
			String lastEvOperation = mEventHandlerOperationList.get(mEventHandlerOperationList.size() - 1).toLowerCase();
			// Log.println(tempEventName.toLowerCase()+lastEvOperation);
			mIntEvent2OperationMap.put(tempEventName.toLowerCase(), lastEvOperation);

		} else {// closing a handler with no operation -> use dummy operation
			Log.debugPrintln("adding dummy operation to handler");
			mEventHandler.mOperation = createDummyOperation();
		}
		// clear ops list
		mEventHandlerOperationList.clear();
		mEventHandlerList.add(mEventHandler);// add to list
		// create new event handler
		mEventHandler = new eventHandler();
		mEventHandler.mOutputEvent = mTransactionName + "__Internal_Event_" + (++mInternalEventCounter);
	}// closeEventHandler

	// //used with 'simple' groups of ops/steps (no concurRes inside) to ignore
	// composites of just one element
	// public static String getHandlerOperationName(NamedElement theElement) {
	// List<Operation> simpleOperations =
	// HelperFunctions.getSubUsage(theElement,"SaStep","subUsage");
	// if(simpleOperations.size()==1){//skip it
	// for(Operation anOperation:simpleOperations){//there should be just one
	// //return anOperation.getName();
	// return HelperFunctions.getElementLongName(anOperation);
	// }
	// }else{
	// //return theElement.getName();
	// return HelperFunctions.getElementLongName(theElement);
	//
	// }
	// return null;
	// }

	/**
	 * Given an element ('SaStep' stereotyped), it returns the corresponding
	 * text for a simple operation (MAST). It also stores the name of the
	 * operation in a list called 'mSimpleOperationsNames' in order to not
	 * repeating operations in the MAST files when they are used more than once.
	 * 
	 * @param theElement
	 *            the UML element
	 * 
	 * @return the simple operation's text in MAST format
	 */
	public static String getSimpleOperationText(NamedElement theElement) {

		// check if it was previously written
		for (String operationName : mSimpleOperationsNames) {
			if (operationName.equals(HelperFunctions.getElementLongName(theElement))) {
				Log.debugPrintln("Operation " + HelperFunctions.getElementLongName(theElement) + " already in MAST file");
				return "";
			}
		}
		// add op name to list
		mSimpleOperationsNames.add(HelperFunctions.getElementLongName(theElement));
		// Log.println("writting "+HelperFunctions.getElementLongName(theElement)+" operation to file");

		String outputText = "";
		outputText += "\nOperation (\n";
		outputText += "    Type                        => Simple,\n";
		outputText += "    Name                        => " + HelperFunctions.getElementLongName(theElement);
		// print step data
		if (HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime") != null) {

			if (HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best").equals("MALFORMED INPUT") && HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst").equals("MALFORMED INPUT")) {
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value").equals("MALFORMED INPUT")) {
					// worst
					outputText += ",\n    Worst_Case_Execution_Time   => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value");
				} else {
					// !!!no valid data
					// is this warning useful/necessary?
					Log.debugPrintln("WARNING: the operation " + theElement.getName() + " doesn't have W/A/B case execution times " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
				}

			} else {

				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst").equals("MALFORMED INPUT")) {
					// worst
					outputText += ",\n    Worst_Case_Execution_Time   => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst");
				}
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best").equals("MALFORMED INPUT")) {
					// best
					outputText += ",\n    Best_Case_Execution_Time    => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best");
				}
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value").equals("MALFORMED INPUT")) {
					// average
					outputText += ",\n    Avg_Case_Execution_Time     => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value");
				}
			}

		} else {
			// is this warning useful/necessary?
			// Log.debugPrintln("WARNING: the operation "+
			// HelperFunctions.getElementLongName(theElement)
			// +" doesn't have W/A/B case execution times " +
			// Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
			outputText += ",\n    Worst_Case_Execution_Time   => 0.0";
			outputText += ",\n    Avg_Case_Execution_Time     => 0.0";
			outputText += ",\n    Best_Case_Execution_Time    => 0.0";
		}// end if execTime

		if (!HelperFunctions.getStereotypeRefListNames(theElement, "SaStep", "sharedRes").toString().equals("")) {
			outputText += ",\n    Shared_Resources_List       => ( " + HelperFunctions.getStereotypeRefListNames(theElement, "SaStep", "sharedRes").toString() + " )";
		}
		// outputText +=
		// "\n    --Overridden_Sched_Parameters => Overridden_Sched_Parameters,-- NOT FOUND MARTE";
		outputText += "\n);\n";

		return outputText;
	}

	/**
	 * Given an element ('SaStep stereotyped) and a vector containing the names
	 * of the operations it contains, it returns the corresponding text for a
	 * composite operation (MAST).
	 * 
	 * @param theElement
	 *            the UML element
	 * 
	 * @param simpleOperations
	 *            a vector containing the names of the operations it contains
	 * 
	 * @return the composite operation's text in MAST format
	 */
	public static String getCompositeOperationText(NamedElement theElement, Vector<String> simpleOperations) {

		String outputText = "";

		if (simpleOperations != null) {
			outputText += "\nOperation (\n";
			outputText += "    Type                        => Composite,\n";
			outputText += "    Name                        => " + HelperFunctions.getElementLongName(theElement);
			outputText += ",\n    Composite_Operation_List    => ( ";

			for (int i = 0; i < simpleOperations.size(); i++) {
				outputText += simpleOperations.get(i);
				if (i < simpleOperations.size() - 1)
					outputText += ", ";
			}
			outputText += " )";
			outputText += "\n);\n";
		} else {
			Log.println("ERROR: the operation " + HelperFunctions.getElementLongName(theElement) + " should have a component operation list " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
		}

		return outputText;
	}

	/**
	 * Given an element ('SaStep stereotyped) and a vector containing the names
	 * of the operations it contains, it returns the corresponding text for an
	 * enclosing operation (MAST).
	 * 
	 * @param theElement
	 *            the UML element
	 * 
	 * @param simpleOperations
	 *            a vector containing the names of the operations it contains
	 * 
	 * @return the enclosing operation's text in MAST format
	 */
	public static String getEnclosingOperationText(NamedElement theElement, Vector<String> simpleOperations) {

		String outputText = "";
		outputText += "\nOperation (\n";
		outputText += "    Type                        => Enclosing,\n";
		outputText += "    Name                        => " + HelperFunctions.getElementLongName(theElement);
		// print step data
		if (HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime") != null) {

			if (HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best").equals("MALFORMED INPUT") && HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst").equals("MALFORMED INPUT")) {
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value").equals("MALFORMED INPUT")) {
					// worst
					outputText += ",\n    Worst_Case_Execution_Time   => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value");
				} else {
					// !!!no valid data
					// is this warning useful/necessary?
					Log.debugPrintln("WARNING: the operation " + HelperFunctions.getElementLongName(theElement) + " doesn't have W/A/B case execution times " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
				}

			} else {

				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst").equals("MALFORMED INPUT")) {
					// worst
					outputText += ",\n    Worst_Case_Execution_Time   => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "worst");
				}
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best").equals("MALFORMED INPUT")) {
					// best
					outputText += ",\n    Best_Case_Execution_Time    => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "best");
				}
				if (!HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value").equals("MALFORMED INPUT")) {
					// average
					outputText += ",\n    Avg_Case_Execution_Time     => ";
					outputText += HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime").toString(), "value");
				}
			}

		} else {
			// is this warning useful/necessary?
			Log.debugPrintln("WARNING: the operation " + HelperFunctions.getElementLongName(theElement) + " doesn't have W/A/B case execution times " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
		}// end if execTime

		if (simpleOperations != null) {
			outputText += ",\n    Composite_Operation_List    => ( ";
			// old:
			// for(int i=0;i<simpleOperations.size();i++){
			// outputText +=
			// HelperFunctions.getElementLongName(simpleOperations.get(i));//simpleOperations.get(i).getName();
			// if(i<simpleOperations.size()-1) outputText += ", ";
			// }
			for (int i = 0; i < simpleOperations.size(); i++) {
				outputText += simpleOperations.get(i);
				if (i < simpleOperations.size() - 1)
					outputText += ", ";
			}
			outputText += " )";
		} else {
			Log.println("ERROR: the operation " + HelperFunctions.getElementLongName(theElement) + " should have a component operation list " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
		}

		outputText += "\n);\n";

		return outputText;
	}

	/**
	 * Given an element ('GaWorkloadEvent' stereotyped) containing valid Arrival
	 * Pattern data (MARTE), it returns the text for an external event (MAST)
	 * 
	 * @param elt
	 *            an element ('GaWorkloadEvent' stereotyped)
	 * 
	 * @return the external event's text in MAST format
	 */
	public static String getExternalEventText(Element elt) {
		String outputText = "";

		if (HelperFunctions.getArrivalPatternType(elt) == null) {// -----------------------------
																	// ERROR
																	// ----------------
			Log.println("Warning: no valid pattern found in GaWorkloadEvent " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));// changed
																																							// from
																																							// error
																																							// to
																																							// warning
		}
		// ----------------------------- PERIODIC ----------------
		else if (HelperFunctions.getArrivalPatternType(elt).equals("periodic")) {
			// i.e. periodic(period=(value=0.6,unit=ms))
			outputText += "        (Type 		=> Periodic,\n";
			outputText += "         Name 		=> " + ((NamedElement) elt).getName();
			if (HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern") != null) {
				outputText += ",\n         Period 	=> " + HelperFunctions.parseNFP_Duration(HelperFunctions.parseSchedParams((String) HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern"), "periodic", "period"), "value") + ")";
			} else {
				Log.println("ERROR: no valid pattern found in GaWorkloadEvent " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
			}
		}
		// ----------------------------- SPORADIC ----------------
		else if (HelperFunctions.getArrivalPatternType(elt).equals("sporadic")) {
			// i.e. sporadic(minInterarrival=(value=1.0,unit=s))
			outputText += "        (Type 		        => Sporadic,\n";
			outputText += "         Name 		        => " + ((NamedElement) elt).getName();
			if (!HelperFunctions.parseNFP_Duration(HelperFunctions.parseSchedParams((String) HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern"), "sporadic", "minInterarrival"), "value").equals("MALFORMED INPUT")) {
				outputText += ",\n         Min_Interarrival 	=> " + HelperFunctions.parseNFP_Duration(HelperFunctions.parseSchedParams((String) HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern"), "sporadic", "minInterarrival"), "value");
			}
			if (!HelperFunctions.parseNFP_Duration(HelperFunctions.parseSchedParams((String) HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern"), "sporadic", "maxInterarrival"), "value").equals("MALFORMED INPUT")) {
				outputText += ",\n         Max_Interarrival 	=> " + HelperFunctions.parseNFP_Duration(HelperFunctions.parseSchedParams((String) HelperFunctions.getStereotypeProperty(elt, "GaWorkloadEvent", "pattern"), "sporadic", "maxInterarrival"), "value");
			}
			outputText += ")";
		}
		// ----------------------------- ERROR ----------------
		else {
			Log.println("Warning: no valid pattern found in GaWorkloadEvent " + Log.getFileNameAndLineNumber(Thread.currentThread().getStackTrace()[1]));
		}
		return outputText;
	}

	/**
	 * Returns the text for all the internal events stored in the list
	 * 'internalEventList'. The parameter 'theConstraint' is used to created the
	 * global timing requirements for the last event and the parameter
	 * 'gaWorkloadEvent' is used to get the name of the external event.
	 * 
	 * <p>
	 * Called from 'extractActivityData'.
	 * 
	 * @param internalEventList
	 *            a list of all the internal events
	 * @param theConstraint
	 *            a constraint element ('GaLatencyObs' stereotyped) containing
	 *            the global timing requirements' data
	 * @param gaWorkloadEvent
	 *            an element ('GaWorkloadEvent' stereotyped) corresponding to
	 *            the external event
	 * @return the text for all the internal events in MAST format
	 */
	public static String getInternalEventsText(Vector<String> internalEventList, Constraint theConstraint, NamedElement gaWorkloadEvent) {
		String intEvtsText = "";
		int index = 0;
		int numEvents = internalEventList.size();
		for (String theEvent : internalEventList) {
			if (index > 0) {
				intEvtsText += ",\n";
			}
			index++;

			intEvtsText += "        (Type 	=> Regular,\n";
			intEvtsText += "         Name 	=> " + theEvent;
			// -------------------------------------------
			// check how many timing requirements we have. if we have more than
			// one, we will use a composite type
			// numTiming will store how many requirements we have and
			// actualTiming will store which one we are
			// printing (so we put a comma or not at its end)
			int numTiming = 0;
			int actualTiming = 0;
			// [TODO] local timing reqs' code commented out till I know how we
			// are going to use it after what we changed
			// if(
			// HelperFunctions.getStereotypeProperty(stepElement,"SaStep","deadline")
			// != null) numTiming+=1;
			if (index == numEvents && theConstraint != null) {// LAST STEP with
																// global reqs

				String theLatency = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "latency") != null)
					theLatency = HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "latency").toString(), "value");
				String theJitter = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "maxJitter") != null)
					theJitter = HelperFunctions.parseNFP_Real(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "maxJitter").toString(), "value");
				String theMissRatio = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "miss") != null)
					theMissRatio = HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "miss").toString(), "value");
				if (theMissRatio != "MALFORMED INPUT" && theLatency != "MALFORMED INPUT") {
					numTiming += 1;
				} else if (theLatency != "MALFORMED INPUT")
					numTiming += 1;
				if (theJitter != "MALFORMED INPUT")
					numTiming += 1;
			}

			if (numTiming > 0) {// write timing reqs
				intEvtsText += ",\n         Timing_Requirements => (";
			}
			if (numTiming > 1) {// composite
				intEvtsText += "\n              Type              => Composite,";
				intEvtsText += "\n              Requirements_List => (";
			}
			// [TODO] rework the code below to reuse it now so we have local
			// timing reqs again
			// //-------------------------start
			// rqs----------------------------------------
			//
			// //local:
			// if(
			// HelperFunctions.getStereotypeProperty(stepElement,"SaStep","deadline")
			// != null){
			// //intEvtsText +=",\n         Timing_Requirements => (";
			// actualTiming+=1;
			// if(numTiming>1) intEvtsText +="\n               (";
			// intEvtsText
			// +="\n                Type              => Hard_Local_Deadline,";
			// intEvtsText +="\n                Deadline          => " +
			// HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(stepElement,"SaStep","deadline").toString(),
			// "value");
			// if(numTiming>1) intEvtsText +="\n               )";
			// if(actualTiming<numTiming) intEvtsText +=",";
			// //intEvtsText +=" )";
			// }//end if SaStep
			// global:
			if (index == numEvents && theConstraint != null) {// LAST STEP with
																// global reqs

				String theLatency = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "latency") != null)
					theLatency = HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "latency").toString(), "value");
				String theJitter = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "maxJitter") != null)
					theJitter = HelperFunctions.parseNFP_Real(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "maxJitter").toString(), "value");
				String theMissRatio = "MALFORMED INPUT";
				if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "miss") != null)
					theMissRatio = HelperFunctions.parseNFP_Duration(HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "miss").toString(), "value");

				// if miss
				if (theMissRatio != "MALFORMED INPUT" && theLatency != "MALFORMED INPUT") {
					/*
					 * Timing_Requirement = ( Type => Global_Max_Miss_Ratio,
					 * Deadline => Time, Ratio => Percentage, Referenced_Event
					 * => Identifier)
					 */
					actualTiming += 1;
					if (numTiming > 1)
						intEvtsText += "\n               (";

					intEvtsText += "\n                Type              => Global_Max_Miss_Ratio,";
					intEvtsText += "\n                Deadline          => " + theLatency;
					intEvtsText += ",\n                Ratio             => " + theMissRatio;
					intEvtsText += ",\n                Referenced_Event  => " + ((NamedElement) gaWorkloadEvent).getName();
					;

					if (numTiming > 1)
						intEvtsText += "\n               )";
					if (actualTiming < numTiming)
						intEvtsText += ",";

				} else if (theLatency != "MALFORMED INPUT") {
					/*
					 * Timing_Requirement = ( Type => Hard_Global_Deadline,
					 * Deadline => Time, Referenced_Event => Identifier)
					 */
					actualTiming += 1;
					if (numTiming > 1)
						intEvtsText += "\n               (";

					// intEvtsText +=",\n         Timing_Requirements => (";
					if (HelperFunctions.getStereotypeProperty(theConstraint, "GaLatencyObs", "laxity").toString().equals("hard")) {
						intEvtsText += "\n                Type              => Hard_Global_Deadline,";
					} else {// not required
						intEvtsText += "\n                Type              => Soft_Global_Deadline,";
					}
					intEvtsText += "\n                Deadline          => " + theLatency;
					intEvtsText += ",\n                Referenced_Event  => " + ((NamedElement) gaWorkloadEvent).getName();// old:
																															// worng:+
																															// inEvtName;
					// intEvtsText +=" )";

					if (numTiming > 1)
						intEvtsText += "\n               )";
					if (actualTiming < numTiming)
						intEvtsText += ",";

				}// latency

				// if jitter
				if (theJitter != "MALFORMED INPUT") {
					/*
					 * Timing_Requirement = ( Type => Max_Output_Jitter_Req,
					 * Max_Output_Jitter => Time, Referenced_Event =>
					 * Identifier)
					 */
					actualTiming += 1;
					if (numTiming > 1)
						intEvtsText += "\n               (";

					intEvtsText += "\n                Type              => Max_Output_Jitter_Req,";
					intEvtsText += "\n                Max_Output_Jitter => " + theJitter;
					intEvtsText += ",\n                Referenced_Event  => " + ((NamedElement) gaWorkloadEvent).getName();
					;

					if (numTiming > 1)
						intEvtsText += "\n               )";
					if (actualTiming < numTiming)
						intEvtsText += ",";

				}// jitter

			}// if last step
				// -------------------------end
				// rqs----------------------------------------
			if (numTiming > 1) {// composite
				intEvtsText += "\n              )";// close composite
			}
			if (numTiming > 0) {// write timing reqs
				intEvtsText += "\n         )";// close timing reqs
			}
			// END TIMING REQUIREMENTS*******************

			// -------------------------------------------
			// end internal event
			intEvtsText += "\n        )";

		}// for events
		return intEvtsText;
	}// getInternalEventsText

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Given an element (instance of Operation) this method returns the Activity
	 * element associated to it, if exists.
	 *
	 * @param theElement
	 *            UML Element (Operation)
	 * 
	 * @return the Activity element associated to the operation, if exists.
	 */
	public static Element getActivityFromOperation(NamedElement theElement) {

		if (theElement instanceof Operation) {
			Operation theOperation = (Operation) theElement;
			EList<Behavior> theMethods = theOperation.getMethods();
			// Log.println(theElement.getName());
			if (theMethods.size() > 0) {
				for (int i = 0; i < theMethods.size(); i++) {
					if (theMethods.get(i) instanceof Activity) {
						Activity theActivity = (Activity) theMethods.get(i);
						// Log.println("activity");
						EList<Element> activityElements = theActivity.allOwnedElements();
						for (int j = 0; j < activityElements.size(); j++) {
							if (activityElements.get(j) instanceof InitialNode) {
								// Log.println(((NamedElement)activityElements.get(j)).getName());
								return activityElements.get(j);
							}
						}
					}// if activity
				}// for methods
			}// end if methods
		}// end if operation
		return null;
	}

	/**
	 * Called from extractStepData when it reaches an operation (SaStep) with NO
	 * subUsages with concurRes or inner activities with concurRes. This method
	 * calls itself recursively till there are no more operations to add. Simple
	 * operations are added to the mSimpleOperationsText static member variable,
	 * composite operations to mCompositeOperationsText and enclosing operations
	 * to mEnclosingOperationsText.
	 *
	 * @param rootElement
	 *            UML Element ("SaStep" stereotyped)
	 */
	public static void generateOperations(NamedElement rootElement) {
		if (HelperFunctions.hasStereotype(rootElement, "SaStep")) {

			// it HAS to be a simple/enclosing -> CAN'T have subUsages with
			// concurRes or inner activities with concurRes

			boolean checkSubusages = true;
			if (HelperFunctions.getStereotypeProperty(rootElement, "SaStep", "execTime") != null) {// simple
																									// or
																									// enclosing
				if (treeHasExecTime(rootElement, false)) {// create enclosing

					Vector<String> enclosingOperationsList = new Vector<String>();// to
																					// create
																					// the
																					// enclosing
																					// text
					// ////////////////////////////////////////////////////////////////////
					// check children
					// activities--------------------------
					if (rootElement instanceof Operation) {
						Operation theOperation = (Operation) rootElement;
						// Log.println("checking tree: activity operation "+theOperation.getName());
						EList<Behavior> theMethods = theOperation.getMethods();
						if (theMethods.size() > 0) {
							for (int i = 0; i < theMethods.size(); i++) {
								if (theMethods.get(i) instanceof Activity) {
									Activity theActivity = (Activity) theMethods.get(i);
									EList<Element> activityElements = theActivity.allOwnedElements();
									for (int j = 0; j < activityElements.size(); j++) {
										if (activityElements.get(j) instanceof InitialNode) {
											// check activity steps:
											InitialNode theInitialNode = (InitialNode) activityElements.get(j);
											// code modified from
											// extractActivityData
											// activity elements extraction:
											Vector<NamedElement> pathElements = new Vector<NamedElement>();
											pathElements = extractActivityElements(theInitialNode);
											for (NamedElement anElement : pathElements) {
												if (HelperFunctions.hasStereotype(anElement, "SaStep")) {
													checkSubusages = false;
													enclosingOperationsList.add(HelperFunctions.getElementLongName(anElement));
													generateOperations(anElement);
												}
											}// end for
										}
									}
								}// if activity
							}// for methods
						}// end if methods
					}// end if operation
						// subUsages--------------------------
					List<Operation> simpleOperations = HelperFunctions.getSubUsage(rootElement, "SaStep", "subUsage");
					if (simpleOperations.size() != 0 && checkSubusages) {// subUsage
																			// has
																			// data
																			// &
																			// there
																			// was
																			// no
																			// activity
																			// data
																			// (design
																			// decision)
						for (Operation anOperation : simpleOperations) {
							if (HelperFunctions.hasStereotype(anOperation, "SaStep")) {
								enclosingOperationsList.add(HelperFunctions.getElementLongName(anOperation));
								generateOperations(anOperation);
							}
						}
					}// if subUsage data

					// create enclosing:
					if (enclosingOperationsList.size() > 0) {
						mEnclosingOperationsText += getEnclosingOperationText(rootElement, enclosingOperationsList);
					} else {
						Log.println("ERROR: enclosing -> shouldn't reach this point");
					}
					// /////////////////////////////////////////////////////////////////////
				} else {// is simple
					mSimpleOperationsText += getSimpleOperationText(rootElement);
				}

			} else {// no execTime: composite or 'nothing' (no execT no
					// concurRes)
				if (treeHasExecTime(rootElement, false)) {// composite
					Vector<String> compositeOperationsList = new Vector<String>();// to
																					// create
																					// the
																					// composite
																					// text
					// ////////////////////////////////////////////////////////////////////
					// check children
					// activities--------------------------
					if (rootElement instanceof Operation) {
						Operation theOperation = (Operation) rootElement;
						// Log.println("checking tree: activity operation "+theOperation.getName());
						EList<Behavior> theMethods = theOperation.getMethods();
						if (theMethods.size() > 0) {
							for (int i = 0; i < theMethods.size(); i++) {
								if (theMethods.get(i) instanceof Activity) {
									Activity theActivity = (Activity) theMethods.get(i);
									EList<Element> activityElements = theActivity.allOwnedElements();
									for (int j = 0; j < activityElements.size(); j++) {
										if (activityElements.get(j) instanceof InitialNode) {
											// check activity steps:
											InitialNode theInitialNode = (InitialNode) activityElements.get(j);
											// code modified from
											// extractActivitiData
											// activity elements extraction:
											Vector<NamedElement> pathElements = new Vector<NamedElement>();
											pathElements = extractActivityElements(theInitialNode);
											for (NamedElement anElement : pathElements) {
												if (HelperFunctions.hasStereotype(anElement, "SaStep")) {
													checkSubusages = false;
													compositeOperationsList.add(HelperFunctions.getElementLongName(anElement));
													generateOperations(anElement);
												}
											}// end for
										}
									}
								}// if activity
							}// for methods
						}// end if methods
					}// end if operation
						// subUsages--------------------------
					List<Operation> simpleOperations = HelperFunctions.getSubUsage(rootElement, "SaStep", "subUsage");
					if (simpleOperations.size() != 0 && checkSubusages) {// subUsage
																			// has
																			// data
																			// &
																			// there
																			// was
																			// no
																			// activity
																			// data
																			// (design
																			// decision)
						for (Operation anOperation : simpleOperations) {
							if (HelperFunctions.hasStereotype(anOperation, "SaStep")) {
								compositeOperationsList.add(HelperFunctions.getElementLongName(anOperation));
								generateOperations(anOperation);
							}
						}
					}// if subUsage data

					// create composite://TODO it said enclosing before, check
					// if it was an error or there's something to it.
					if (compositeOperationsList.size() > 0) {
						mCompositeOperationsText += getCompositeOperationText(rootElement, compositeOperationsList);
					} else {
						Log.println("ERROR: enclosing -> shouldn't reach this point");
					}
					// /////////////////////////////////////////////////////////////////////

				} else {
					// nothing, 'useless'/empty operation
					mSimpleOperationsText += getSimpleOperationText(rootElement);
				}
			}// end if execTime

		}// if saStep
	}// generateOperations

	/**
	 * Extracts the relevant data from a given element ("SaStep" stereotyped).
	 * This method is called from each of the elements on the list created by
	 * extractActivityElements and calls itself recursively for each of its
	 * valid ("SaStep" stereotyped) children elements (from an Activity if it
	 * has one associated or from its subUsage list).
	 * 
	 * <p>
	 * The parentThread parameter is used to keep track of the caller element's
	 * concuRes, which is important to generate the event handlers (MAST).
	 *
	 * @param theElement
	 *            UML Element (should be an Activity)
	 * 
	 * @param parentThread
	 *            concuRes of the parent element.
	 * 
	 * @return a vector containing the relevant elements of the Activity diagram
	 *         in order
	 */
	public static boolean extractStepData(NamedElement theElement, String parentThread) {
		if (HelperFunctions.hasStereotype(theElement, "SaStep")) {

			Log.debugPrintln("*EXTRACTING step data from: " + theElement.getName());

			// check if we need to change servers/threads
			if (HelperFunctions.getStereotypeProperty(theElement, "SaStep", "concurRes") != null) {
				Log.debugPrintln("EXTRACT: parent-> " + parentThread + " Actual-> " + mActualThread + " new-> " + HelperFunctions.getStereotypeReferenceName(theElement, "SaStep", "concurRes").toString());
				if (!HelperFunctions.getStereotypeReferenceName(theElement, "SaStep", "concurRes").toString().equals(mActualThread)) {
					// new thread is different from current one
					Log.debugPrintln("EXTRACT: concures->" + theElement.getName());
					// is there an Ev_Handler opened?---------
					if (!mEventHandler.mInputEvent.equals("")) {
						// there's an opened event -> close old, add to list and
						// create empty one
						closeEventHandler();
					}// end opened event ------
					Log.debugPrintln("NOT NULL CONCURRES" + theElement.getName());
					Log.debugPrintln("EXTRACT: athread is:" + HelperFunctions.getStereotypeReferenceName(theElement, "SaStep", "concurRes").toString());
					mActualThread = HelperFunctions.getStereotypeReferenceName(theElement, "SaStep", "concurRes").toString();
					parentThread = mActualThread;
					mEventHandler.mInputEvent = mNextInputEvent;
					mEventHandler.mServer = mActualThread;
				}// else everything stays the same

			} else {// no concurRes
				Log.debugPrintln("EXTRACT: parent-> " + parentThread + " Actual-> " + mActualThread);
				if (!parentThread.equals(mActualThread)) {// actual and parent
															// are different
					Log.debugPrintln("Parent != actual" + theElement.getName());
					// is there an Ev_Handler opened?---------
					if (!mEventHandler.mInputEvent.equals("")) {
						// there's an opened event -> close old, add to list and
						// create empty one
						closeEventHandler();
					}// end opened event ------
					mActualThread = parentThread;
					// parentThread stays the same
					mEventHandler.mInputEvent = mNextInputEvent;
					mEventHandler.mServer = mActualThread;
				}// else everything stays the same
			}// end if concurRes
				// ----------------------------------------------

			// start common actions--------------------------
			Element lastChildren = null;
			Element initialNode = getActivityFromOperation(theElement);
			if (HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime") != null) {
				// it HAS to be a simple/enclosing -> CAN'T have subUsages or
				// inner activities with concurRes
				if (treeHasConcurRes(theElement, false)) {
					Log.println("ERROR!! an element with execTime has children with concuRes: " + theElement.getName() + HelperFunctions.getStereotypeProperty(theElement, "SaStep", "execTime"));
					return false;
				}
				// SIMPLE OR ENCLOSING:
				// mEventHandlerOperationList.add(getHandlerOperationName(theElement));
				mEventHandlerOperationList.add(HelperFunctions.getElementLongName(theElement));
				// getOperationText(theElement);
				generateOperations(theElement);
				// Log.println("HANDLER OPERATION added "+getHandlerOperationName(theElement));

			} else if (initialNode != null) {// no execTime BUT an associated
												// ***Activity***
				// activity elements extraction:
				Vector<NamedElement> pathElements = new Vector<NamedElement>();
				pathElements = extractActivityElements(initialNode);
				// extract data from steps:
				mNextInputEvent = ((NamedElement) initialNode).getName();
				String tempActualThread = mActualThread;
				lastChildren = null;
				for (NamedElement anElement : pathElements) {
					lastChildren = anElement;
					extractStepData(anElement, tempActualThread);// TODO: check
																	// if
																	// mActualThread
																	// is the
																	// correct
																	// one
				}// end for
				if (lastChildren != null) {
					fatherChildrenStruct aFatherChildrenStruct = new fatherChildrenStruct();
					aFatherChildrenStruct.mElement = lastChildren;
					aFatherChildrenStruct.mInternalEventName = mEventHandler.mOutputEvent;
					mFatherChildrenList.add(aFatherChildrenStruct);
					lastChildren = null;
				}
			} else {// no execTime + no activity -> is a "job" (subUsages?)
				// check children
				Log.debugPrintln("Checking children for: " + theElement.getName());
				List<Operation> simpleOperations = HelperFunctions.getSubUsage(theElement, "SaStep", "subUsage");
				if (simpleOperations.size() != 0) {// subUsage has data
					String tempActualThread = mActualThread;
					lastChildren = null;
					for (NamedElement nextStep : simpleOperations) {
						lastChildren = nextStep;
						if (!extractStepData(nextStep, tempActualThread))
							return false;
					}
					if (lastChildren != null) {
						fatherChildrenStruct aFatherChildrenStruct = new fatherChildrenStruct();
						aFatherChildrenStruct.mElement = lastChildren;
						aFatherChildrenStruct.mInternalEventName = mEventHandler.mOutputEvent;
						mFatherChildrenList.add(aFatherChildrenStruct);
						lastChildren = null;
					}
				}
			}// end no execTime
				// ----------------------------------------------

		}// if step
			// no errors
		return true;
	}// extractStepData

	/**
	 * Extracts an vector containing the relevant elements ("SaStep"
	 * stereotyped) of the Activity diagram in order. Given an InitailNode
	 * ("GaWorkloadEvent" stereotyped) it follows the diagram elements until it
	 * reaches the end. i.e.
	 * ControlFlow->ActivityNode->ControlFlow->ActivityNode
	 * ->...->ActivityNode->-|
	 *
	 * @param anElement
	 *            UML Element (should be an Activity)
	 * 
	 * @return a vector containing the relevant elements of the Activity diagram
	 *         in order
	 */
	public static Vector<NamedElement> extractActivityElements(Element anElement) {

		Vector<NamedElement> pathElements = new Vector<NamedElement>();

		Log.debugPrintln("" + ((NamedElement) anElement).getName());

		if (anElement instanceof InitialNode) {

			EList<ActivityEdge> outgoings = ((InitialNode) anElement).getOutgoings();
			ActivityNode aNode;

			while (true) {

				if (outgoings == null)
					break;

				aNode = null;
				for (ActivityEdge anOutgoing : outgoings) {
					if (anOutgoing instanceof ControlFlow) {
						Log.debugPrintln("" + anOutgoing.getName());
						aNode = ((ControlFlow) anOutgoing).getTarget();
						break;
					}
				}
				if (aNode == null)
					break;

				if (HelperFunctions.hasStereotype(aNode, "SaStep")) {
					Log.debugPrintln("found a step");
					pathElements.add(aNode);
				}

				outgoings = aNode.getOutgoings();

			}// while true
		}// if InitialNode

		return pathElements;

	}// END extractActivityElements

	/**
	 * Static method called from Acceleo's marte2mast.mtl template for every
	 * Activity with the stereotype "SaEndtoEndFlow" (also, the Activity has to
	 * be owned by an element with the stereotype "SaAnalysisContext").
	 * 
	 * <p>
	 * Uses other methods from this class, like extractActivityElements,
	 * extractStepData, closeEventHandler, getExternalEventText ... to generate
	 * the text for this transaction.
	 *
	 * @param elt
	 *            should be a UML Activity with the restrictions above described
	 */
	public static void extractActivityData(Element elt) {

		if (!(elt instanceof Activity)) {
			return;
		}
		String tempText;
		String activityName = ((NamedElement) elt).getName();

		mTransactionName = activityName;
		mEventHandler.mOutputEvent = mTransactionName + "__Internal_Event_" + (++mInternalEventCounter);

		Element gaWorkloadEvent = null;

		// check owned elements
		EList<Element> listOfElements = elt.getOwnedElements();
		for (Element anElement : listOfElements) {
			if (HelperFunctions.hasStereotype(anElement, "GaWorkloadEvent")) {
				gaWorkloadEvent = anElement;
				break;// one found, no need to look for more (should't be more)
			}
		}

		// check owned nodes (ActivityNodes are not part of OwnedElements)
		if (null == gaWorkloadEvent) {
			EList<ActivityNode> ownedNodes = ((Activity)elt).getOwnedNodes();
			for (Element anElement : ownedNodes) {
				if (HelperFunctions.hasStereotype(anElement, "GaWorkloadEvent")) {
					gaWorkloadEvent = anElement;
					break;// one found, no need to look for more (should't be more)
				}
			}
		}

		if (null == gaWorkloadEvent) {
			return;
		}
		// activity elements extraction:
		Vector<NamedElement> pathElements = new Vector<NamedElement>();
		pathElements = extractActivityElements(gaWorkloadEvent);

		// extract data from
		// steps/////////////////////////////////////////////////////
		// mTransactionFatherChildrensLists.clear();
		mNextInputEvent = ((NamedElement) gaWorkloadEvent).getName();
		for (NamedElement anElement : pathElements) {
			mFatherChildrenList = new Vector<fatherChildrenStruct>();// didn't work if i used .clear()??
			// ////
			extractStepData(anElement, "");
			// ////
			fatherChildrenStruct aFatherChildrenStruct = new fatherChildrenStruct();
			aFatherChildrenStruct.mElement = anElement;
			aFatherChildrenStruct.mInternalEventName = mEventHandler.mOutputEvent;
			mFatherChildrenList.add(aFatherChildrenStruct);
			// Log.println("added: "+anElement.getName());
			mTransactionFatherChildrensLists.add(mFatherChildrenList);
		}// end for

		// Log.println("father-children list:");
		// for(int i=0;i<mTransactionFatherChildrensLists.size();i++){
		// Vector<fatherChildrenStruct> fc =
		// mTransactionFatherChildrensLists.get(i);
		// for(int j=0;j<fc.size();j++){
		// Log.println(HelperFunctions.getElementLongName(fc.get(j).mElement)+" - "+fc.get(j).mInternalEventName);
		//
		// }
		// Log.println("");
		// }

		// ////////CLOSE LAST EV HANDLER//////////////////
		// check if we have an event handler not closed
		if (!mEventHandler.mInputEvent.equals("")) {
			// close old ///////
			closeEventHandler();
		}
		// ////////////////////////////////

		// get constraint data (GaLatencyObs)
		Constraint theConstraint = null;
		for (Element anElement : listOfElements) {
			if (anElement instanceof Constraint) {
				if (HelperFunctions.hasStereotype(anElement, "GaLatencyObs")) {
					Log.debugPrintln("found a constraint with a valid stereotype: " + ((NamedElement) anElement).getName());
					if (HelperFunctions.getStereotypeProperty(anElement, "GaLatencyObs", "latency") != null) {
						theConstraint = (Constraint) anElement;
					}
				}// end if SaStep
			}// end if constraint
		}

		// fill transaction's
		// text/////////////////////////////////////////////////////
		Log.debugPrintln("storing text from a transaction");

		tempText = "";
		tempText += "\nTransaction (\n";

		tempText += "    Type            => Regular,\n";

		tempText += "    Name            => " + activityName + ",\n";// transaction's
																		// name
																		// =
																		// activity's
																		// name

		tempText += "    External_Events => (\n";
		// ---------------------------External_Events--------------------------------------
		tempText += getExternalEventText(gaWorkloadEvent);
		// ---------------------------End
		// External_Events----------------------------------
		tempText += "\n    ),\n";

		tempText += "    Internal_Events => (\n";
		// ---------------------------Internal_Events--------------------------------------
		tempText += getInternalEventsText(mInternalEventList, theConstraint, (NamedElement) gaWorkloadEvent);// intEvtsText;
		// ---------------------------End
		// Internal_Events----------------------------------
		tempText += "\n    ),\n";

		tempText += "    Event_Handlers  => (\n";
		// ---------------------------Event_Handlers
		// --------------------------------------
		int index = 0;
		String evtHandlersText = "";
		Log.debugPrintln("Handlers:" + mEventHandlerList.size());
		for (eventHandler theHandler : mEventHandlerList) {
			Log.debugPrintln("Handler: " + index + " - " + theHandler.mOperation);

			if (index > 0) {
				evtHandlersText += ",\n";
			}
			index++;
			evtHandlersText += "        (Type                 => Activity,\n";
			evtHandlersText += "         Input_Event          => " + theHandler.mInputEvent + ",\n";
			evtHandlersText += "         Output_Event         => " + theHandler.mOutputEvent + ",\n";
			evtHandlersText += "         Activity_Operation   => " + theHandler.mOperation + ",\n";
			evtHandlersText += "         Activity_Server      => " + theHandler.mServer;
			evtHandlersText += "\n        )";
		}
		tempText += evtHandlersText;
		// ---------------------------End Event_Handlers
		// ----------------------------------
		tempText += "\n    )\n";

		tempText += ");\n";// end transaction

		mTransactionsText += tempText;
		Log.debugPrintln("finished storing text from a transaction");
		Log.debugPrintln("");
		// CLEAN UP
		mInternalEventList.clear();
		mInternalEventCounter = 0;
		mEventHandlerList.clear();
		mActualThread = "";
		mEventHandlerOperationList.clear();

		Log.println(">  Activity: " + ((NamedElement) elt).getName());
	}// extractActivityData END ---------

	/**
	 * Static method called from Acceleo's marte2mast.mtl template, after
	 * extracting the data from all the activities, to return the text
	 * describing the MAST operations and transactions.
	 *
	 * @param elt
	 *            UML Element from which the method is called in Acceleo
	 * 
	 * @return the text describing the MAST operations and transactions
	 */
	public static Object getOperationsTransactionsText(Element elt) {
		Log.println(">> Writing Operations and Transactions to the output file");
		String output = "";
		output += "-- Operations\n\n" + mSimpleOperationsText + mEnclosingOperationsText + mCompositeOperationsText + "\n\n-- Transactions\n\n" + mTransactionsText;
		// CLEAN UP (plug-in)/////////////////////
		// there's some kind of data persistence between different executions of
		// the plug-in!!
		// doesn't happen when working on 'workspace mode'
		mSimpleOperationsText = "";
		mEnclosingOperationsText = "";
		mCompositeOperationsText = "";
		mTransactionsText = "";
		mSimpleOperationsNames.clear();

		mNextInputEvent = "";
		mActualThread = "";
		mInternalEventCounter = 0;
		mDummyOperationCreated = false;
		mCompositeOperationsCounter = 0;

		mTransactionName = "";

		return output;
	}// getOperationsTransactionsText

}// end class ActivityFunctions