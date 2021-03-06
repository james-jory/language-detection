# Language Detection library for Java

Imported from [Language Detection](https://code.google.com/p/language-detection/) Google code project.

Modified original project to support using more than one DetectorFactory at once based on different 
profiles/models.

Also reorganized project to fit a more conventional structure.

## Examples

There are two profiles/models for language detection bundled with the JAR. The default profile/model 
is suitable for longer text. There is a second profile/model that is optimized for shorter text. 
There are static accessors on DetectorFactory for both default profiles. The default profiles/models 
are loaded automatically so loadProfile does NOT need to be called.

```
try {
	// Use the default factory which automatically loads the 
	// profile/model suitable for longer text. When detecting 
	// for shorter text, use DetectorFactory.getDefaultShortTextFactory()
	// instead.
	DetectorFactory factory = DetectorFactory.getDefaultFactory();

	// Create a detector instance
	Detector detector = factory.create();
	
	// Append text to detect.
	detector.append(...);
	
	// Get best matched language.
	String lang = detector.detect();
	
	// Or get all matched languages with probabilities.
	List<Language> matches = detector.getProbabilities();
}
catch (LangDetectException e) {
	// Something went wrong.
}
``` 

You can also load your own profile/model. The profile/model only needs to be done once 
for a factory.

```
try {
	// Creates a factory that needs a profile/model to be loaded.
	DetectorFactory factory = DetectorFactory.getFactory("my factory");
	// Do this once in your app for the factory.
	File profileDir = new File(...);	// Where your profile/model files are located.
	factory.loadProfile(profileDir);

	// Elsewhere in your app, you can retrieve the already initialized 
	// factory by name. No need to load the profile again.
	factory = DetectorFactory.getFactory("my factory");
	
	// Create a detector instance
	Detector detector = factory.create();
	
	// Append some text to detect.
	detector.append(...);
	
	String lang = detector.detect();
	
	// Or get all matched languages with probabilities.
	List<Language> matches = detector.getProbabilities();
}
catch (LangDetectException e) {
	// Something went wrong.
}
``` 

### Language Preferences

You can limit and/or set language preferences on the Detector. The key is whether 
the priorities are set as additive or not (the second parameter to Detector.setPriorityMap). 
When set as additive, the priority map is added to all existing supported languages. That is, 
the map value for each language in your map is added to the default priority.

When additive is false, your priority map replaces the values used by your Detector instance 
which essentially limits the languages detected to those in your priority map. 

```
DetectorFactory factory = DetectorFactory.getDefaultFactory();
Detector detector = factory.create();

// Limit detection to "en", "es", and "it" only with higher priority on "en".
Map<String, Double> priorities = new HashMap<String, Double>();
priorities.put("en", .1);
priorities.put("es", .05);
priorities.put("it", .05);

detector.setPriorityMap(priorities, false);

// Detect all supported languages but just set a higher priority on "en".
priorities = new HashMap<String, Double>();
priorities.put("en", 1.0);

detector.setPriorityMap(priorities, true);
```
