This project contains an Eclipse plugin for using MAST to evaluate Marte annotated UML diagrams which are design with the Papyrus UML editor.

The original (GPL v2 licensed) sourcecode of the Marte2Mast Eclipse plugin was published here :
http://mast.unican.es/umlmast/marte2mast/

Based on this version, some modifications were made to the plugin in order to get it usable on Eclipse Luna.
The plugin works when started from within the plugin development workbench but might not work when installed as a package into a separate Eclipse installation.

Additionally, a preference pane was added whose values must yet be passed on to the package actually doing the work.
(Reference for adding preference panes: http://help.eclipse.org/luna/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fpreferences_prefs_implement.htm)



 
TODOS
 - How to get the preferences from UI to non-ui package (especially in Mast.java -> openGmast() function)
 - MAST command for calculation: mast_analysis default -c ResourceModel.mast ResourceModel.mast.out
 - MAST command for display of result: gmastresults ResourceModel.mast ResourceModel.mast.out


ISSUES
 - if SaStep::subUsage contains reference to the method it is applied to, nothing gets generated and no usable error message is returned 
