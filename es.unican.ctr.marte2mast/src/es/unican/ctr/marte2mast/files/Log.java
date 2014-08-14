package es.unican.ctr.marte2mast.files;
/*---------------------------------------------------------------------
--                           Marte2Mast                              --
--      Converter of Schedulability analysis models made with UML2   --
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
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;

/**
 * This class contains a series of methods used to create a log text (that will be saved 
 * through an Acceleo template into a file).
 */
public class Log {
	
	//	
	static boolean mCreateLog=false;
	static boolean mLogDebugText=false;
	static String mLogText="";
	static String mModelName="";
	static boolean mSaveToFile=false;
	static boolean mPrintToConsole=false;
	
	/**
	 * Configures the necessary variables. Called from Acceleo's marte2mast.mtl template.
	 *
	 * @param  createLog          <code>true</code> if you want the log text to be created, 
	 *                            <code>false</code> if not
	 *                            Not sure if it's used anymore.
	 *                            
	 * @param   logDebugText      <code>true</code> if you want the debug text (created using debugPrintln) to be stored, 
	 *                            <code>false</code> if not
	 * 
	 * @param   printToConsole    <code>true</code> if you want to print the text to the console (using System.out.print...), 
	 *                            <code>false</code> if not
	 * 
	 * @param   saveToFile        <code>true</code> if you want the log text to be saved into a file, 
	 *                            <code>false</code> if not
	 * 
	 * @param   modelName         the name of the Model (used to generate the name of the log file)
	 */
	public static void initLog(boolean createLog, boolean logDebugText, boolean printToConsole, boolean saveToFile, String modelName){
		mCreateLog=createLog;
		mLogDebugText=logDebugText;
		mModelName=modelName;
		mSaveToFile=saveToFile;
		mPrintToConsole=printToConsole;
		mLogText="";		
	}//end initLog
	
	/**
	 * Cleans up the static variables.
	 */
	public static void closeLog(){
		//clean up
		mCreateLog=false;
		mLogDebugText=false;
		mModelName="";
		mSaveToFile=false;
		mPrintToConsole=false;
		mLogText="";		
	}//end closeLog
	
	/**
	 * Returns the name of the log file. The name is the model name plus ".m2m.log"
	 * 
	 * @return   the name of the log file
	 */
	public static String getFileName(){
		return mModelName+".m2m.log";
	}
	
	/**
	 * Returns the log text.
	 * 
	 * @return   a String with the log text
	 */
	public static String getLogText(){
		String output="";		
		if(!mLogText.equals("")){
			Date todaysDate = new java.util.Date();
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
			String formattedDate = formatter.format(todaysDate);
			output+= "'Marte2Mast Converter' - Log file :: " + formattedDate + " - user: " + System.getProperty("user.name");
			output+= "\n\n" + mLogText;
		}
		return output;
	}
	
	/**
	 * Adds a String to the log text.
	 * 
	 * @param theString   string text to add to the log
	 */
	public static void print(String theString){
		if(mPrintToConsole){
			System.out.print(theString);
		}	
		if(mSaveToFile){
			mLogText+=theString;
		}
	}
	
	/**
	 * Adds a String to the log text + "\n".
	 * 
	 * @param theString   string text to add to the log
	 */
	public static void println(String theString){
		if(mPrintToConsole){
			System.out.println(theString);
		}	
		if(mSaveToFile){
			mLogText+=theString+"\n";
		}
	}
	
	/**
	 * Adds a String to the log text + "\n".
	 * 
	 * <p> Same as println.
	 * 
	 * @param theString   string text to add to the log
	 */
	public static void logLine(String theString){
		println(theString);
	}
	
	/**
	 * Adds a String to the log text + "\n" only if mLogDebugText is set to true.
	 * 
	 * @param theString   string text to add to the log
	 */
	public static void debugPrintln(String theString){
		if(mLogDebugText){
			if(mPrintToConsole){
				System.out.println(theString);
			}	
			if(mSaveToFile){
				mLogText+=theString+"\n";
			}			
		}//if debug	
	}

	public static void logElement(Element theElement, String theType){
		String output="";
		output+=">  "+theType+": ";
		output+= ((NamedElement)theElement).getName();
		if(mPrintToConsole){
			System.out.println(output);
		}	
		if(mSaveToFile){
			mLogText+=output+"\n";
		}
	}
	
	//[TODO] ugly hack/repeated function to bypass some strange things when using logElement in scheduler.mtl
	//if I use logElement in that file, with the correct stereotype name string (i.e. 'SaExecHost')... nothing shows in the log file (?????)
	public static void logElement2(Element theElement, String theType){
		logElement(theElement, theType);
	}
	
	public static String getFileNameAndLineNumber(StackTraceElement stack){
		//REFS:
		//http://stackoverflow.com/questions/115008/how-can-we-print-line-numbers-to-the-log-in-java
		//getStackTrace()[2] doesn't work, [1] seems to work
		//http://www.javaworld.com/community/node/1407
			return "(file: " + stack.getFileName() + ", line~: " + stack.getLineNumber() + ")";		
			//ummm, just notice the number line it gives is sometimes wrong (near, but not the exact line) :(
	}
	
	
}//end class Log