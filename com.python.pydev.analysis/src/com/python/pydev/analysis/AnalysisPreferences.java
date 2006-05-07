/*
 * Created on 24/07/2005
 */
package com.python.pydev.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Preferences;

public class AnalysisPreferences extends AbstractAnalysisPreferences{


    /**
     * singleton
     */
    private static IAnalysisPreferences analysisPreferences;
    
    /**
     * @return get the preferences for analysis based on the preferences
     */
    public static IAnalysisPreferences getAnalysisPreferences(){
        if(analysisPreferences == null){
            analysisPreferences = new AnalysisPreferences();
        }
        return analysisPreferences;
    }
    
    /**
     * when adding a new type, it must be specified:
     * here
     * AnalysisPreferenceInitializer
     * IAnalysisPreferences
     * AnalysisPreferencesPage
     */
    public static Object [][] completeSeverityMap = new Object[][]{
        {IAnalysisPreferences.TYPE_UNUSED_IMPORT              , AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT              , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_IMPORT                },
        {IAnalysisPreferences.TYPE_UNUSED_VARIABLE            , AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE            , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_VARIABLE              },
        {IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE         , AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE         , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNDEFINED_VARIABLE           },
        {IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE       , AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE       , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_DUPLICATED_SIGNATURE         },
        {IAnalysisPreferences.TYPE_REIMPORT                   , AnalysisPreferenceInitializer.SEVERITY_REIMPORT                   , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_REIMPORT                     },
        {IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT          , AnalysisPreferenceInitializer.SEVERITY_UNRESOLVED_IMPORT          , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNRESOLVED_IMPORT            },
        {IAnalysisPreferences.TYPE_NO_SELF                    , AnalysisPreferenceInitializer.SEVERITY_NO_SELF                    , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_NO_SELF                      },
        {IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT         , AnalysisPreferenceInitializer.SEVERITY_UNUSED_WILD_IMPORT         , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_WILD_IMPORT           },
        {IAnalysisPreferences.TYPE_UNDEFINED_IMPORT_VARIABLE  , AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_IMPORT_VARIABLE  , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNDEFINED_IMPORT_VARIABLE    },
        {IAnalysisPreferences.TYPE_UNUSED_PARAMETER           , AnalysisPreferenceInitializer.SEVERITY_UNUSED_PARAMETER           , AnalysisPreferenceInitializer.DEFAULT_SEVERITY_UNUSED_PARAMETER             },
    };
    


    public void clearCaches() {
        severityTypeMapCache = null;
    }
    
    HashMap<Integer, Integer> severityTypeMapCache = null;
    
    private Map<Integer, Integer> getSeverityTypeMap() {
        if(severityTypeMapCache == null){
            severityTypeMapCache = new HashMap<Integer, Integer>();
            Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
    
            for (int i = 0; i < completeSeverityMap.length; i++) {
                Object[] s = completeSeverityMap[i];
                severityTypeMapCache.put((Integer)s[0], pluginPreferences.getInt((String)s[1]));
            }
        }        
        return severityTypeMapCache;
    }
    
    /**
     * return the severity based on the user-set values
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#getSeverityForType(int)
     */
    public int getSeverityForType(int type) {
        Map<Integer, Integer> severityTypeMap = getSeverityTypeMap();
        Integer sev = severityTypeMap.get(type);
        if(sev == null){
            throw new RuntimeException("Unable to get severity for: "+type);
        }
        return sev;
    }

    /**
     * yeah, we always do code analysis...
     *  
     * @see com.python.pydev.analysis.IAnalysisPreferences#makeCodeAnalysis()
     */
    public boolean makeCodeAnalysis() {
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
        return pluginPreferences.getBoolean(AnalysisPreferenceInitializer.DO_CODE_ANALYSIS);
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getNamesIgnoredByUnusedVariable()
     */
    public Set<String> getNamesIgnoredByUnusedVariable() {
        return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE);
    }
    
    public Set<String> getTokensAlwaysInGlobals() {
    	return getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_CONSIDER_GLOBALS);
    }

    /**
     * @param preferencesName
     * @return
     */
    private Set<String> getSetOfNames(String preferencesName) {
        HashSet<String> names = new HashSet<String>();
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();

        String string = pluginPreferences.getString(preferencesName);
        if(string != null){
            String[] strings = string.split(",");
            for (int i = 0; i < strings.length; i++) {
                names.add(strings[i].trim());
            }
        }
        
        return names;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getModuleNamePatternsToBeIgnored()
     */
    public Set<String> getModuleNamePatternsToBeIgnored() {
        Set<String> setOfNames = getSetOfNames(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_IMPORT);
        HashSet<String> ret = new HashSet<String>();
        for (String string : setOfNames) {
            //we have to make it a regular expression as java requires, so * is actually .*
            ret.add(string.replaceAll("\\*", ".*"));
        }
        return ret;
    }

    /**
     * @see com.python.pydev.analysis.IAnalysisPreferences#getWhenAnalyze()
     */
    public int getWhenAnalyze() {
        Preferences pluginPreferences = AnalysisPlugin.getDefault().getPluginPreferences();
        return pluginPreferences.getInt(AnalysisPreferenceInitializer.WHEN_ANALYZE);
    }


}
