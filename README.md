# Language Detection library for Java

Imported from [Language Detection](https://code.google.com/p/language-detection/) Google code project.

Modified original project to support using more than one DetectorFactory at once based on different 
profiles/models.

Also reorganized project to fit a more conventional structure.

## Example

```
try {
	DetectorFactory factory = DetectorFactory.getFactory("my factory");
	// Do this once for your app.
	File profileDir = new File(...);	// Where profile/model files are located.
	factory.loadProfile(profileDir);

	// Elsewhere in your app, retrieve the factory by name.
	factory = DetectorFactory.getFactory("my factory");
	
	// Create a detector instance
	Detector detector = factory.create();
	
	// Append some text to detect.
	detector.append("See spot run.");
	
	String lang = detector.detect();
}
catch (LangDetectException e) {
	// Something went wrong.
}
``` 