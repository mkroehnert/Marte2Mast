package es.unican.ctr.marte2mast.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import es.unican.ctr.marte2mast.ui.Activator;

/**
 * Class used to initialize default preference values.
 */
public class Marte2MastPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(Marte2MastPreferenceConstants.P_GMAST_EXECUTABLE_PATH, "gmast");
		store.setDefault(Marte2MastPreferenceConstants.P_OUTPUT_PATH, "out-m2m");
	}

}
