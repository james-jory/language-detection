package com.cybozu.labs.langdetect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;

import com.cybozu.labs.langdetect.util.LangProfile;

/**
 * Language Detector Factory Class
 * 
 * This class manages an initialization and construction of DetectoryFactory instances. 
 * 
 * Before using language detection library you must create a DetectorFactory. The factory 
 * manages the profile/model data that is used during detection. The profile/model 
 * data must be loaded before creating instances of {@link Detector}. 
 * 
 * When the language detection factory is created and loaded with a profile/model, construct 
 * Detector instance via {@link DetectorFactory#create()}.
 * 
 * There are two default factories that load the profile/model for detecting standard/longer text
 * and another for detecting shorter text. The profiles for both of these factories are bundled 
 * with the module's JAR and loaded as resources. To use your own profile/model, you must 
 * create a factory and load your profile before creating Detector instances.
 * 
 * See also {@link Detector}'s sample code.
 * 
 * <ul>
 * <li>4x faster improvement based on Elmer Garduno's code. Thanks!</li>
 * </ul>
 * 
 * @see Detector
 * @author Nakatani Shuyo
 */
public class DetectorFactory {
	private static final Map<String, DetectorFactory> factories = new HashMap<String, DetectorFactory>();
	private static final String DEFAULT_PROFILE = "DEFAULT";
	private static final String DEFAULT_SHORT_PROFILE = "SHORT";
	
    HashMap<String, double[]> wordLangProbMap;
    ArrayList<String> langlist;
    Long seed;
    
    private DetectorFactory() {
        wordLangProbMap = new HashMap<String, double[]>();
        langlist = new ArrayList<String>();
    }

    public static DetectorFactory getDefaultFactory() throws LangDetectException {
    	DetectorFactory factory = getFactory(DEFAULT_PROFILE, false);
    	
    	synchronized(factory) {
    		if (factory.langlist.isEmpty())
    			factory.loadProfileAsResource("profiles/");
    	}
    	
    	return factory;
    }

    public static DetectorFactory getDefaultShortTextFactory() throws LangDetectException {
    	DetectorFactory factory = getFactory(DEFAULT_SHORT_PROFILE, false);
    	
    	synchronized(factory) {
    		if (factory.langlist.isEmpty())
    			factory.loadProfileAsResource("profiles.sm/");
    	}
    	
    	return factory;
    }
    
    private synchronized void loadProfileAsResource(String path) throws LangDetectException {
    	CodeSource src = DetectorFactory.class.getProtectionDomain().getCodeSource();
    	if (src == null)
            throw new LangDetectException(ErrorCode.NeedLoadProfileError, "Not found profile: " + path);
    	
    	URL url = src.getLocation();    	
    	if (url == null)
            throw new LangDetectException(ErrorCode.NeedLoadProfileError, "Not found profile: " + path);
    	
    	JarFile jar = null;
    	
    	try {
    		jar = new JarFile(url.getFile());

    		Enumeration<JarEntry> enumEntries = jar.entries();
    		
    		List<LangProfile> profiles = new ArrayList<LangProfile>();
    		
    		while (enumEntries.hasMoreElements()) {
    			JarEntry entry = enumEntries.nextElement();
    			if (entry.isDirectory())
    				continue;
                
    			String name = entry.getName();
    			if (name.startsWith(path)) {
    				InputStream is = null;
                	
    				try {
    					is = jar.getInputStream(entry);
    					LangProfile profile = JSON.decode(is, LangProfile.class);
    					profiles.add(profile);
    				} 
    				catch (JSONException e) {
    					throw new LangDetectException(ErrorCode.FormatError, "profile format error in '" + name + "'");
    				}
    				catch (IOException e) {
    					throw new LangDetectException(ErrorCode.FileLoadError, "can't open '" + name + "'");
    				} 
    				finally {
    					try {
    						if (is!=null) is.close();
    					} catch (IOException e) {}
    				}
    			}
    		}
    		
    		int langsize = profiles.size();
    		for (int i = 0; i < langsize; i++)
    			addProfile(profiles.get(i), i, langsize);
    	}
    	catch (IOException e) {
    		throw new LangDetectException(ErrorCode.FileLoadError, "can't open '" + url + "'");
    	}
    	finally {
    		if (jar != null) {
    			try { jar.close(); } catch (IOException e) {}
    		}
    	}
	}
    
    /**
     * Load profiles from specified directory.
     * This method must be called once before language detection.
     *  
     * @param profileDirectory profile directory path
     * @throws LangDetectException  Can't open profiles(error code = {@link ErrorCode#FileLoadError})
     *                              or profile's format is wrong (error code = {@link ErrorCode#FormatError})
     */
    public void loadProfile(String profileDirectory) throws LangDetectException {
        loadProfile(new File(profileDirectory));
    }

    /**
     * Load profiles from specified directory.
     * This method must be called once before language detection.
     *  
     * @param profileName profile name
     * @param profileDirectory profile directory path
     * @throws LangDetectException  Can't open profiles(error code = {@link ErrorCode#FileLoadError})
     *                              or profile's format is wrong (error code = {@link ErrorCode#FormatError})
     */
    public synchronized void loadProfile(File profileDirectory) throws LangDetectException {
        File[] listFiles = profileDirectory.listFiles();
        if (listFiles == null)
            throw new LangDetectException(ErrorCode.NeedLoadProfileError, "Not found profile: " + profileDirectory);
        
        int langsize = listFiles.length, index = 0;
        for (File file: listFiles) {
            if (file.getName().startsWith(".") || !file.isFile()) continue;
            FileInputStream is = null;
            try {
                is = new FileInputStream(file);
                LangProfile profile = JSON.decode(is, LangProfile.class);
                addProfile(profile, index, langsize);
                ++index;
            } catch (JSONException e) {
                throw new LangDetectException(ErrorCode.FormatError, "profile format error in '" + file.getName() + "'");
            } catch (IOException e) {
                throw new LangDetectException(ErrorCode.FileLoadError, "can't open '" + file.getName() + "'");
            } finally {
                try {
                    if (is!=null) is.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Load profiles from specified directory.
     * This method must be called once before language detection.
     *
     * @param profileName profile name
     * @param profileDirectory profile directory path
     * @throws LangDetectException  Can't open profiles(error code = {@link ErrorCode#FileLoadError})
     *                              or profile's format is wrong (error code = {@link ErrorCode#FormatError})
     */
    public synchronized void loadProfile(List<String> json_profiles) throws LangDetectException {
        int index = 0;
        int langsize = json_profiles.size();
        if (langsize < 2)
            throw new LangDetectException(ErrorCode.NeedLoadProfileError, "Need more than 2 profiles");
            
        for (String json: json_profiles) {
            try {
                LangProfile profile = JSON.decode(json, LangProfile.class);
                addProfile(profile, index, langsize);
                ++index;
            } catch (JSONException e) {
                throw new LangDetectException(ErrorCode.FormatError, "profile format error");
            }
        }
    }
    
    public static DetectorFactory getFactory(String profileName) {
    	return getFactory(profileName, true);
    }
    
    private static DetectorFactory getFactory(String profileName, boolean checkReserved) {
    	if (checkReserved && (DEFAULT_PROFILE.equals(profileName) || DEFAULT_SHORT_PROFILE.equals(profileName)))
    		throw new IllegalArgumentException("Profile name is a reserved name");
    	
    	synchronized(factories) {
	    	if (factories.containsKey(profileName))
	    		return factories.get(profileName);
	    	
	    	DetectorFactory factory = new DetectorFactory();
	    	factories.put(profileName, factory);
	    	return factory;
    	}
    }

    /**
     * @param profile
     * @param langsize 
     * @param index 
     * @throws LangDetectException 
     */
    /* package scope */ void addProfile(LangProfile profile, int index, int langsize) throws LangDetectException {
        String lang = profile.name;
        if (langlist.contains(lang)) 
            throw new LangDetectException(ErrorCode.DuplicateLangError, "duplicate the same language profile");

        langlist.add(lang);
        for (String word: profile.freq.keySet()) {
            if (!wordLangProbMap.containsKey(word)) {
            	wordLangProbMap.put(word, new double[langsize]);
            }
            int length = word.length();
            if (length >= 1 && length <= 3) {
                double prob = profile.freq.get(word).doubleValue() / profile.n_words[length - 1];
                wordLangProbMap.get(word)[index] = prob;
            }
        }
    }

    /**
     * Clear loaded language profiles (reinitialization to be available)
     */
    public synchronized void clear() {
        langlist.clear();
        wordLangProbMap.clear();
    }

    /**
     * Construct Detector instance
     * 
     * @return Detector instance
     * @throws LangDetectException 
     */
    public Detector create() throws LangDetectException {
        if (langlist.isEmpty())
            throw new LangDetectException(ErrorCode.NeedLoadProfileError, "need to load profiles");
    	
        return new Detector(this);
    }

    /**
     * Construct Detector instance with smoothing parameter 
     * 
     * @param alpha smoothing parameter (default value = 0.5)
     * @return Detector instance
     * @throws LangDetectException 
     */
    public Detector create(double alpha) throws LangDetectException {
        Detector detector = create();
        detector.setAlpha(alpha);
        return detector;
    }
    
    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    public final List<String> getLangList() {
        return Collections.unmodifiableList(langlist);
    }
}
